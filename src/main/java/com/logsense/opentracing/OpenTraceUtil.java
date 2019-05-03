package com.logsense.opentracing;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

public class OpenTraceUtil {
    public static Tracer currentTracer() {
        return GlobalTracer.get();
    }

    public static Span currentSpan() {
        return currentTracer().activeSpan();
    }

    public static SpanContext currentSpanContext() {
        Span span = currentSpan();
        if (span != null) {
            return span.context();
        } else {
            return null;
        }
    }
}
