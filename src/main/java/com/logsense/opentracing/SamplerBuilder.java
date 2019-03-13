package com.logsense.opentracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SamplerBuilder {

    public static class NoSampler implements ISampler {
        @Override
        public boolean isSampledOut() {
            return false;
        }
    }

    final Logger logger = LoggerFactory.getLogger(SamplerBuilder.class);

    public ISampler build() {
        try {
            // Is OpenTracing (including GlobalTracer) present?
            Class.forName("io.opentracing.util.GlobalTracer");
            logger.debug("Enabling LogSense sampler");
            return new LogSenseSampler();
        } catch (ClassNotFoundException ex) {
            logger.debug("LogSense tracing libraries not found. Skipping sampler initialization.");
            return new NoSampler();
        }
    }
}
