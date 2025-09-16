package com.example.learnperformancetest.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP filter thêm traceId vào header phản hồi để client dễ truy vết.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class TraceIdResponseFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_ID_KEY = "traceId";

    private final Tracer tracer;

    public TraceIdResponseFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        // Thử set sớm nếu đã có trace
        try {
            String earlyTraceId = resolveCurrentTraceId();
            if (earlyTraceId != null && !earlyTraceId.isEmpty() && !response.isCommitted()) {
                response.setHeader(TRACE_ID_HEADER, earlyTraceId);
            }
        } catch (Exception ignored) {
            // Không để lỗi filter ảnh hưởng response
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            try {
                String traceId = resolveCurrentTraceId();
                if (traceId != null && !traceId.isEmpty() && !response.isCommitted()) {
                    response.setHeader(TRACE_ID_HEADER, traceId);
                }
            } catch (Exception ignored) {
                // Không để lỗi filter ảnh hưởng response
            }
        }
    }

    private String resolveCurrentTraceId() {
        if (tracer != null) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null && currentSpan.context() != null) {
                String fromTracer = currentSpan.context().traceId();
                if (fromTracer != null && !fromTracer.isEmpty()) {
                    return fromTracer;
                }
            }
        }
        String fromMdc = MDC.get(MDC_TRACE_ID_KEY);
        if (fromMdc != null && !fromMdc.isEmpty()) {
            return fromMdc;
        }
        return null;
    }
}


