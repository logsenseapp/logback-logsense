package com.logsense.opentracing;

import java.util.logging.Logger;

public class SamplerBuilder {

    public static class NoSampler implements ISampler {
        @Override
        public boolean isSampledOut() {
            return false;
        }
    }

    final Logger logger = Logger.getLogger(SamplerBuilder.class.getName());

    public ISampler build() {
        try {
            // Is OpenTracing (including GlobalTracer) present?
            Class.forName("io.opentracing.util.GlobalTracer");
            logger.info("Enabling LogSense sampler");
            return new LogSenseSampler();
        } catch (ClassNotFoundException ex) {
            logger.fine("LogSense tracing libraries not found. Skipping log sampler initialization.");
            return new NoSampler();
        }
    }
}
