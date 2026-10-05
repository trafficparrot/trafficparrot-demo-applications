package com.wbsoftwareconsutlancy;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

import static java.lang.String.format;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.eclipse.jetty.http.HttpStatus.SERVICE_UNAVAILABLE_503;

class WindHandler extends Handler.Abstract {
    private static final Logger LOG = LoggerFactory.getLogger(WindHandler.class);

    public static final String LONDON_LATITUDE = "51.507253";
    public static final String LONDON_LONGITUDE = "-0.127755";
    private final Properties properties;

    public WindHandler(Properties properties) {
        this.properties = properties;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) {
        if (!"/wind-speed".equals(Request.getPathInContext(request))) {
            return false;
        }
        String body;
        try {
            body = parseWindSpeed(forecastIoFor(LONDON_LATITUDE, LONDON_LONGITUDE)) + "mph";
            response.setStatus(OK_200);
        } catch (Exception e) {
            LOG.error("Unknown problem while retrieving wind speed", e);
            response.setStatus(SERVICE_UNAVAILABLE_503);
            body = "ERROR";
        }
        Content.Sink.write(response, true, body, callback);
        return true;
    }

    private String parseWindSpeed(String forecastIo) throws JSONException {
        // get().toString(), not getString(): the forecast's windSpeed is a number, which getString now refuses
        return new JSONObject(forecastIo).getJSONObject("currently").get("windSpeed").toString();
    }

    private String forecastIoFor(String latitude, String longitude) throws IOException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(format(getForecastIoUrl() + "/%s,%s", latitude, longitude));
            httpget.addHeader("accept-encoding", "identity");

            ResponseHandler<String> responseHandler = response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            };
            return httpclient.execute(httpget, responseHandler);
        }
    }

    private String getForecastIoUrl() {
        return properties.getProperty("weather-application.forecastio.url");
    }
}
