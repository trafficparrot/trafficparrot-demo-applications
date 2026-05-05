package com.trafficparrot.demo.sftpfix.client;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public final class UiServer {

    private static final Logger LOG = LoggerFactory.getLogger(UiServer.class);

    private final int port;
    private final App.Config config;
    private final Server server = new Server();
    private final Deque<Map<String, Object>> recent = new ArrayDeque<>();
    private final ReentrantLock recentLock = new ReentrantLock();

    public UiServer(int port, App.Config config) {
        this.port = port;
        this.config = config;
    }

    public void start() throws Exception {
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        ServletContextHandler ctx = new ServletContextHandler();
        ctx.setContextPath("/");
        URL staticUrl = UiServer.class.getClassLoader().getResource("static");
        if (staticUrl == null) throw new IllegalStateException("Cannot locate static/ on classpath");
        ctx.setBaseResourceAsString(staticUrl.toExternalForm());

        ctx.addServlet(new ServletHolder(new DefaultsServlet(config)), "/api/defaults");
        ctx.addServlet(new ServletHolder(new SendServlet(this)), "/api/send");
        ctx.addServlet(new ServletHolder(new RecentServlet(this)), "/api/recent");
        ctx.addServlet(new ServletHolder(new DefaultServlet()), "/");

        server.setHandler(ctx);
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }

    private void recordRecent(Map<String, Object> entry) {
        recentLock.lock();
        try {
            recent.addFirst(entry);
            while (recent.size() > 10) recent.removeLast();
        } finally {
            recentLock.unlock();
        }
    }

    private static final class DefaultsServlet extends HttpServlet {
        private final App.Config c;
        DefaultsServlet(App.Config c) { this.c = c; }
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("application/json");
            String body = "{"
                + "\"host\":" + Json.s(c.defaultHost) + ","
                + "\"port\":" + c.defaultPort + ","
                + "\"username\":" + Json.s(c.defaultUser) + ","
                + "\"password\":" + Json.s(c.defaultPassword) + ","
                + "\"requestDir\":" + Json.s(c.defaultRequestDir) + ","
                + "\"responseDir\":" + Json.s(c.defaultResponseDir)
                + "}";
            resp.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static final class RecentServlet extends HttpServlet {
        private final UiServer ui;
        RecentServlet(UiServer ui) { this.ui = ui; }
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("application/json");
            StringBuilder sb = new StringBuilder("[");
            ui.recentLock.lock();
            try {
                boolean first = true;
                for (Map<String, Object> e : ui.recent) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append(Json.obj(e));
                }
            } finally {
                ui.recentLock.unlock();
            }
            sb.append("]");
            resp.getOutputStream().write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static final class SendServlet extends HttpServlet {
        private final UiServer ui;
        SendServlet(UiServer ui) { this.ui = ui; }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String json;
            try (InputStream in = req.getInputStream()) {
                json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            Map<String, String> form = Json.parseFlat(json);

            String host = form.getOrDefault("host", "localhost");
            int port = Integer.parseInt(form.getOrDefault("port", "2222"));
            String user = form.getOrDefault("username", "demo");
            String password = form.getOrDefault("password", "demo");
            String requestDir = form.getOrDefault("requestDir", "inbound");
            String responseDir = form.getOrDefault("responseDir", "outbound");

            String clOrdId = form.getOrDefault("clOrdId", "DEMO-" + System.currentTimeMillis());
            String symbol = form.getOrDefault("symbol", "DEMO");
            String side = form.getOrDefault("side", "1");
            String qty = form.getOrDefault("qty", "100");
            String price = form.getOrDefault("price", "100.00");

            String outgoing = FixBuilder.newOrderSingle("CLIENT", "EXCHANGE", clOrdId, symbol, side, qty, price);
            long startNanos = System.nanoTime();

            Map<String, Object> result = new HashMap<>();
            result.put("clOrdId", clOrdId);
            result.put("symbol", symbol);
            result.put("requestDir", requestDir);
            result.put("responseDir", responseDir);
            result.put("outgoing", outgoing);
            result.put("startedAtMillis", System.currentTimeMillis());

            SftpUploader sftp = new SftpUploader(host, port, user, password);
            Session session = null;
            ChannelSftp channel = null;
            try {
                session = sftp.openSession();
                channel = sftp.openSftp(session);

                String requestFile = "order-" + clOrdId + ".fix";
                sftp.upload(channel, requestDir, requestFile, outgoing);
                LOG.info("Uploaded {}/{}", requestDir, requestFile);

                String responseFile = "exec-" + clOrdId + ".fix";
                String response = waitForResponse(sftp, channel, responseDir, responseFile, 15_000);
                long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
                if (response == null) {
                    result.put("error", "timeout waiting for " + responseDir + "/" + responseFile);
                } else {
                    result.put("incoming", response);
                }
                result.put("roundTripMillis", elapsedMillis);
            } catch (Exception e) {
                LOG.warn("send failed", e);
                result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
                result.put("roundTripMillis", (System.nanoTime() - startNanos) / 1_000_000);
            } finally {
                if (channel != null) try { channel.disconnect(); } catch (Exception ignored) {}
                if (session != null) try { session.disconnect(); } catch (Exception ignored) {}
            }

            ui.recordRecent(result);
            resp.setContentType("application/json");
            resp.getOutputStream().write(Json.obj(result).getBytes(StandardCharsets.UTF_8));
        }

        private String waitForResponse(SftpUploader sftp, ChannelSftp channel, String dir, String fileName, long timeoutMillis) {
            long deadline = System.currentTimeMillis() + timeoutMillis;
            while (System.currentTimeMillis() < deadline) {
                String body = sftp.tryRead(channel, dir, fileName);
                if (body != null && !body.isEmpty()) return body;
                try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
            }
            return null;
        }
    }
}
