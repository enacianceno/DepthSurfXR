package com.limelight.browser;

/**
 * Pure URL-bar logic: turn typed text into a URL to load. No Android
 * dependencies, so plain JUnit covers it.
 */
public final class BrowserNav {
    private BrowserNav() {
    }

    public static final String HOME_URL = "https://www.google.com/";
    private static final String SEARCH_PREFIX =
            "https://www.google.com/search?q=";

    /**
     * Normalize bar text: blank returns null (no-op), text that looks like a
     * host gets an https scheme when missing, anything else becomes a Google
     * search.
     */
    public static String normalizeUrl(String input) {
        if (input == null) {
            return null;
        }
        String text = input.trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.contains(" ")) {
            return SEARCH_PREFIX + text.replace(" ", "+");
        }
        if (text.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            return text;
        }
        if (text.contains(".")) {
            return "https://" + text;
        }
        return SEARCH_PREFIX + text;
    }

    /**
     * True for URLs no WebView can load: a scheme other than the page's
     * own (http/https), the favorites page (fav:), or inert content
     * (about:/data:). App links (intent:, tiktok:, market:) go out to
     * an Android activity instead of dying as an error page.
     */
    public static boolean isExternalScheme(String url) {
        if (url == null) {
            return false;
        }
        int colon = url.indexOf(':');
        if (colon <= 0) {
            return false;
        }
        String scheme = url.substring(0, colon).toLowerCase();
        if (!scheme.matches("^[a-z][a-z0-9+.-]*$")) {
            return false;
        }
        return !scheme.equals("http") && !scheme.equals("https")
                && !scheme.equals("fav") && !scheme.equals("about")
                && !scheme.equals("data");
    }

    /**
     * True for "open/install our app" hijack navigations fired by a page
     * (TikTok silence_awaken: snssdk1233://aweme/..., intent://aweme/...,
     * then a store fallback). Swallowing them keeps the user on the web
     * page; URLs typed in the bar bypass this via loadUrl.
     */
    public static boolean isAppInstallLink(String url, String currentUrl) {
        if (url == null) {
            return false;
        }
        String u = url.toLowerCase();
        if (u.startsWith("snssdk") || u.startsWith("musically")
                || u.startsWith("tiktok://") || u.startsWith("market://")) {
            return true;
        }
        if (u.startsWith("intent://aweme") || u.startsWith("intent://www.tiktok")
                || u.contains("com.zhiliaoapp") || u.contains("com.ss.android.ugc")) {
            return true;
        }
        String host = baseHostOf(url);
        boolean store = "play.google.com".equals(host)
                || "apps.apple.com".equals(host);
        return store && baseHostOf(currentUrl) != null
                && baseHostOf(currentUrl).contains("tiktok");
    }

    /**
     * True when a popup's navigation returns to the opener page's site:
     * same base domain (last two labels), so country/subdomain hops
     * (www vs us) still count. OAuth popups complete this way; only
     * these move to the main view, the rest of the flow stays in the
     * popup with its opener intact.
     */
    public static boolean isReturnToOpener(String popupUrl, String openerUrl) {
        String a = baseHostOf(popupUrl);
        String b = baseHostOf(openerUrl);
        return a != null && a.equals(b);
    }

    private static String baseHostOf(String url) {
        if (url == null) {
            return null;
        }
        String host;
        try {
            host = new java.net.URI(url).getHost();
        } catch (Exception e) {
            return null;
        }
        if (host == null) {
            return null;
        }
        host = host.toLowerCase();
        int dot = host.lastIndexOf('.');
        if (dot <= 0) {
            return host;
        }
        int prev = host.lastIndexOf('.', dot - 1);
        return prev < 0 ? host : host.substring(prev + 1);
    }
}
