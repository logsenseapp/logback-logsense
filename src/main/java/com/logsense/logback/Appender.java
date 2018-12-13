package com.logsense.logback;

import ch.qos.logback.more.appenders.FluencyLogbackAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.*;

/**
 * Simple Appender class that sets up fluentd sink with the
 * defaults used by logsense.com service.
 */
public class Appender<E> extends FluencyLogbackAppender<E> {
    final Logger logger = LoggerFactory.getLogger(Appender.class);

    private final static String FIELD_CS_CUSTOMER_TOKEN = "cs_customer_token";
    private final static String FIELD_CS_PATTERN_KEY = "cs_pattern_key";
    private final static String FIELD_CS_SOURCE_IP = "cs_src_ip";
    private final static String FIELD_SOURCE_NAME = "source_name";

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

        setCsPatternKey("message");
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
                setCsSourceIp(addr.getHostAddress());
                logger.info("Using {} as the source IP address", addr.getHostAddress());
            }
        }
    }

    public boolean isUseLocalIpAddres() {
        return sendLocalIpAddress;
    }

    /**
     * @param csCustomerToken the CUSTOMER_TOKEN which identifies each client when sending data to logsense.com
     */
    public void setCsCustomerToken(String csCustomerToken) {
        this.additionalFields.put(FIELD_CS_CUSTOMER_TOKEN, csCustomerToken);
    }

    public String getCsCustomerToken() {
        return this.additionalFields.get(FIELD_CS_CUSTOMER_TOKEN);
    }

    /**
     * @param csPatternKey name of the key which is a subject of automated pattern recognition. By default set to
     *                     `message`
     */
    public void setCsPatternKey(String csPatternKey) {
        this.additionalFields.put(FIELD_CS_PATTERN_KEY, csPatternKey);
    }

    public String getCsPatternKey() {
        return this.additionalFields.get(FIELD_CS_PATTERN_KEY);
    }

    /**
     * @param csSourceIp if set, overwrites the source IP with the string. Please use only valid IP addresses
     */
    public void setCsSourceIp(String csSourceIp) {
        this.additionalFields.put(FIELD_CS_SOURCE_IP, csSourceIp);
    }

    public String getCsSourceIp() {
        return this.additionalFields.get(FIELD_CS_SOURCE_IP);
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

    private boolean sendLocalIpAddress;
}

