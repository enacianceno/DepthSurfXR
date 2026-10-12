package com.limelight.browser;

/**
 * Pure User-Agent shaping. The stock WebView UA carries a "; wv" token
 * that flags the client as an embedded view; several large sites
 * (TikTok among them) restrict or degrade those. Stripping the token
 * presents the page's own Chrome renderer as plain Chrome.
 */
public final class BrowserUa {
    private BrowserUa() {
    }

    /** Desktop Chrome UA reusing the stock Chrome version, when present. */
    public static String desktopChrome(String userAgent) {
        String version = "126.0.0.0";
        if (userAgent != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("Chrome/(\\d+\\.\\d+\\.\\d+\\.\\d+)").matcher(userAgent);
            if (m.find()) {
                version = m.group(1);
            }
        }
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/"
                + version + " Safari/537.36";
    }

    public static String stripWebViewToken(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.replace("; wv", "");
    }
}
