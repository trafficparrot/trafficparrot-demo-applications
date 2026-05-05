package com.trafficparrot.demo.sftpfix.client;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class FixBuilder {

    public static final char SOH = (char) 0x01;

    private FixBuilder() {}

    public static String newOrderSingle(String sender, String target, String clOrdId, String symbol,
                                        String side, String qty, String price) {
        StringBuilder body = new StringBuilder();
        body.append("35=D").append(SOH);
        body.append("49=").append(sender).append(SOH);
        body.append("56=").append(target).append(SOH);
        body.append("34=1").append(SOH);
        body.append("52=").append(nowFixUtc()).append(SOH);
        body.append("11=").append(clOrdId).append(SOH);
        body.append("21=1").append(SOH);
        body.append("55=").append(symbol).append(SOH);
        body.append("54=").append(side).append(SOH);
        body.append("60=").append(nowFixUtc()).append(SOH);
        body.append("38=").append(qty).append(SOH);
        body.append("40=2").append(SOH);
        body.append("44=").append(price).append(SOH);
        body.append("59=0").append(SOH);

        int bodyLength = body.toString().getBytes(StandardCharsets.US_ASCII).length;
        StringBuilder full = new StringBuilder();
        full.append("8=FIX.4.4").append(SOH);
        full.append("9=").append(bodyLength).append(SOH);
        full.append(body);
        int checksum = 0;
        for (byte b : full.toString().getBytes(StandardCharsets.US_ASCII)) checksum += (b & 0xff);
        checksum %= 256;
        full.append("10=").append(String.format("%03d", checksum)).append(SOH);
        return full.toString();
    }

    private static String nowFixUtc() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }
}
