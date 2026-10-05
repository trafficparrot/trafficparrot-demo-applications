package com.trafficparrot.example;

import com.github.tomakehurst.wiremock.http.RequestListener;
import com.lsd.core.LsdContext;
import com.lsd.core.domain.Participant;

import static com.lsd.core.builders.MessageBuilder.messageBuilder;
import static com.lsd.core.domain.MessageType.SYNCHRONOUS_RESPONSE;
import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;

public class LogWireMockInLsd implements RequestListener {
    private final LsdContext lsd;
    private final Participant sourceSystem;
    private final Participant targetSystem;

    public LogWireMockInLsd(LsdContext lsd, Participant sourceSystem, Participant targetSystem) {
        this.lsd = lsd;
        this.sourceSystem = sourceSystem;
        this.targetSystem = targetSystem;
    }

    @Override
    public void requestReceived(com.github.tomakehurst.wiremock.http.Request request, com.github.tomakehurst.wiremock.http.Response response) {
        lsd.capture(
                messageBuilder().from(sourceSystem).to(targetSystem)
                        .label(request.getMethod() + " " + request.getUrl())
                        .data(toString(request))
                        .build(),
                messageBuilder().from(targetSystem).to(sourceSystem)
                        .label("HTTP " + response.getStatus())
                        .data(toString(response))
                        .type(SYNCHRONOUS_RESPONSE)
                        .build());
    }

    private String toString(com.github.tomakehurst.wiremock.http.Response response) {
        StringBuilder result = new StringBuilder();
        result.append("HTTP").append(" ").append(response.getStatus()).append("\n");
        if (response.getHeaders() != null) {
            response.getHeaders().all().forEach(h -> result.append(h.key()).append(": ").append(join(",", h.values())).append("\n"));
        }
        String body = response.getBody() == null ? "" : new String(response.getBody(), UTF_8);
        result.append("\n").append("\n").append(body);
        return result.toString();
    }

    private String toString(com.github.tomakehurst.wiremock.http.Request request) {
        StringBuilder result = new StringBuilder();
        result.append(request.getMethod()).append(" ").append(request.getUrl()).append("\n");
        request.getHeaders().all().forEach(h -> result.append(h.key()).append(": ").append(join(",", h.values())).append("\n"));
        String body = request.getBody() == null ? "" : new String(request.getBody(), UTF_8);
        result.append("\n").append("\n").append(body);
        return result.toString();
    }
}
