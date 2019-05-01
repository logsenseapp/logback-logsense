package com.logsense.opentracing;

public interface ITraceExtractor {
    /**
     * @return current trace-id (if present), null - otherwise
     */
    public String extractTraceId();
    /**
     * @return current span-id (if present), null - otherwise
     */
    public String extractSpanId();
}
