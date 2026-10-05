package com.trafficparrot.example;

import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.layered.TFramedTransport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThriftCalculatorClientTest {
    private final HttpClient http = HttpClient.newHttpClient();
    private TServer thriftServer;
    private ThriftCalculatorClient client;
    private URI page;

    @BeforeEach
    void startServerAndClient() throws Exception {
        TServerSocket socket = new TServerSocket(0);
        thriftServer = new TSimpleServer(new TServer.Args(socket)
                .transportFactory(new TFramedTransport.Factory())
                .processor(new Calculator.Processor<>(new ThriftCalculatorHandler())));
        new Thread(thriftServer::serve).start();

        client = new ThriftCalculatorClient(0);
        page = client.start();
        assertEquals(204, post("api/server", "hostAndPort", "localhost:" + socket.getServerSocket().getLocalPort()).statusCode());
    }

    @AfterEach
    void stopServerAndClient() {
        client.stop();
        thriftServer.stop();
    }

    @Test
    void calculatesOnTheThriftServer() throws Exception {
        press("5", "+", "7");
        String state = press("=");

        assertTrue(state.contains("\"total\":12,"), state);
        assertTrue(state.contains("\"message\":\"5 + 7 = 12\""), state);
        assertTrue(state.contains("\"digitsDisabled\":true"), state);
    }

    @Test
    void ignoresDigitsAfterEqualsUntilAnOperation() throws Exception {
        press("5", "+", "7", "=", "3");
        String state = press("+", "3", "=");

        assertTrue(state.contains("\"message\":\"12 + 3 = 15\""), state);
    }

    @Test
    void showsTheServersAnswerToDivideByZero() throws Exception {
        String state = press("8", "/", "0", "=");

        assertTrue(state.contains("\"total\":8,"), state);
        assertTrue(state.contains("\"message\":\"Error with / Cannot divide by 0\""), state);
    }

    @Test
    void previousMovesBackThroughTheServersHistory() throws Exception {
        press("5", "+", "7", "=", "+", "3", "=");
        String state = press("previous");

        assertTrue(state.contains("\"total\":12,"), state);
        assertTrue(state.contains("\"message\":\"Moved to previous total 12\""), state);
    }

    @Test
    void clearResetsTheTotal() throws Exception {
        press("5", "+", "7", "=");
        String state = press("c");

        assertTrue(state.contains("\"total\":0,\"current\":0,\"message\":\"0\""), state);
        assertTrue(state.contains("\"digitsDisabled\":false"), state);
    }

    @Test
    void reportsAServerItCannotReach() throws Exception {
        post("api/server", "hostAndPort", "localhost:1");
        String state = press("1", "+", "1", "=");

        assertTrue(state.contains("\"message\":\"java.net.ConnectException: Connection refused\""), state);
    }

    @Test
    void servesThePage() throws Exception {
        HttpResponse<String> index = http.send(HttpRequest.newBuilder(page).build(), HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> script = http.send(HttpRequest.newBuilder(page.resolve("calculator.js")).build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(200, index.statusCode());
        assertTrue(index.body().contains("<title>Thrift Remote Calculator Client</title>"));
        assertEquals(200, script.statusCode());
        assertEquals(404, http.send(HttpRequest.newBuilder(page.resolve("pom.xml")).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test
    void refusesAPressFromAnotherSitesPage() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(page.resolve("api/press"))
                .header("Origin", "http://example.com")
                .POST(HttpRequest.BodyPublishers.ofString("key=5"))
                .build();

        assertEquals(403, http.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test
    void refusesAnUnknownKey() throws Exception {
        assertEquals(400, post("api/press", "key", "%").statusCode());
    }

    private String press(String... keys) throws Exception {
        String state = null;
        for (String key : keys) {
            HttpResponse<String> response = post("api/press", "key", key);
            assertEquals(200, response.statusCode(), response.body());
            state = response.body();
        }
        return state;
    }

    private HttpResponse<String> post(String path, String field, String value) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(page.resolve(path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(field + "=" + URLEncoder.encode(value, UTF_8)))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
