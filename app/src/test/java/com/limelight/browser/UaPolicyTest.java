package com.limelight.browser;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class UaPolicyTest {
    @Test public void googleFamilyGetsStockUa() {
        assertEquals(UaPolicy.GOOGLE, UaPolicy.familyFor("https://www.google.com/"));
        assertEquals(UaPolicy.GOOGLE, UaPolicy.familyFor("https://accounts.google.com/v3/signin"));
        assertEquals(UaPolicy.GOOGLE, UaPolicy.familyFor("https://www.youtube.com/watch"));
    }
    @Test public void othersGetDesktopUa() {
        assertEquals(UaPolicy.DESKTOP, UaPolicy.familyFor("https://www.tiktok.com/"));
        assertEquals(UaPolicy.DESKTOP, UaPolicy.familyFor("https://www.wikipedia.org/"));
        assertEquals(UaPolicy.DESKTOP, UaPolicy.familyFor(null));
        assertEquals(UaPolicy.DESKTOP, UaPolicy.familyFor("https://google.com.evil.com/"));
    }
}
