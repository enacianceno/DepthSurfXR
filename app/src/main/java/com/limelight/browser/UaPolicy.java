package com.limelight.browser;

/** Per-domain UA choice: Google sign-in rejects desktop/embedded UAs. */
public final class UaPolicy {
    private UaPolicy() {}

    public static final String GOOGLE = "google";
    public static final String DESKTOP = "desktop";

    public static String familyFor(String url) {
        if (url == null) return DESKTOP;
        String host;
        try {
            host = new java.net.URI(url).getHost();
        } catch (Exception e) {
            return DESKTOP;
        }
        if (host == null) return DESKTOP;
        host = host.toLowerCase();
        if (host.endsWith("google.com") || host.endsWith("google.com.mx")
                || host.endsWith("youtube.com") || host.endsWith("ytimg.com")
                || host.endsWith("googleusercontent.com")
                || host.endsWith("gstatic.com")) {
            return GOOGLE;
        }
        return DESKTOP;
    }
}
