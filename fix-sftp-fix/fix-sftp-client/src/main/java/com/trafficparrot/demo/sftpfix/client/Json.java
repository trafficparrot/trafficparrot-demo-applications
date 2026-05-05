package com.trafficparrot.demo.sftpfix.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal JSON helpers — flat object only, no nesting, no arrays as values.
 * Avoids pulling in Jackson for a 30-line need.
 */
final class Json {

    private Json() {}

    static String s(String v) {
        if (v == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    static String obj(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append(s(e.getKey())).append(":");
            Object v = e.getValue();
            if (v == null) sb.append("null");
            else if (v instanceof Number || v instanceof Boolean) sb.append(v);
            else sb.append(s(String.valueOf(v)));
        }
        sb.append("}");
        return sb.toString();
    }

    /** Parses a flat JSON object {"k":"v","n":123,"b":true} into a Map<String,String>. */
    static Map<String, String> parseFlat(String json) {
        Cursor c = new Cursor(json == null ? "" : json);
        Map<String, String> out = new HashMap<>();
        c.skipWs();
        if (!c.eat('{')) return out;
        c.skipWs();
        while (!c.atEnd() && c.peek() != '}') {
            String key = c.readString();
            c.skipWs();
            if (!c.eat(':')) break;
            c.skipWs();
            String value;
            if (c.peek() == '"') {
                value = c.readString();
            } else {
                StringBuilder sb = new StringBuilder();
                while (!c.atEnd() && ",}".indexOf(c.peek()) < 0) sb.append(c.advance());
                value = sb.toString().trim();
            }
            out.put(key, value);
            c.skipWs();
            if (!c.eat(',')) break;
            c.skipWs();
        }
        return out;
    }

    private static final class Cursor {
        private final String s;
        private int i;
        Cursor(String s) { this.s = s; }
        boolean atEnd() { return i >= s.length(); }
        char peek() { return atEnd() ? '\0' : s.charAt(i); }
        char advance() { return s.charAt(i++); }
        boolean eat(char c) {
            if (atEnd() || s.charAt(i) != c) return false;
            i++;
            return true;
        }
        void skipWs() { while (!atEnd() && Character.isWhitespace(s.charAt(i))) i++; }

        String readString() {
            if (!eat('"')) return "";
            StringBuilder out = new StringBuilder();
            while (!atEnd() && peek() != '"') {
                char c = advance();
                if (c == '\\' && !atEnd()) {
                    char esc = advance();
                    switch (esc) {
                        case '"': out.append('"'); break;
                        case '\\': out.append('\\'); break;
                        case '/': out.append('/'); break;
                        case 'n': out.append('\n'); break;
                        case 'r': out.append('\r'); break;
                        case 't': out.append('\t'); break;
                        case 'u':
                            if (i + 4 <= s.length()) {
                                out.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                                i += 4;
                            }
                            break;
                        default: out.append(esc);
                    }
                } else {
                    out.append(c);
                }
            }
            eat('"');
            return out.toString();
        }
    }
}
