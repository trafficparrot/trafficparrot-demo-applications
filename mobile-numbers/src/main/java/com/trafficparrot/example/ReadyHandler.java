package com.trafficparrot.example;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import static org.eclipse.jetty.http.HttpStatus.OK_200;

class ReadyHandler extends Handler.Abstract {

    @Override
    public boolean handle(Request request, Response response, Callback callback) {
        if (!"/ready".equals(Request.getPathInContext(request))) {
            return false;
        }
        response.setStatus(OK_200);
        callback.succeeded();
        return true;
    }
}
