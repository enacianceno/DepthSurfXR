package com.limelight.browser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WebViewInputMapperTest {
    @Test
    public void clamp01KeepsInside() {
        assertEquals(0.5f, WebViewInputMapper.clamp01(0.5f), 0.0f);
    }

    @Test
    public void clamp01ClampsLow() {
        assertEquals(0.0f, WebViewInputMapper.clamp01(-2.0f), 0.0f);
    }

    @Test
    public void clamp01ClampsHigh() {
        assertEquals(1.0f, WebViewInputMapper.clamp01(9.0f), 0.0f);
    }

    @Test
    public void toPixelsScalesAndClamps() {
        assertEquals(640.0f, WebViewInputMapper.toPixels(0.5f, 1280), 0.0f);
        assertEquals(0.0f, WebViewInputMapper.toPixels(-1.0f, 1280), 0.0f);
        assertEquals(720.0f, WebViewInputMapper.toPixels(2.0f, 720), 0.0f);
    }

    @Test
    public void touchCarriesKindAndPosition() {
        WebViewInputMapper.TouchCmd cmd = WebViewInputMapper.touch(
                WebViewInputMapper.TouchKind.DOWN, 0.25f, 0.5f, 1280, 720);
        assertEquals(WebViewInputMapper.TouchKind.DOWN, cmd.kind);
        assertEquals(320.0f, cmd.x, 0.0f);
        assertEquals(360.0f, cmd.y, 0.0f);
    }

    @Test
    public void scrollPixelsScalesClicks() {
        assertEquals(160, WebViewInputMapper.scrollPixels(1));
        assertEquals(-320, WebViewInputMapper.scrollPixels(-2));
        assertEquals(0, WebViewInputMapper.scrollPixels(0));
    }
}
