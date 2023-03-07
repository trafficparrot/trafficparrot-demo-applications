package com.trafficparrot.example;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
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
import java.io.DataInput;
import java.io.IOException;
import java.util.Date;
import java.util.stream.Collectors;

import static com.trafficparrot.example.AppProperties.loadProperties;
import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_OK;

class MobileNumbersHandler extends AbstractHandler {
    public MobileNumbersHandler() {
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if ("/transfer-number".equals(target)) {
            try {
                String responseBody = transferNumber(request);

                response.setContentType("text/html; charset=utf-8");
                response.setStatus(SC_OK);
                response.getWriter().print(responseBody);

                baseRequest.setHandled(true);
            } catch (JSONException e) {
                throw new ServletException(e);
            }
        }
    }

    private String transferNumber(HttpServletRequest request) throws JSONException, IOException {
        String mobileNumber = request.getParameter("mobileNumber");
        String responseBody = transferNumber(mobileNumber);
        // ignore on purpose to demo sad path scenarios
        JSONObject response = new JSONObject(responseBody);
        return "{\n" +
                "  \"status\": \"SUCCESS\",\n" +
                "  \"mobileNumber\": \"" + mobileNumber + "\",\n" +
                "  \"message\": \"Successfully requested user mobile number transfer to our mobile network\",\n" +
                "  \"date\": \"" + new Date() + "\"\n" +
                "}";
    }

    private String transferNumber(String mobileNumber) throws IOException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(getPortNumberUrl());

            String json = " {\"mobileNumber\":\"" + mobileNumber + "\",\"mobileType\":\"data-and-voice\"}";
            StringEntity entity = new StringEntity(json);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");

            System.out.println("Sending request to " + httpPost.getURI());

            ResponseHandler<String> responseHandler = response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    return responseString(response);
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status + " with response body: " + responseString(response));
                }
            };
            String responseBody = httpclient.execute(httpPost, responseHandler);
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

    private String getPortNumberUrl() {
        return loadProperties().getProperty("port.number.url");
    }
}
