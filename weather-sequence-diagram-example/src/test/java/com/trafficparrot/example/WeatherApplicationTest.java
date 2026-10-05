package com.trafficparrot.example;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.lsd.core.LsdContext;
import com.lsd.core.domain.NoteOver;
import com.lsd.core.domain.Participant;
import io.lsdconsulting.junit5.LsdExtension;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Arrays;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.lsd.core.builders.MessageBuilder.messageBuilder;
import static com.lsd.core.domain.MessageType.SYNCHRONOUS_RESPONSE;
import static com.lsd.core.domain.ParticipantType.ACTOR;
import static com.lsd.core.domain.ParticipantType.BOUNDARY;
import static com.lsd.core.domain.ParticipantType.PARTICIPANT;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Each test becomes a scenario in an HTML report, target/lsd/, with a sequence diagram of the HTTP
 * traffic between the client, the weather application and the DarkSky API WireMock stands in for.
 */
@ExtendWith(LsdExtension.class)
class WeatherApplicationTest {
    private static final Participant CLIENT = ACTOR.called("Client");
    private static final Participant WEATHER_APPLICATION = PARTICIPANT.called("WeatherApplication");
    private static final Participant DARK_SKY = BOUNDARY.called("DarkSky");
    private static final String DARK_SKY_LONDON_FORECAST = "/forecast/e67b0e3784104669340c3cb089412b67/51.507253,-0.127755";

    private final LsdContext lsd = LsdContext.getInstance();
    private final WeatherApplication weatherApplication = new WeatherApplication();
    private final WireMockServer darkSkyAPIStub = new WireMockServer(8080);

    private HttpResponse httpResponse;
    private String responseBody;

    @BeforeEach
    void setUp() {
        darkSkyAPIStub.addMockServiceRequestListener(new LogWireMockInLsd(lsd, WEATHER_APPLICATION, DARK_SKY));
        darkSkyAPIStub.start();
        weatherApplication.start();
    }

    @AfterEach
    void tearDown() {
        weatherApplication.stop();
        darkSkyAPIStub.stop();
    }

    @Test
    void servesWindSpeedBasedOnDarkSkyResponse() throws IOException {
        lsd.capture(new NoteOver("The DarkSky response is quite big and complex, our weather application extracts one attribute from it", WEATHER_APPLICATION));
        givenDarkSkyForecastForLondonContainsWindSpeed("12.34");
        whenIRequestForecast();
        thenTheWindSpeedIs("12.34mph");
    }

    @ParameterizedTest(name = "DarkSky responds {0}")
    @DisplayName("Reports an error when DarkSky does not succeed")
    @ValueSource(ints = {500, 503})
    void reportsErrorWhenDarkSkyReturnsANonSuccessfulResponse(int darkSkyResponseCode) throws IOException {
        givenDarkSkyReturnsAnError(darkSkyResponseCode);
        whenIRequestForecast();
        thenTheResponseContains("Error while fetching data from DarkSky APIs");
    }

    private void thenTheResponseContains(String error) {
        assertEquals(503, httpResponse.getStatusLine().getStatusCode());
        assertEquals(error, responseBody);
    }

    private void givenDarkSkyReturnsAnError(int status) {
        lsd.capture(new NoteOver("Responds with status " + status, DARK_SKY));
        darkSkyAPIStub.stubFor(get(urlEqualTo(DARK_SKY_LONDON_FORECAST))
                .willReturn(aResponse().withStatus(status)));
    }

    private void whenIRequestForecast() throws IOException {
        Request get = Request.Get("http://localhost:" + weatherApplication.port() + "/wind-speed");
        lsd.capture(messageBuilder().from(CLIENT).to(WEATHER_APPLICATION).label("GET /wind-speed").data(get.toString()).build());
        httpResponse = get.execute().returnResponse();
        responseBody = EntityUtils.toString(httpResponse.getEntity(), UTF_8);
        lsd.capture(messageBuilder().from(WEATHER_APPLICATION).to(CLIENT)
                .label("HTTP " + httpResponse.getStatusLine().getStatusCode())
                .data(toString(httpResponse, responseBody))
                .type(SYNCHRONOUS_RESPONSE)
                .build());
    }

    private String toString(HttpResponse response, String responseBody) {
        StringBuilder result = new StringBuilder();
        result.append("HTTP").append(" ").append(response.getStatusLine().getStatusCode()).append("\n");
        if (response.getAllHeaders() != null) {
            Arrays.stream(response.getAllHeaders()).forEach(h -> result.append(h.getName()).append(": ").append(h.getValue()).append("\n"));
        }
        result.append("\n").append("\n").append(responseBody);
        return result.toString();
    }

    private void thenTheWindSpeedIs(String expected) {
        assertEquals(expected, responseBody);
    }

    private void givenDarkSkyForecastForLondonContainsWindSpeed(String windSpeed) throws IOException {
        lsd.capture(new NoteOver("Wind speed in London: " + windSpeed, DARK_SKY));
        darkSkyAPIStub.stubFor(get(urlEqualTo(DARK_SKY_LONDON_FORECAST))
                .willReturn(aResponse().withBody(darkSkyResponseBody(windSpeed))));
    }

    private String darkSkyResponseBody(String windSpeed) throws IOException {
        return format(IOUtils.toString(getClass().getClassLoader().getResourceAsStream("darksky-response-body.json"), UTF_8), windSpeed);
    }
}
