package com.logsense.opentracing;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

public class TraceExtractor implements ITraceExtractor {
    @Override
    public String extractSpanId() {
        SpanContext spanContext = OpenTraceUtil.currentSpanContext();
        if (spanContext != null) {
            String spanId = spanContext.toSpanId();
            if (spanId.isEmpty()) {
                return null;
            } else {
                return spanId;
            }
        } else {
            return null;
        }
    }

    @Override
    public String extractTraceId() {
        SpanContext spanContext = OpenTraceUtil.currentSpanContext();
        if (spanContext != null) {
            String traceId = spanContext.toTraceId();
            if (traceId.isEmpty()) {
                return null;
            } else {
                return traceId;
            }
        } else {
            return null;
        }
    }
}
