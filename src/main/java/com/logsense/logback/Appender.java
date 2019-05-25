package com.logsense.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.more.appenders.FluencyLogbackAppender;
import com.logsense.fluency.LogSenseFluencyBuilder;
import com.logsense.opentracing.ISampler;
import com.logsense.opentracing.ITraceExtractor;
import com.logsense.opentracing.SamplerBuilder;
import com.logsense.opentracing.TraceExtractorBuilder;
import org.komamitsu.fluency.fluentd.FluencyBuilderForFluentd;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.SubstituteLogger;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Simple Appender class that sets up fluentd sink with the
 * defaults used by logsense.com service.
 */
public class Appender<E> extends FluencyLogbackAppender<E> {
    private static final Logger logger = Logger.getLogger(Appender.class.getName());

    private final static String FIELD_CS_CUSTOMER_TOKEN = "cs_customer_token";
    private final static String FIELD_CS_PATTERN_KEY = "cs_pattern_key";
    private final static String FIELD_CS_SOURCE_IP = "cs_src_ip";
    private final static String FIELD_SOURCE_NAME = "source_name";

    private final static String FIELD_TRACE_ID = "ot.trace_id";
    private final static String FIELD_SPAN_ID = "ot.span_id";

    private final static String FIELD_TYPE = "_type";
    private final static String VALUE_TYPE = "java";

    private final static String PROPERTY_LOGSENSE_TOKEN = "logsense.token";
    private final static String PROPERTY_LOGSENSE_CONFIG = "logsense.config";
    private final static String ENV_LOGSENSE_TOKEN = "LOGSENSE_TOKEN";

    private final static String[] FIELDS_SKIPPED = new String[] {
            "msg" // essentially a duplicate of message
    };

    // Guards for a case when no or invalid token is set
    private boolean enabled = false;
    private boolean sendLocalIpAddress;
    private boolean silenceFluencyWarnings = true;

    private ITraceExtractor traceExtractor;
    private ISampler sampler;

    /**
     * Utility class that does it best to figure out what is the machine IP address.
     * It prefers non-local addresses over the local ones (or loopback)
     */
    public static class AddressDeterminer {
        private List<InetAddress> preferredAddresses = new ArrayList<>();

        public AddressDeterminer() {
            try {
                Enumeration e = NetworkInterface.getNetworkInterfaces();
                while(e.hasMoreElements())
                {
                    NetworkInterface n = (NetworkInterface) e.nextElement();
                    preferredAddresses.addAll(Collections.list(n.getInetAddresses()));
                }
                sortAddresses();
            } catch (SocketException e) {
                // Silently ignore :-o
            }
        }

        @Override
        public String toString() {
            StringBuffer buf = new StringBuffer();

            for (int i = 0; i < preferredAddresses.size(); i++) {
                buf.append(i);
                buf.append(": ");
                buf.append(preferredAddresses.get(i).getHostAddress());
                buf.append('\n');
            }

            return buf.toString();
        }

        public InetAddress getPreferredAddress() {

            if (preferredAddresses.isEmpty()) {
                return null;
            } else {
                return preferredAddresses.get(0);
            }
        }

        private void sortAddresses() {
            Collections.sort(preferredAddresses, new Comparator<InetAddress>() {
                @Override
                public int compare(InetAddress a1, InetAddress a2) {
                    // Don't like loopbacks
                    if (a1.isLoopbackAddress() && !a2.isLoopbackAddress()) {
                        return 1;
                    } else if (a2.isLoopbackAddress() && !a1.isLoopbackAddress()) {
                        return -1;
                    }

                    // Prefer anything else above site local addresses
                    if (a1.isSiteLocalAddress() && !a2.isSiteLocalAddress()) {
                        return -1;
                    } else if (!a1.isSiteLocalAddress() && a2.isSiteLocalAddress()) {
                        return 1;
                    }

                    // Prefer IPv4 over IPv6
                    if (a1 instanceof Inet6Address && a2 instanceof Inet4Address) {
                        return 1;
                    } else if (a1 instanceof Inet4Address && a2 instanceof Inet6Address){
                        return -1;
                    }

                    byte[] b1 = a1.getAddress();
                    byte[] b2 = a2.getAddress();

                    // Since this is the same address type, we expect the byte arrays to be of equal size
                    for (int i = 0; i < b1.length; i++) {
                        int res = Byte.compare(b1[i], b2[i]);
                        if (res != 0) {
                            return res;
                        }
                    }

                    return 0;
                }
            });
        }
    }

