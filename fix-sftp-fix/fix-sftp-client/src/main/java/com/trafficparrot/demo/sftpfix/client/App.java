package com.trafficparrot.demo.sftpfix.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class App {

    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    private App() {}

    public static void main(String[] args) throws Exception {
        Config config = Config.parse(args);
        LOG.info("fix-sftp-client starting: {}", config);
        UiServer ui = new UiServer(config.uiPort, config);
        ui.start();
        LOG.info("UI listening on http://localhost:{}/", config.uiPort);
        Thread.currentThread().join();
    }

    static final class Config {
        int uiPort = parseInt(System.getenv("UI_PORT"), 8082);
        String defaultHost = orDefault(System.getenv("SFTP_HOST"), "localhost");
        int defaultPort = parseInt(System.getenv("SFTP_PORT"), 2222);
        String defaultUser = orDefault(System.getenv("SFTP_USER"), "demo");
        String defaultPassword = orDefault(System.getenv("SFTP_PASSWORD"), "demo");
        String defaultRequestDir = orDefault(System.getenv("REQUEST_DIR"), "inbound");
        String defaultResponseDir = orDefault(System.getenv("RESPONSE_DIR"), "outbound");

        static Config parse(String[] args) {
            Config c = new Config();
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--ui-port":      c.uiPort = Integer.parseInt(args[++i]); break;
                    case "--sftp-host":    c.defaultHost = args[++i]; break;
                    case "--sftp-port":    c.defaultPort = Integer.parseInt(args[++i]); break;
                    case "--user":         c.defaultUser = args[++i]; break;
                    case "--password":     c.defaultPassword = args[++i]; break;
                    case "--request-dir":  c.defaultRequestDir = args[++i]; break;
                    case "--response-dir": c.defaultResponseDir = args[++i]; break;
                    case "--help": case "-h":
                        System.out.println("fix-sftp-client [--ui-port N] [--sftp-host H] [--sftp-port N] [--user U] [--password P] [--request-dir D] [--response-dir D]");
                        System.exit(0);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown flag: " + args[i]);
                }
            }
            return c;
        }

        @Override public String toString() {
            return "uiPort=" + uiPort + " sftp=" + defaultUser + "@" + defaultHost + ":" + defaultPort
                    + " req=" + defaultRequestDir + " resp=" + defaultResponseDir;
        }

        private static int parseInt(String s, int fallback) {
            return s == null || s.isEmpty() ? fallback : Integer.parseInt(s);
        }
        private static String orDefault(String s, String fallback) {
            return s == null || s.isEmpty() ? fallback : s;
        }
    }
}
