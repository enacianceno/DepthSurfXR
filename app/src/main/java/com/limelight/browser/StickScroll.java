package com.limelight.browser;

/** Thumbstick to scroll mapping. Pure logic, JUnit-covered. */
public final class StickScroll {
    private StickScroll() {}

    public static final int DEADZONE = 6000;
    public static final int MAX_STEP_PX = 90;

    /** Stick up is positive and scrolls up (negative dy). */
    public static int deltaYPx(int stickY) {
        if (Math.abs(stickY) <= DEADZONE) return 0;
        float f = stickY / 32766f;
        if (f > 1f) f = 1f;
        if (f < -1f) f = -1f;
        return Math.round(-f * MAX_STEP_PX);
    }
}
