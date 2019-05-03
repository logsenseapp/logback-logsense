package com.logsense.opentracing;

import java.util.logging.Logger;

public class TraceExtractorBuilder {
    final Logger logger = Logger.getLogger(TraceExtractorBuilder.class.getName());

    public ITraceExtractor build() {
        try {
            // Is OpenTracing (including GlobalTracer) present?
            Class.forName("io.opentracing.util.GlobalTracer");
            logger.info("Enabling OpenTracing context extractor to amend logs with trace-id and span-id");
            return new TraceExtractor();
        } catch (ClassNotFoundException ex) {
            logger.fine("OpenTracing context extractor is disabled as no OpenTracing agent is found");
            return null;
        }
    }
}
