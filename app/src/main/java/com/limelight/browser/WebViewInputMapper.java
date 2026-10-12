package com.limelight.browser;

/**
 * Pure mapping from Moonlight XR pointer callbacks to WebView touch geometry.
 * No Android dependencies, so plain JUnit covers it. The activity turns
 * TouchCmd into MotionEvents on the UI thread.
 */
public final class WebViewInputMapper {
    private WebViewInputMapper() {
    }

    public enum TouchKind {
        DOWN, MOVE, UP
    }

    public static final class TouchCmd {
        public final TouchKind kind;
        public final float x;
        public final float y;

        public TouchCmd(TouchKind kind, float x, float y) {
            this.kind = kind;
            this.x = x;
            this.y = y;
        }
    }

    /** Pixels scrolled per onVrScroll click. */
    public static final int SCROLL_PX_PER_CLICK = 160;

    public static float clamp01(float v) {
        if (v < 0.0f) {
            return 0.0f;
        }
        if (v > 1.0f) {
            return 1.0f;
        }
        return v;
    }

    public static float toPixels(float uv, int sizePx) {
        return clamp01(uv) * (float) sizePx;
    }

    public static TouchCmd touch(TouchKind kind, float u, float v, int wPx, int hPx) {
        return new TouchCmd(kind, toPixels(u, wPx), toPixels(v, hPx));
    }

    public static int scrollPixels(int clicks) {
        return clicks * SCROLL_PX_PER_CLICK;
    }
}
