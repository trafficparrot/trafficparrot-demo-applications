package com.trafficparrot.example;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

import static java.lang.String.format;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.eclipse.jetty.http.HttpStatus.SERVICE_UNAVAILABLE_503;

class UnitHandler extends Handler.Abstract {
    private static final Logger LOG = LoggerFactory.getLogger(UnitHandler.class);
    private final Properties properties;

    public UnitHandler(Properties properties) {
        this.properties = properties;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) {
        if (!"/convert".equals(Request.getPathInContext(request))) {
            return false;
        }
        String body;
        try {
            body = parseResult(yardsToMeters(Request.extractQueryParameters(request).getValue("yards")));
            response.setStatus(OK_200);
        } catch (Exception e) {
            LOG.error("Unknown problem while retrieving wind speed", e);
            response.setStatus(SERVICE_UNAVAILABLE_503);
            body = "ERROR";
        }
        Content.Sink.write(response, true, body, callback);
        return true;
    }

    private String parseResult(String forecastIo) throws JSONException, IOException, SAXException, XPathExpressionException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(forecastIo.getBytes()));
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile("//Envelope/Body/ChangeLengthUnitResponse/ChangeLengthUnitResult/text()");
        Object evaluate = expr.evaluate(doc, XPathConstants.STRING);
        return (String) evaluate;
    }

    private String yardsToMeters(String yards) throws IOException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(getWebserviceUrl() + "/length.asmx");
            httpPost.addHeader("accept-encoding", "identity");
            httpPost.addHeader("Accept", "text/xml");
            StringEntity stringEntity = new StringEntity(format("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                    "  <soap12:Body>\n" +
                    "    <ChangeLengthUnit xmlns=\"http://www.webserviceX.NET/\">\n" +
                    "      <LengthValue>%s</LengthValue>\n" +
                    "      <fromLengthUnit>Yards</fromLengthUnit>\n" +
                    "      <toLengthUnit>Meters</toLengthUnit>\n" +
                    "    </ChangeLengthUnit>\n" +
                    "  </soap12:Body>\n" +
                    "</soap12:Envelope>", yards), ContentType.create("application/soap+xml"));
            LOG.info(stringEntity.toString());
            LOG.info(EntityUtils.toString(stringEntity));
            httpPost.setEntity(stringEntity);
            ResponseHandler<String> responseHandler = response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    LOG.error(EntityUtils.toString(response.getEntity()));
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            };
            return httpclient.execute(httpPost, responseHandler);
        }
    }

    private String getWebserviceUrl() {
        return properties.getProperty("unit-converter-application.converter.url");
    }
}
