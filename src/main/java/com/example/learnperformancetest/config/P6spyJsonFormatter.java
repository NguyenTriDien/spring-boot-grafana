package com.example.learnperformancetest.config;

import com.p6spy.engine.common.P6Util;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * P6Spy formatter to output compact JSON line suitable for Logstash ingestion.
 */
public class P6spyJsonFormatter implements MessageFormattingStrategy {

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("executionTime", elapsed);
        payload.put("elapsedTime", elapsed);
        payload.put("category", category);
        payload.put("connectionId", connectionId);
        payload.put("sql", P6Util.singleLine(sql));

        // Optional: trace correlation if present in MDC
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            payload.put("traceId", traceId);
        }
        String spanId = MDC.get("spanId");
        if (spanId != null) {
            payload.put("spanId", spanId);
        }

        return toJson(payload);
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(entry.getKey())).append('"').append(':');
            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value.toString());
            } else {
                sb.append('"').append(escape(String.valueOf(value))).append('"');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}


