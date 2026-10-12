package com.limelight.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BrowserNavTest {
    @Test
    public void blankIsNoOp() {
        assertNull(BrowserNav.normalizeUrl(null));
        assertNull(BrowserNav.normalizeUrl(""));
        assertNull(BrowserNav.normalizeUrl("   "));
    }

    @Test
    public void bareHostGetsHttps() {
        assertEquals("https://example.com",
                BrowserNav.normalizeUrl("example.com"));
        assertEquals("https://www.google.com/search?q=example.com/a+b",
                BrowserNav.normalizeUrl("example.com/a b"));
    }

    @Test
    public void schemeIsKept() {
        assertEquals("http://example.com/x",
                BrowserNav.normalizeUrl("http://example.com/x"));
        assertEquals("https://example.com/",
                BrowserNav.normalizeUrl("https://example.com/"));
    }

    @Test
    public void freeTextBecomesSearch() {
        assertEquals("https://www.google.com/search?q=quest+3s",
                BrowserNav.normalizeUrl("quest 3s"));
        assertEquals("https://www.google.com/search?q=xr",
                BrowserNav.normalizeUrl("xr"));
    }

    @Test
    public void appLinksAreExternal() {
        assertTrue(BrowserNav.isExternalScheme("intent://x#Intent;scheme=tiktok;end"));
        assertTrue(BrowserNav.isExternalScheme("tiktok://video/123"));
        assertTrue(BrowserNav.isExternalScheme("market://details?id=x"));
        assertTrue(BrowserNav.isExternalScheme("INTENT://x"));
    }

    @Test
    public void popupReturnToOpenerMatchesSite() {
        assertTrue(BrowserNav.isReturnToOpener(
                "https://www.tiktok.com/login/callback", "https://www.tiktok.com/login"));
        assertTrue(BrowserNav.isReturnToOpener(
                "https://us.tiktok.com/callback", "https://www.tiktok.com/login"));
        assertTrue(BrowserNav.isReturnToOpener(
                "https://www.tiktok.com/x", "https://m.tiktok.com/y"));
    }

    @Test
    public void popupOAuthStepsStayOutside() {
        assertFalse(BrowserNav.isReturnToOpener(
                "https://accounts.google.com/o/oauth2/auth", "https://www.tiktok.com/login"));
        assertFalse(BrowserNav.isReturnToOpener(
                "https://www.tiktok.com/login", "https://accounts.google.com/x"));
        assertFalse(BrowserNav.isReturnToOpener(null, "https://www.tiktok.com/"));
        assertFalse(BrowserNav.isReturnToOpener("https://www.tiktok.com/", null));
        assertFalse(BrowserNav.isReturnToOpener("/relative/path", "https://www.tiktok.com/"));
        assertFalse(BrowserNav.isReturnToOpener("https://tiktok.evil.com/", "https://www.tiktok.com/"));
    }

    @Test
    public void tiktokAwakenLinksAreBlocked() {
        String page = "https://www.tiktok.com/foryou";
        org.junit.Assert.assertTrue(BrowserNav.isAppInstallLink(
                "snssdk1233://aweme/detail/123?gd_label=click_wap_silence_awaken", page));
        org.junit.Assert.assertTrue(BrowserNav.isAppInstallLink(
                "snssdk1340://user/profile/1", page));
        org.junit.Assert.assertTrue(BrowserNav.isAppInstallLink(
                "intent://aweme/detail/1#Intent;scheme=snssdk1233;package=com.ss.android.ugc.aweme;end", page));
        org.junit.Assert.assertTrue(BrowserNav.isAppInstallLink(
                "https://play.google.com/store/apps/details?id=com.zhiliaoapp.musically", page));
        org.junit.Assert.assertFalse(BrowserNav.isAppInstallLink(
                "https://www.tiktok.com/@user/video/1", page));
        org.junit.Assert.assertFalse(BrowserNav.isAppInstallLink(
                "https://play.google.com/store/apps/details?id=x",
                "https://www.google.com/"));
        org.junit.Assert.assertFalse(BrowserNav.isAppInstallLink(null, page));
    }

    @Test
    public void pageSchemesStayInside() {
        assertFalse(BrowserNav.isExternalScheme("https://www.tiktok.com/"));
        assertFalse(BrowserNav.isExternalScheme("http://example.com/"));
        assertFalse(BrowserNav.isExternalScheme("fav:page"));
        assertFalse(BrowserNav.isExternalScheme("about:blank"));
        assertFalse(BrowserNav.isExternalScheme("data:text/html,x"));
        assertFalse(BrowserNav.isExternalScheme(null));
        assertFalse(BrowserNav.isExternalScheme("tiktok.com"));
        assertFalse(BrowserNav.isExternalScheme("nota scheme:x"));
    }
}
