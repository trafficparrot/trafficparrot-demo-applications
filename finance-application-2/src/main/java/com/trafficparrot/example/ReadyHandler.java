package com.trafficparrot.example;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

class ReadyHandler extends Handler.Abstract {

    @Override
    public boolean handle(Request request, Response response, Callback callback) {
        if ("/ready".equals(Request.getPathInContext(request))) {
            response.setStatus(HttpStatus.OK_200);
            callback.succeeded();
            return true;
        }
        return false;
    }
}