    public Appender() {
        // Just set the defaults that are always the same for LogSense output
        setTag("structured");

        setPort(32714);
        setRemoteHost("logs.logsense.com");

        setAckResponseMode(true);
        setBufferChunkInitialSize(2097152);
        setBufferChunkRetentionSize(16777216);
        setMaxBufferSize(268435456L);
        setWaitUntilBufferFlushed(30);
        setWaitUntilFlusherTerminated(40);
        setFlushIntervalMillis(200);
        setSenderMaxRetryCount(12);
        setUseEventTime(true);
        setSslEnabled(true);

        if (additionalFields == null) {
            additionalFields = new HashMap<String, String>();
        }

        setPropertiesFromEnv();
        setPatternKey("message");
        this.additionalFields.put(FIELD_TYPE, VALUE_TYPE);

        for (String ignoredField : FIELDS_SKIPPED) {
            this.additionalFields.put(ignoredField, null);
        }

        this.traceExtractor = new TraceExtractorBuilder().build();
        this.sampler = new SamplerBuilder().build();
    }

    protected FluencyBuilderForFluentd configureFluency() {
        if (this.silenceFluencyWarnings) {
            LogSenseFluencyBuilder builder = new LogSenseFluencyBuilder();
            builder.setAckResponseMode(isAckResponseMode());
            if (getFileBackupDir() != null) { builder.setFileBackupDir(getFileBackupDir()); }
            if (getBufferChunkInitialSize() != null) { builder.setBufferChunkInitialSize(getBufferChunkInitialSize()); }
            if (getBufferChunkRetentionSize() != null) { builder.setBufferChunkRetentionSize(getBufferChunkRetentionSize()); }
            if (getMaxBufferSize() != null) { builder.setMaxBufferSize(getMaxBufferSize()); }
            if (getWaitUntilBufferFlushed() != null) { builder.setWaitUntilBufferFlushed(getWaitUntilBufferFlushed()); }
            if (getWaitUntilFlusherTerminated() != null) { builder.setWaitUntilFlusherTerminated(getWaitUntilFlusherTerminated()); }
            if (getFlushIntervalMillis() != null) { builder.setFlushIntervalMillis(getFlushIntervalMillis()); }
            if (getSenderMaxRetryCount() != null) { builder.setSenderMaxRetryCount(getSenderMaxRetryCount()); }

            return builder;
        } else {
            return super.configureFluency();
        }
    }

    @Override
    public void start() {
        // Start nevertheless, the token could be provided in runtime later
        super.start();

        if (enabled == false) {
            logger.severe("LogSense appender has no LOGSENSE_TOKEN set. Sending logs will be skipped unless the token is provided");
        } else {
            logger.fine("Starting LogSense appender");
        }
    }

    @Override
    protected void append(E event) {
        if (enabled == false) {
            return;
        }

        if (this.sampler.isSampledOut()) {
            return;
        }

        setSpanContext();
        super.append(event);
        cleanSpanContext();
    }

    private void setSpanContext() {
        if (this.traceExtractor == null) {
            return;
        }

        String traceId = this.traceExtractor.extractTraceId();
        String spanId = this.traceExtractor.extractSpanId();

        if (traceId != null) {
            this.additionalFields.put(FIELD_TRACE_ID, traceId);
        }

        if (spanId != null) {
            this.additionalFields.put(FIELD_SPAN_ID, spanId);
        }
    }

    private void cleanSpanContext() {
        if (this.traceExtractor == null) {
            return;
        }

        this.additionalFields.remove(FIELD_TRACE_ID);
        this.additionalFields.remove(FIELD_SPAN_ID);
    }

    private void setPropertiesFromEnv() {
        // Step 1 - try to fetch it from property
        String token_maybe = System.getProperties().getProperty(PROPERTY_LOGSENSE_TOKEN);

        // Step 2 - not present? use env variable
        if (!isValidLogsenseToken(token_maybe)) {
            token_maybe = System.getenv(ENV_LOGSENSE_TOKEN);
        }

        // Step 3 - not present? maybe config file was specified?
        if (!isValidLogsenseToken(token_maybe)) {
            String config_file = System.getProperties().getProperty(PROPERTY_LOGSENSE_CONFIG);
            if (config_file != null && !config_file.isEmpty()) {
                Properties prop = attemptLoadingPropertyFile(config_file);
                try {
                    token_maybe = prop.getProperty(PROPERTY_LOGSENSE_TOKEN);
                } catch (NullPointerException npe) {
                    // Just skip it silently
                }
            }
        }

        if (isValidLogsenseToken(token_maybe)) {
            setLogsenseToken(token_maybe);
        }
    }

