package com.trafficparrot.example.testing.framework;

import com.browserup.bup.filters.ResponseFilter;
import com.browserup.bup.util.HttpMessageContents;
import com.browserup.bup.util.HttpMessageInfo;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.qameta.allure.Attachment;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.System.lineSeparator;

public class RecordNetworkActivity implements ResponseFilter {
    private final List<String> requests = new CopyOnWriteArrayList<>();

    @Override
    public void filterResponse(HttpResponse httpResponse, HttpMessageContents httpMessageContents, HttpMessageInfo httpMessageInfo) {
        HttpRequest httpRequest = httpMessageInfo.getOriginalRequest();
        HttpResponseStatus responseStatus = httpResponse.status();
        requests.add(responseStatus.code() + " " + responseStatus.reasonPhrase() + " - " + httpRequest.method() + " " + httpMessageInfo.getUrl());
    }

    @Attachment(value = "Browser network requests", type = "text/plain")
    public String renderNetworkActivity() {
        return String.join(lineSeparator(), requests);
    }
}
