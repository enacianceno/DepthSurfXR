package com.limelight.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class BrowserUaTest {
    private static final String STOCK =
            "Mozilla/5.0 (Linux; Android 14; Quest 3S) AppleWebKit/537.36"
            + " (KHTML, like Gecko) Version/4.0 Chrome/120.0 Mobile Safari/537.36";

    @Test
    public void webViewTokenIsStripped() {
        assertEquals(STOCK, BrowserUa.stripWebViewToken(
                STOCK.replace("Mobile", "Mobile; wv")));
    }

    @Test
    public void plainChromeIsUnchanged() {
        assertEquals(STOCK, BrowserUa.stripWebViewToken(STOCK));
    }

    @Test
    public void desktopUaReusesChromeVersion() {
        assertEquals("Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/120.0.0.0 Safari/537.36",
                BrowserUa.desktopChrome(STOCK.replace(
                        "Chrome/120.0", "Chrome/120.0.0.0")));
    }

    @Test
    public void nullIsNoOp() {
        assertNull(BrowserUa.stripWebViewToken(null));
    }
}
