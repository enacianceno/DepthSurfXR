package com.limelight.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;

import org.junit.Test;

public class WebViewKeyMapperTest {
    @Test
    public void digitsMapContiguously() {
        assertEquals(KeyEvent.KEYCODE_0, WebViewKeyMapper.vkToKeyCode(0x30));
        assertEquals(KeyEvent.KEYCODE_9, WebViewKeyMapper.vkToKeyCode(0x39));
    }

    @Test
    public void lettersMapContiguously() {
        assertEquals(KeyEvent.KEYCODE_A, WebViewKeyMapper.vkToKeyCode(0x41));
        assertEquals(KeyEvent.KEYCODE_Z, WebViewKeyMapper.vkToKeyCode(0x5A));
    }

    @Test
    public void controlKeysMap() {
        assertEquals(KeyEvent.KEYCODE_DEL, WebViewKeyMapper.vkToKeyCode(8));
        assertEquals(KeyEvent.KEYCODE_TAB, WebViewKeyMapper.vkToKeyCode(9));
        assertEquals(KeyEvent.KEYCODE_ENTER, WebViewKeyMapper.vkToKeyCode(13));
        assertEquals(KeyEvent.KEYCODE_SPACE, WebViewKeyMapper.vkToKeyCode(32));
        assertEquals(KeyEvent.KEYCODE_ESCAPE, WebViewKeyMapper.vkToKeyCode(0x1B));
    }

    @Test
    public void arrowsAndEditingMap() {
        assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, WebViewKeyMapper.vkToKeyCode(0x25));
        assertEquals(KeyEvent.KEYCODE_DPAD_UP, WebViewKeyMapper.vkToKeyCode(0x26));
        assertEquals(KeyEvent.KEYCODE_DPAD_RIGHT, WebViewKeyMapper.vkToKeyCode(0x27));
        assertEquals(KeyEvent.KEYCODE_DPAD_DOWN, WebViewKeyMapper.vkToKeyCode(0x28));
        assertEquals(KeyEvent.KEYCODE_FORWARD_DEL, WebViewKeyMapper.vkToKeyCode(0x2E));
        assertEquals(KeyEvent.KEYCODE_PAGE_DOWN, WebViewKeyMapper.vkToKeyCode(0x22));
    }

    @Test
    public void functionKeysMap() {
        assertEquals(KeyEvent.KEYCODE_F1, WebViewKeyMapper.vkToKeyCode(0x70));
        assertEquals(KeyEvent.KEYCODE_F12, WebViewKeyMapper.vkToKeyCode(0x7B));
    }

    @Test
    public void punctuationMaps() {
        assertEquals(KeyEvent.KEYCODE_PERIOD, WebViewKeyMapper.vkToKeyCode(0xBE));
        assertEquals(KeyEvent.KEYCODE_SLASH, WebViewKeyMapper.vkToKeyCode(0xBF));
        assertEquals(KeyEvent.KEYCODE_MINUS, WebViewKeyMapper.vkToKeyCode(0xBD));
    }

    @Test
    public void unknownVkIsMinusOne() {
        assertEquals(-1, WebViewKeyMapper.vkToKeyCode(0xFF));
        assertEquals(-1, WebViewKeyMapper.vkToKeyCode(0x00));
    }

    @Test
    public void modifierVksDetected() {
        assertTrue(WebViewKeyMapper.isModifierVk(0x10));
        assertTrue(WebViewKeyMapper.isModifierVk(0xA2));
        assertTrue(WebViewKeyMapper.isModifierVk(0xA4));
        assertTrue(WebViewKeyMapper.isModifierVk(0x5B));
        assertFalse(WebViewKeyMapper.isModifierVk(0x41));
    }

    @Test
    public void metaFlagsMap() {
        assertEquals(KeyEvent.META_SHIFT_ON, WebViewKeyMapper.metaForVk(0x10));
        assertEquals(KeyEvent.META_CTRL_ON, WebViewKeyMapper.metaForVk(0xA2));
        assertEquals(0, WebViewKeyMapper.metaForVk(0x41));
        assertEquals(KeyEvent.META_SHIFT_ON | KeyEvent.META_CTRL_ON,
                WebViewKeyMapper.metaFromModsByte(0x03));
        assertEquals(0, WebViewKeyMapper.metaFromModsByte(0));
    }
}
