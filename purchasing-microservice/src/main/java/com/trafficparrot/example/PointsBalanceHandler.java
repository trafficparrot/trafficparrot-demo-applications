package com.trafficparrot.example;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.trafficparrot.example.AppProperties.loadProperties;
import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_OK;

class PointsBalanceHandler extends AbstractHandler {
    private static final String LOGGED_IN_USERNAME = "bobsmith";

    public PointsBalanceHandler() {
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if ("/points-balance".equals(target)) {
            try {
                double lastPrice = pointsBalance(pointsFor(LOGGED_IN_USERNAME));

                response.setContentType("text/html; charset=utf-8");
                response.setStatus(SC_OK);
                response.getWriter().print(lastPrice);

                baseRequest.setHandled(true);
            } catch (JSONException e) {
                throw new ServletException(e);
            }
        }
    }

    private double pointsBalance(String username) throws JSONException {
        return new JSONObject(username).getDouble("LoyaltyPoints");
    }

    private String pointsFor(String username) throws IOException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(format(getLoyaltyPointsUrl(), username));
            httpget.addHeader("accept-encoding", "identity");
            System.out.println("Executing request " + httpget.getRequestLine());

            ResponseHandler<String> responseHandler = response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    return responseString(response);
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status + " with response body: " + responseString(response));
                }
            };
            String responseBody = httpclient.execute(httpget, responseHandler);
            System.out.println("----------------------------------------");
            System.out.println(responseBody);
            return responseBody;
        }
    }

    private String responseString(HttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return null;
        }
        return EntityUtils.toString(entity);
    }

    private String getLoyaltyPointsUrl() {
        return loadProperties().getProperty("purchasing-microservice.stock.quote.url");
    }
}
