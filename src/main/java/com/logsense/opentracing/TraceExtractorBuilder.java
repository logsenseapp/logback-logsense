package com.logsense.opentracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceExtractorBuilder {
    final Logger logger = LoggerFactory.getLogger(TraceExtractorBuilder.class);

    public ITraceExtractor build() {
        try {
            // Is OpenTracing (including GlobalTracer) present?
            Class.forName("io.opentracing.util.GlobalTracer");
            logger.info("Enabling OpenTracing context extractor to amend logs with trace-id and span-id");
            return new TraceExtractor();
        } catch (ClassNotFoundException ex) {
            logger.info("OpenTracing context extractor is disabled as no OpenTracing agent is found");
            return null;
        }
    }
}