    /**
     * @param useLocalIpAddress if set to true, figures out what is the local IP address and sends it
     *                          with the logs
     */
    public void setUseLocalIpAddress(boolean useLocalIpAddress) {
        this.sendLocalIpAddress = useLocalIpAddress;

        // This is a simple implementation - just try to determine the source IP address once and keep using it
        if (this.sendLocalIpAddress) {
            InetAddress addr = new AddressDeterminer().getPreferredAddress();
            if (addr != null) {
                setSourceIp(addr.getHostAddress());
                logger.info("Using " + addr.getHostAddress() + " as the source IP address");
            }
        }
    }

    public boolean isUseLocalIpAddres() {
        return sendLocalIpAddress;
    }

    private boolean isValidLogsenseToken(String potentialLogsenseToken) {
        if (potentialLogsenseToken == null || potentialLogsenseToken.trim().isEmpty() || potentialLogsenseToken.trim().equals("ENTER_LOGSENSE_TOKEN")) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @param logsenseToken the LOGSENSE_TOKEN which identifies each client when sending data to logsense.com
     */
    public void setLogsenseToken(String logsenseToken) {
        if (isValidLogsenseToken(logsenseToken)) {
            this.additionalFields.put(FIELD_CS_CUSTOMER_TOKEN, logsenseToken);
        }

        if (isValidLogsenseToken(getLogsenseToken())) {
            this.enabled = true;
        } else {
            this.enabled = false;
        }
    }

    public String getLogsenseToken() {
        return this.additionalFields.get(FIELD_CS_CUSTOMER_TOKEN);
    }

    /**
     * @param patternKey name of the key which is a subject of automated pattern recognition. By default set to
     *                  `message`
     */
    public void setPatternKey(String patternKey) {
        this.additionalFields.put(FIELD_CS_PATTERN_KEY, patternKey);
    }

    public String getPatternKey() {
        return this.additionalFields.get(FIELD_CS_PATTERN_KEY);
    }

    /**
     * @param sourceIp if set, overwrites the source IP with the string. Please use only valid IP addresses
     */
    public void setSourceIp(String sourceIp) {
        this.additionalFields.put(FIELD_CS_SOURCE_IP, sourceIp);
    }

    public String getSourceIp() {
        return this.additionalFields.get(FIELD_CS_SOURCE_IP);
    }

    /**
     * @param csCustomerToken the CUSTOMER_TOKEN which identifies each client when sending data to logsense.com
     * @deprecated use {@link #setLogsenseToken(String)}
     */
    public void setCsCustomerToken(String csCustomerToken) {
        this.setLogsenseToken(csCustomerToken);
    }

    /**
     * @return the set token
     * @deprecated use {@link #getLogsenseToken()}
     */
    public String getCsCustomerToken() {
        return this.getLogsenseToken();
    }

    /**
     * @param csPatternKey name of the key which is a subject of automated pattern recognition. By default set to
     *                     `message`
     * @deprecated use {@link #setPatternKey(String)}
     */
    public void setCsPatternKey(String csPatternKey) {
        this.setPatternKey(csPatternKey);
    }

    public String getCsPatternKey() {
        return this.getPatternKey();
    }

    /**
     * @param csSourceIp if set, overwrites the source IP with the string. Please use only valid IP addresses
     * @deprecated use {@link #setSourceIp(String)}
     */
    public void setCsSourceIp(String csSourceIp) {
        this.setSourceIp(csSourceIp);
    }

    public String getCsSourceIp() {
        return this.getSourceIp();
    }

    /**
     * @param csSourceName sets the `source_name` property. Useful when identifying where the log came from.
     */
    public void setSourceName(String csSourceName) {
        this.additionalFields.put(FIELD_SOURCE_NAME, csSourceName);
    }

    public String getSourceName() {
        return this.additionalFields.get(FIELD_SOURCE_NAME);
    }

    private Properties attemptLoadingPropertyFile(String path) {
        Properties prop = new Properties();
        InputStream fis=null;
        try {
            fis = new FileInputStream(path);
            prop.load(fis);
        } catch(Exception e) {
            logger.severe(String.format("Skipping loading LogSense properties from %s due to exception: %s", path, e.getMessage(), e));
        } finally {
            try {
                fis.close();;
            } catch (Exception e) {
                return null;
            }
        }

        return prop;
    }

    /**
     * @param silenceFluencyWarnings - if set to true (default), Fluency WARNING logs are being silenced.
     */
    public void setSilenceFluencyWarnings(boolean silenceFluencyWarnings) {
        this.silenceFluencyWarnings = silenceFluencyWarnings;
    }

    public boolean isSilenceFluencyWarnings() {
        return silenceFluencyWarnings;
    }
}

