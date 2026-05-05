package com.trafficparrot.demo.sftpfix.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class UiServer {

    private final int port;
    private final Path root;
    private final SftpServer sftp;
    private final Server server = new Server();

    public UiServer(int port, Path root, SftpServer sftp) {
        this.port = port;
        this.root = root;
        this.sftp = sftp;
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

        ctx.addServlet(new ServletHolder(new StatusServlet(sftp, root)), "/api/status");
        ctx.addServlet(new ServletHolder(new FileServlet(root)), "/api/file");
        ctx.addServlet(new ServletHolder(new DefaultServlet()), "/");

        server.setHandler(ctx);
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }

    private static final class StatusServlet extends HttpServlet {
        private final SftpServer sftp;
        private final Path root;

        StatusServlet(SftpServer sftp, Path root) {
            this.sftp = sftp;
            this.root = root;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("application/json");
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"sftpPort\":").append(sftp.port()).append(",");
            json.append("\"root\":").append(jsonString(root.toString())).append(",");
            json.append("\"sessions\":[");
            json.append(sftp.sessions().values().stream().map(s ->
                "{\"id\":" + jsonString(s.id())
                + ",\"username\":" + jsonString(s.username())
                + ",\"clientAddress\":" + jsonString(s.clientAddress())
                + ",\"connectedAtMillis\":" + s.connectedAtMillis() + "}"
            ).collect(Collectors.joining(",")));
            json.append("],\"directories\":{");
            String[] dirs = {"inbound", "outbound", "tp-inbound", "tp-outbound"};
            for (int i = 0; i < dirs.length; i++) {
                if (i > 0) json.append(",");
                json.append(jsonString(dirs[i])).append(":[");
                List<Map<String, Object>> files = listDir(root.resolve(dirs[i]));
                for (int j = 0; j < files.size(); j++) {
                    if (j > 0) json.append(",");
                    Map<String, Object> f = files.get(j);
                    json.append("{")
                            .append("\"name\":").append(jsonString((String) f.get("name"))).append(",")
                            .append("\"size\":").append(f.get("size")).append(",")
                            .append("\"mtime\":").append(f.get("mtime"))
                            .append("}");
                }
                json.append("]");
            }
            json.append("}}");
            resp.getOutputStream().write(json.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static List<Map<String, Object>> listDir(Path dir) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) return out;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) {
                    Map<String, Object> m = new TreeMap<>();
                    m.put("name", p.getFileName().toString());
                    m.put("size", Files.size(p));
                    m.put("mtime", Files.getLastModifiedTime(p).toMillis());
                    out.add(m);
                }
            }
        } catch (IOException ignored) {}
        out.sort((a, b) -> Long.compare((long) b.get("mtime"), (long) a.get("mtime")));
        return out;
    }

    private static final class FileServlet extends HttpServlet {
        private final Path root;
        FileServlet(Path root) { this.root = root; }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String dir = req.getParameter("dir");
            String name = req.getParameter("name");
            if (dir == null || name == null || dir.contains("..") || name.contains("..") || name.contains("/")) {
                resp.sendError(400, "Bad params");
                return;
            }
            Path file = root.resolve(dir).resolve(name).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                resp.sendError(404, "Not found");
                return;
            }
            resp.setContentType("text/plain; charset=UTF-8");
            byte[] bytes = Files.readAllBytes(file);
            resp.getOutputStream().write(bytes);
        }
    }

    private static String jsonString(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    @SuppressWarnings("unused")
    private static String prettyBytes(long bytes) {
        return NumberFormat.getInstance().format(bytes);
    }
}
