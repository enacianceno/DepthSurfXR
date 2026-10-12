package com.limelight.browser;

import android.view.KeyEvent;

/**
 * Windows virtual-key to Android mapping for a {@code VrKeyboard.Sink}.
 * Constants only (inlined at compile time), so plain JUnit covers it.
 */
public final class WebViewKeyMapper {
    private WebViewKeyMapper() {
    }

    public static final int VK_SHIFT = 0x10;
    public static final int VK_CONTROL = 0xA2;
    public static final int VK_MENU = 0xA4;
    public static final int VK_LWIN = 0x5B;

    public static boolean isModifierVk(int vk) {
        return vk == VK_SHIFT || vk == VK_CONTROL || vk == VK_MENU || vk == VK_LWIN;
    }

    /** Meta flag a held modifier VK contributes. 0 for the rest. */
    public static int metaForVk(int vk) {
        if (vk == VK_SHIFT) {
            return KeyEvent.META_SHIFT_ON;
        }
        if (vk == VK_CONTROL) {
            return KeyEvent.META_CTRL_ON;
        }
        if (vk == VK_MENU) {
            return KeyEvent.META_ALT_ON;
        }
        if (vk == VK_LWIN) {
            return KeyEvent.META_META_ON;
        }
        return 0;
    }

    /** Modifier byte (KeyboardPacket.MODIFIER_*) to Android meta flags. */
    public static int metaFromModsByte(int mods) {
        int meta = 0;
        if ((mods & 0x01) != 0) {
            meta |= KeyEvent.META_SHIFT_ON;
        }
        if ((mods & 0x02) != 0) {
            meta |= KeyEvent.META_CTRL_ON;
        }
        if ((mods & 0x04) != 0) {
            meta |= KeyEvent.META_ALT_ON;
        }
        if ((mods & 0x08) != 0) {
            meta |= KeyEvent.META_META_ON;
        }
        return meta;
    }

    /** Windows virtual key to Android key code, or -1 when unmapped. */
    public static int vkToKeyCode(int vk) {
        if (vk >= 0x30 && vk <= 0x39) {
            return KeyEvent.KEYCODE_0 + (vk - 0x30);
        }
        if (vk >= 0x41 && vk <= 0x5A) {
            return KeyEvent.KEYCODE_A + (vk - 0x41);
        }
        if (vk >= 0x70 && vk <= 0x7B) {
            return KeyEvent.KEYCODE_F1 + (vk - 0x70);
        }
        switch (vk) {
            case 8:
                return KeyEvent.KEYCODE_DEL;
            case 9:
                return KeyEvent.KEYCODE_TAB;
            case 13:
                return KeyEvent.KEYCODE_ENTER;
            case 32:
                return KeyEvent.KEYCODE_SPACE;
            case 0x1B:
                return KeyEvent.KEYCODE_ESCAPE;
            case 0x21:
                return KeyEvent.KEYCODE_PAGE_UP;
            case 0x22:
                return KeyEvent.KEYCODE_PAGE_DOWN;
            case 0x23:
                return KeyEvent.KEYCODE_MOVE_END;
            case 0x24:
                return KeyEvent.KEYCODE_MOVE_HOME;
            case 0x25:
                return KeyEvent.KEYCODE_DPAD_LEFT;
            case 0x26:
                return KeyEvent.KEYCODE_DPAD_UP;
            case 0x27:
                return KeyEvent.KEYCODE_DPAD_RIGHT;
            case 0x28:
                return KeyEvent.KEYCODE_DPAD_DOWN;
            case 0x2D:
                return KeyEvent.KEYCODE_INSERT;
            case 0x2E:
                return KeyEvent.KEYCODE_FORWARD_DEL;
            case 0xBA:
                return KeyEvent.KEYCODE_SEMICOLON;
            case 0xBB:
                return KeyEvent.KEYCODE_EQUALS;
            case 0xBC:
                return KeyEvent.KEYCODE_COMMA;
            case 0xBD:
                return KeyEvent.KEYCODE_MINUS;
            case 0xBE:
                return KeyEvent.KEYCODE_PERIOD;
            case 0xBF:
                return KeyEvent.KEYCODE_SLASH;
            case 0xC0:
                return KeyEvent.KEYCODE_GRAVE;
            case 0xDB:
                return KeyEvent.KEYCODE_LEFT_BRACKET;
            case 0xDC:
                return KeyEvent.KEYCODE_BACKSLASH;
            case 0xDD:
                return KeyEvent.KEYCODE_RIGHT_BRACKET;
            case 0xDE:
                return KeyEvent.KEYCODE_APOSTROPHE;
            default:
                return -1;
        }
    }
}
