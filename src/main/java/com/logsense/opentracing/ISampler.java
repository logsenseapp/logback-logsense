package com.logsense.opentracing;

public interface ISampler {
    /**
     * @return true if logs should be sampled out (skipped)
     */
    boolean isSampledOut();
}
