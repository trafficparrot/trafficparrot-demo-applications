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
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;

import static com.trafficparrot.example.AppProperties.loadProperties;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

class MobileNumbersHandler extends Handler.Abstract {
    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        if (!"/transfer-number".equals(Request.getPathInContext(request))) {
            return false;
        }
        String mobileNumber = Request.getParameters(request).getValue("mobileNumber");
        String responseBody;
        try {
            responseBody = transferNumber(mobileNumber);
        } catch (IOException e) {
            // The error page then reads "Unexpected response status: 500 with response body: ...", so a sad
            // path shows the port number API's own answer, as it did on Jetty 9
            Response.writeError(request, response, callback, INTERNAL_SERVER_ERROR_500, e.getMessage());
            return true;
        }

        response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/html; charset=utf-8");
        response.setStatus(OK_200);
        Content.Sink.write(response, true, responseBody, callback);
        return true;
    }

    private String transferNumber(String mobileNumber) throws IOException {
        String responseBody = requestPortNumber(mobileNumber);
        // ignore on purpose to demo sad path scenarios
        JSONObject response = new JSONObject(responseBody);
        return "{\n" +
                "  \"status\": \"SUCCESS\",\n" +
                "  \"mobileNumber\": \"" + mobileNumber + "\",\n" +
                "  \"message\": \"Successfully requested user mobile number transfer to our mobile network\",\n" +
                "  \"date\": \"" + new Date() + "\"\n" +
                "}";
    }

    private String requestPortNumber(String mobileNumber) throws IOException {
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
