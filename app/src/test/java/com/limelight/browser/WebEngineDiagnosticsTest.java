package com.limelight.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

public class WebEngineDiagnosticsTest {
    @Test public void http200AloneIsLoadedNotJsFailure() {
        assertEquals(WebEngineDiagnostics.Outcome.LOADED_NO_ERROR,
                WebEngineDiagnostics.classify(200, null, false));
    }
    @Test public void http200WithJsEvidenceIsJs() {
        assertEquals(WebEngineDiagnostics.Outcome.JS_ERROR_EVIDENCE,
                WebEngineDiagnostics.classify(200, null, true));
    }
    @Test public void http403IsPossibleBlock() {
        assertEquals(WebEngineDiagnostics.Outcome.HTTP_BLOCK_POSSIBLE,
                WebEngineDiagnostics.classify(403, null, false));
    }
    @Test public void networkErrorWins() {
        assertEquals(WebEngineDiagnostics.Outcome.NETWORK_ERROR,
                WebEngineDiagnostics.classify(null, -8, false));
    }
    @Test public void sanitizeDropsQueryAndPath() {
        String s = WebEngineDiagnostics.sanitizeUrl(
                "https://www.tiktok.com/login?token=SECRET#frag");
        assertEquals("https://www.tiktok.com", s);
        assertFalse(s.contains("SECRET"));
    }
    @Test public void logLineSeparatesScopes() {
        String main = WebEngineDiagnostics.logLine(
                WebEngineDiagnostics.Scope.MAIN_FRAME,
                "https://www.tiktok.com/?a=1", 403, null, "Forbidden", false);
        String sub = WebEngineDiagnostics.logLine(
                WebEngineDiagnostics.Scope.SUBRESOURCE,
                "https://cdn.example/x.js?sig=1", null, -8, "timeout", false);
        assertFalse(main.equals(sub));
        assertFalse(main.contains("a=1"));
    }
}
