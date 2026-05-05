package com.trafficparrot.demo.sftpfix.server;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Hand-rolled FIX 4.4 helpers. Single concern: parse + build messages with valid
 * BodyLength and CheckSum. Only enough surface for a demo (NewOrderSingle parse,
 * ExecutionReport build).
 */
public final class FixMessage {

    public static final char SOH = (char) 0x01;
    private static final String SOH_STRING = String.valueOf(SOH);

    private final Map<String, String> fields;

    private FixMessage(Map<String, String> fields) {
        this.fields = fields;
    }

    public static FixMessage parse(String text) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String pair : text.split(SOH_STRING)) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            fields.put(pair.substring(0, eq), pair.substring(eq + 1));
        }
        return new FixMessage(fields);
    }

    public String field(String tag) { return fields.get(tag); }

    public String fieldOr(String tag, String fallback) {
        String v = fields.get(tag);
        return v == null || v.isEmpty() ? fallback : v;
    }

    public Map<String, String> fields() { return fields; }

    public static String buildExecutionReport(FixMessage incoming, String execId, String orderId) {
        String sender = incoming.fieldOr("56", "EXCHANGE");
        String target = incoming.fieldOr("49", "CLIENT");
        String clOrdId = incoming.fieldOr("11", "UNKNOWN");
        String symbol = incoming.fieldOr("55", "UNKNOWN");
        String side = incoming.fieldOr("54", "1");
        String qty = incoming.fieldOr("38", "0");
        String price = incoming.fieldOr("44", "0");
        String now = nowFixUtc();

        StringBuilder body = new StringBuilder();
        body.append("35=8").append(SOH);
        body.append("49=").append(sender).append(SOH);
        body.append("56=").append(target).append(SOH);
        body.append("34=1").append(SOH);
        body.append("52=").append(now).append(SOH);
        body.append("37=").append(orderId).append(SOH);
        body.append("17=").append(execId).append(SOH);
        body.append("150=F").append(SOH); // ExecType = Trade
        body.append("39=2").append(SOH);  // OrdStatus = Filled
        body.append("11=").append(clOrdId).append(SOH);
        body.append("55=").append(symbol).append(SOH);
        body.append("54=").append(side).append(SOH);
        body.append("38=").append(qty).append(SOH);
        body.append("44=").append(price).append(SOH);
        body.append("32=").append(qty).append(SOH);   // LastQty
        body.append("31=").append(price).append(SOH); // LastPx
        body.append("14=").append(qty).append(SOH);   // CumQty
        body.append("6=").append(price).append(SOH);  // AvgPx
        body.append("151=0").append(SOH);             // LeavesQty

        return frame(body.toString());
    }

    public static String buildNewOrderSingle(String sender, String target, String clOrdId, String symbol,
                                             String side, String qty, String price) {
        StringBuilder body = new StringBuilder();
        body.append("35=D").append(SOH);
        body.append("49=").append(sender).append(SOH);
        body.append("56=").append(target).append(SOH);
        body.append("34=1").append(SOH);
        body.append("52=").append(nowFixUtc()).append(SOH);
        body.append("11=").append(clOrdId).append(SOH);
        body.append("21=1").append(SOH);             // HandlInst = automated
        body.append("55=").append(symbol).append(SOH);
        body.append("54=").append(side).append(SOH);
        body.append("60=").append(nowFixUtc()).append(SOH);
        body.append("38=").append(qty).append(SOH);
        body.append("40=2").append(SOH);             // OrdType = Limit
        body.append("44=").append(price).append(SOH);
        body.append("59=0").append(SOH);             // TimeInForce = Day

        return frame(body.toString());
    }

    private static String frame(String body) {
        int bodyLength = body.getBytes(StandardCharsets.US_ASCII).length;
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
