package com.limelight.browser;

/** Pure helpers for the WebView diagnostic experiment. No Android deps. */
public final class WebEngineDiagnostics {
    private WebEngineDiagnostics() {}

    public enum Scope { MAIN_FRAME, SUBRESOURCE }

    /** A 2xx/3xx response is success, never a JS failure by itself. */
    public enum Outcome {
        HTTP_OK, HTTP_BLOCK_POSSIBLE, HTTP_CLIENT_ERROR, HTTP_SERVER_ERROR,
        NETWORK_ERROR, JS_ERROR_EVIDENCE, LOADED_NO_ERROR, UNKNOWN
    }

    public static Outcome classify(Integer httpStatus, Integer errorCode,
                                   boolean jsErrorEvidence) {
        if (errorCode != null && errorCode != 0) return Outcome.NETWORK_ERROR;
        if (httpStatus != null) {
            if (httpStatus == 403 || httpStatus == 429 || httpStatus == 451) {
                return Outcome.HTTP_BLOCK_POSSIBLE;
            }
            if (httpStatus >= 400 && httpStatus < 500) return Outcome.HTTP_CLIENT_ERROR;
            if (httpStatus >= 500) return Outcome.HTTP_SERVER_ERROR;
            if (httpStatus >= 200 && httpStatus < 400) {
                return jsErrorEvidence ? Outcome.JS_ERROR_EVIDENCE
                                       : Outcome.LOADED_NO_ERROR;
            }
        }
        return jsErrorEvidence ? Outcome.JS_ERROR_EVIDENCE : Outcome.UNKNOWN;
    }

    /** Scheme+host only: drops path, query and fragment (may carry tokens). */
    public static String sanitizeUrl(String url) {
        if (url == null) return "null";
        try {
            java.net.URI u = new java.net.URI(url);
            if (u.getScheme() == null || u.getHost() == null) return "non-http";
            return u.getScheme() + "://" + u.getHost();
        } catch (Exception e) {
            return "unparseable";
        }
    }

    public static String desktopUa(String ua) {
        if (ua == null) return null;
        return ua.replace(" Mobile ", " ").replace("Mobile Safari", "Safari");
    }

    public static String logLine(Scope scope, String url, Integer http,
                                 Integer err, String desc, boolean jsError) {
        return "BrowserDiag scope=" + scope + " url=" + sanitizeUrl(url)
                + " http=" + http + " err=" + err
                + " outcome=" + classify(http, err, jsError)
                + " desc=" + (desc == null ? "" : desc.replaceAll("[\\r\\n]+", " "));
    }
}
