package com.yahya.uptime_monitor.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

public final class WebsiteUrlSupport {

    private WebsiteUrlSupport() {
    }

    public static String normalize(String value) {
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            if (scheme == null) {
                throw invalidUrl();
            }

            scheme = scheme.toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw invalidUrl();
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw invalidUrl();
            }
            if (uri.getUserInfo() != null) {
                throw invalidUrl();
            }

            int port = uri.getPort();
            if (port > 65_535) {
                throw invalidUrl();
            }
            if (("http".equals(scheme) && port == 80)
                    || ("https".equals(scheme) && port == 443)) {
                port = -1;
            }

            String path = uri.getPath();
            if (path == null || "/".equals(path)) {
                path = "";
            }

            return new URI(
                    scheme,
                    null,
                    host.toLowerCase(Locale.ROOT),
                    port,
                    path,
                    uri.getQuery(),
                    null
            ).toASCIIString();
        } catch (URISyntaxException exception) {
            throw invalidUrl();
        }
    }

    public static void validateMonitoringTarget(
            String value,
            boolean allowPrivateTargets
    ) throws UnknownHostException {
        if (allowPrivateTargets) {
            return;
        }

        URI uri = URI.create(value);
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            if (isPrivateOrLocal(address)) {
                throw new SecurityException(
                        "Target resolves to a private or local network address"
                );
            }
        }
    }

    private static boolean isPrivateOrLocal(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            return first == 0 || (first == 100 && (second & 0xC0) == 64);
        }

        return bytes.length == 16 && (Byte.toUnsignedInt(bytes[0]) & 0xFE) == 0xFC;
    }

    private static IllegalArgumentException invalidUrl() {
        return new IllegalArgumentException(
                "URL must be an absolute HTTP or HTTPS URL with a valid host"
        );
    }
}
