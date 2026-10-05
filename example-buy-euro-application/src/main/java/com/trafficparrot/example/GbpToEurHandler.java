package com.trafficparrot.example;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Properties;

import static java.lang.String.format;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

class GbpToEurHandler extends Handler.Abstract {
    private final Properties properties;

    public GbpToEurHandler(Properties properties) {
        this.properties = properties;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        if (!"/gbp-to-eur".equals(Request.getPathInContext(request))) {
            return false;
        }
        double gbpToEur = parse(convertGbpToEur());

        response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/html; charset=utf-8");
        response.setStatus(OK_200);
        Content.Sink.write(response, true, format("{\"gbpToEur\": %s, \"buy\": %s}", gbpToEur, gbpToEur > 1.5), callback);
        return true;
    }

    private double parse(String forecastIo) {
        // getDouble, not getString: org.json's getString refuses a number, and Result is one
        return new JSONObject(forecastIo).getDouble("Result");
    }

    private String convertGbpToEur() throws IOException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(getXigniteUrl() + "/xGlobalCurrencies.json/ConvertRealTimeValue?_Token=" + getXigniteToken() + "&From=GBP&To=EUR&Amount=1");
            httpget.addHeader("accept-encoding", "identity");
            System.out.println("Executing request " + httpget.getRequestLine());

            ResponseHandler<String> responseHandler = response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            };
            String responseBody = httpclient.execute(httpget, responseHandler);
            System.out.println("----------------------------------------");
            System.out.println(responseBody);
            return responseBody;
        }
    }

    private String getXigniteUrl() {
        return properties.getProperty("xignite.url");
    }

    private String getXigniteToken() {
        return properties.getProperty("xignite.token");
    }
}
