/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Based on https://thrift.apache.org/tutorial/java modifications copyright Traffic Parrot 2020-2026
 */
package com.trafficparrot.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The calculator client: a web page, served on this machine only, whose buttons call the Thrift calculator server.
 */
public class ThriftCalculatorClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThriftCalculatorClient.class);
    private static final Map<String, String> PAGES = Map.of(
            "/", "text/html; charset=utf-8",
            "/calculator.css", "text/css; charset=utf-8",
            "/calculator.js", "text/javascript; charset=utf-8");

    private final CalculatorDisplay display = new CalculatorDisplay();
    private final HttpServer server;

    public ThriftCalculatorClient(int port) throws IOException {
        // Loopback only: the page makes this process connect to whatever host:port is typed into it
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/", this::handle);
    }

    public static void main(String[] args) throws IOException {
        LOGGER.info("Starting Thrift calculator client");
        ThriftCalculatorClient client = new ThriftCalculatorClient(ThriftCalculatorClientProperties.loadProperties().httpPort());
        URI page = client.start();
        LOGGER.info("Thrift calculator client is at {}", page);
        openInBrowser(page);
    }

    public URI start() {
        server.start();
        return URI.create("http://localhost:" + server.getAddress().getPort() + "/");
    }

    public void stop() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            if (!isFromThisPage(exchange)) {
                send(exchange, 403, "text/plain; charset=utf-8", "Forbidden");
                return;
            }
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (PAGES.containsKey(path) && method.equals("GET")) {
                sendPage(exchange, path);
            } else if (path.equals("/api/state") && method.equals("GET")) {
                sendState(exchange);
            } else if (path.equals("/api/press") && method.equals("POST")) {
                String key = form(exchange).get("key");
                if (key == null || !CalculatorDisplay.KEYS.contains(key)) {
                    send(exchange, 400, "text/plain; charset=utf-8", "Unknown key");
                    return;
                }
                try {
                    display.press(key);
                } catch (RuntimeException e) {
                    LOGGER.warn("Pressing {} failed", key, e);
                }
                sendState(exchange);
            } else if (path.equals("/api/server") && method.equals("POST")) {
                String hostAndPort = form(exchange).get("hostAndPort");
                try {
                    display.hostAndPort(hostAndPort == null ? "" : hostAndPort);
                } catch (RuntimeException e) {
                    LOGGER.warn("Thrift calculator server {} not understood", hostAndPort, e);
                }
                exchange.sendResponseHeaders(204, -1);
            } else {
                send(exchange, 404, "text/plain; charset=utf-8", "Not found");
            }
        } finally {
            exchange.close();
        }
    }

    // Refuses another site's page driving this one (Origin), and a DNS name rebound to this machine (Host)
    private boolean isFromThisPage(HttpExchange exchange) {
        String host = exchange.getRequestHeaders().getFirst("Host");
        int port = server.getAddress().getPort();
        if (!("localhost:" + port).equals(host) && !("127.0.0.1:" + port).equals(host)) {
            return false;
        }
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        return origin == null || origin.equals("http://" + host);
    }

    private void sendPage(HttpExchange exchange, String path) throws IOException {
        String resource = "web" + (path.equals("/") ? "/index.html" : path);
        try (InputStream page = ThriftCalculatorClient.class.getClassLoader().getResourceAsStream(resource)) {
            if (page == null) {
                send(exchange, 404, "text/plain; charset=utf-8", "Not found");
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", PAGES.get(path));
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream body = exchange.getResponseBody()) {
                page.transferTo(body);
            }
        }
    }

    private void sendState(HttpExchange exchange) throws IOException {
        String state = "{\"total\":" + display.total()
                + ",\"current\":" + display.current()
                + ",\"message\":" + json(display.message())
                + ",\"host\":" + json(display.host())
                + ",\"port\":" + display.port()
                + ",\"digitsDisabled\":" + display.digitsDisabled() + "}";
        send(exchange, 200, "application/json", state);
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static Map<String, String> form(HttpExchange exchange) throws IOException {
        Map<String, String> fields = new HashMap<>();
        String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
        for (String field : body.split("&")) {
            int equals = field.indexOf('=');
            if (equals > 0) {
                fields.put(URLDecoder.decode(field.substring(0, equals), UTF_8), URLDecoder.decode(field.substring(equals + 1), UTF_8));
            }
        }
        return fields;
    }

    private static String json(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c < 0x20) {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.append('"').toString();
    }

    private static void openInBrowser(URI page) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(page);
                return;
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.debug("Could not open a browser", e);
        }
        LOGGER.info("Open {} in a web browser", page);
    }
}
