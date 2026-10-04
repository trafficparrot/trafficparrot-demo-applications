package com.wbsoftwareconsutlancy;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import static com.wbsoftwareconsutlancy.AppProperties.loadProperties;
import static java.lang.String.format;

class StockQuoteLastPriceHandler extends Handler.Abstract {
    private static final String APPLE_SYMBOL = "AAPL";

    public StockQuoteLastPriceHandler() {
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        if ("/stock-quote-last-price".equals(Request.getPathInContext(request))) {
            double lastPrice = parseStockQuoteLastPrice(markitStockQuoteFor(APPLE_SYMBOL));

            response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/html; charset=UTF-8");
            response.setStatus(HttpStatus.OK_200);
            Content.Sink.write(response, true, String.valueOf(lastPrice), callback);
            return true;
        }
        return false;
    }

    private double parseStockQuoteLastPrice(String markitStockQuoteJson) throws JSONException {
        return new JSONObject(markitStockQuoteJson).getDouble("LastPrice");
    }

    private String markitStockQuoteFor(String symbol) throws IOException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(format(getMarkitUrl(), symbol));
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

    private String getMarkitUrl() {
        return loadProperties().getProperty("finance-application.markit.url");
    }
}
