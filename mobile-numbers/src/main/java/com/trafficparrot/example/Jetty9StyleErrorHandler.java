package com.trafficparrot.example;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;

import java.io.IOException;
import java.io.Writer;

/**
 * The transfer page shows the error page inline when a transfer fails, so the sad path demos read the
 * port number API's answer from it. This keeps Jetty 9's layout: a short heading with the reason
 * beneath it, rather than Jetty 12's heading that carries the whole message.
 */
class Jetty9StyleErrorHandler extends ErrorHandler {
    Jetty9StyleErrorHandler() {
        setShowMessageInTitle(false);
    }

    @Override
    protected void writeErrorHtmlMessage(Request request, Writer writer, int code, String message, Throwable cause, String uri) throws IOException {
        writer.write("<h2>HTTP ERROR: " + code + "</h2>\n<p>Problem accessing ");
        write(writer, Request.getPathInContext(request));
        writer.write(". Reason:\n<pre>    ");
        write(writer, message);
        writer.write("</pre></p>\n");
    }
}
