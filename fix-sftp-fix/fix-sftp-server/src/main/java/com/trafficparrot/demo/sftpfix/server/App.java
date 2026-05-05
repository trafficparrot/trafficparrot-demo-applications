package com.trafficparrot.demo.sftpfix.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class App {

    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    private App() {}

    public static void main(String[] args) throws Exception {
        Config config = Config.parse(args);
        LOG.info("fix-sftp-server starting: {}", config);

        Path root = Paths.get(config.rootDir).toAbsolutePath();
        Files.createDirectories(root.resolve("inbound"));
        Files.createDirectories(root.resolve("outbound"));
        Files.createDirectories(root.resolve("tp-inbound"));
        Files.createDirectories(root.resolve("tp-outbound"));

        SftpServer sftp = new SftpServer(root, config.sftpPort, config.username, config.password);
        sftp.start();
        LOG.info("SFTP listening on port {} (root: {})", sftp.port(), root);

        BusinessLoop loop = null;
        if (!config.noProcess) {
            loop = new BusinessLoop(root.resolve("inbound"), root.resolve("outbound"));
            loop.start();
            LOG.info("Business loop started (polls inbound/, generates exec into outbound/)");
        } else {
            LOG.info("Business loop DISABLED (--no-process). Server is files-only.");
        }

        UiServer ui = new UiServer(config.uiPort, root, sftp);
        ui.start();
        LOG.info("UI listening on http://localhost:{}/", config.uiPort);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutting down...");
            try { ui.stop(); } catch (Exception e) { LOG.warn("ui stop", e); }
            try { sftp.stop(); } catch (Exception e) { LOG.warn("sftp stop", e); }
        }));

        if (loop != null) {
            loop.joinForever();
        } else {
            Thread.currentThread().join();
        }
    }

    static final class Config {
        int sftpPort = parseInt(System.getenv("SFTP_PORT"), 2222);
        int uiPort = parseInt(System.getenv("UI_PORT"), 8081);
        String rootDir = orDefault(System.getenv("SFTP_ROOT"), "build/sftp-root");
        String username = orDefault(System.getenv("SFTP_USER"), "demo");
        String password = orDefault(System.getenv("SFTP_PASSWORD"), "demo");
        boolean noProcess = "true".equalsIgnoreCase(System.getenv("NO_PROCESS"));

        static Config parse(String[] args) {
            Config c = new Config();
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--sftp-port": c.sftpPort = Integer.parseInt(args[++i]); break;
                    case "--ui-port":   c.uiPort   = Integer.parseInt(args[++i]); break;
                    case "--root":      c.rootDir  = args[++i]; break;
                    case "--user":      c.username = args[++i]; break;
                    case "--password":  c.password = args[++i]; break;
                    case "--no-process": c.noProcess = true; break;
                    case "--help": case "-h":
                        System.out.println("fix-sftp-server [--sftp-port N] [--ui-port N] [--root DIR] [--user U] [--password P] [--no-process]");
                        System.exit(0);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown flag: " + args[i]);
                }
            }
            return c;
        }

        @Override public String toString() {
            return "sftpPort=" + sftpPort + " uiPort=" + uiPort + " root=" + rootDir
                    + " user=" + username + " noProcess=" + noProcess;
        }

        private static int parseInt(String s, int fallback) {
            return s == null || s.isEmpty() ? fallback : Integer.parseInt(s);
        }
        private static String orDefault(String s, String fallback) {
            return s == null || s.isEmpty() ? fallback : s;
        }
    }
}
