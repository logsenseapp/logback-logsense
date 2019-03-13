package com.logsense.opentracing;

import io.opentracing.SpanContext;

public class LogSenseSampler implements ISampler {
    private LogSenseSpanContext getLogSenseSpanContext() {
        SpanContext spanContext = OpenTraceUtil.currentSpanContext();
        if (spanContext instanceof LogSenseSpanContext) {
            return (LogSenseSpanContext) spanContext;
        } else {
            return null;
        }
    }

    @Override
    public boolean isSampledOut() {
        LogSenseSpanContext spanContext = getLogSenseSpanContext();
        if (spanContext != null) {
            return spanContext.isSampledOut();
        } else {
            return false;
        }
    }
}
