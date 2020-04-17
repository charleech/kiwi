package org.kiwiproject.net;

import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang3.StringUtils.isBlank;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Inspired by Guava's {@link com.google.common.net.HostAndPort} but <strong>much</strong> simpler in implementation.
 * (Just go look at the code in {@link com.google.common.net.HostAndPort#fromString(String)} if you don't believe me.)
 * Because it is much simpler, it also only handles a very specific host/port format, which is {@code host:port}.
 * <p>
 * It also does not attempt to validate anything about the host or port, e.g. it will happily accept a negative port
 * value or a one-character long host name.
 */
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SimpleHostAndPort {

    @Getter
    private final String host;

    @Getter
    private final int port;

    /**
     * Parse {@code hostPortString} assuming format {@code host:port}; if it is blank, use the specified
     * {@code defaultHost} and {@code defaultPort} values to create a link {@link SimpleHostAndPort}. No validation is
     * performed on the default values.
     */
    public static SimpleHostAndPort from(String hostPortString, String defaultHost, int defaultPort) {
        if (isBlank(hostPortString)) {
            return new SimpleHostAndPort(defaultHost, defaultPort);
        }

        return from(hostPortString);
    }

    /**
     * Parse {@code hostPortString} assuming format {@code host:port}
     *
     * @throws NullPointerException if {@code hostPortString} is null
     * @throws IllegalStateException if not in the expected format
     * @throws NumberFormatException if port is not a valid number
     */
    public static SimpleHostAndPort from(String hostPortString) {
        var split = hostPortString.split(":");
        checkState(split.length == 2, "%s is not in format host:port", hostPortString);

        return new SimpleHostAndPort(split[0], Integer.parseInt(split[1], 10));
    }

    /**
     * Return a string in {@code host:port} format.
     */
    @Override
    public String toString() {
        return host + ":" + port;
    }
}
