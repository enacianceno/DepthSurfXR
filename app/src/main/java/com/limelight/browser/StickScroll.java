package com.limelight.browser;

/** Thumbstick to scroll mapping. Pure logic, JUnit-covered. */
public final class StickScroll {
    private StickScroll() {}

    public static final int DEADZONE = 6000;
    public static final int MAX_STEP_PX = 90;

    /** Wheel steps for one tick: sign follows deltaYPx, 1..3 by tilt. */
    public static int wheelSteps(int stickY) {
        int px = deltaYPx(stickY);
        if (px == 0) return 0;
        int mag = Math.abs(px);
        int steps = mag > 70 ? 3 : (mag > 35 ? 2 : 1);
        return px > 0 ? steps : -steps;
    }

    /** Stick up is positive and scrolls up (negative dy). */
    public static int deltaYPx(int stickY) {
        if (Math.abs(stickY) <= DEADZONE) return 0;
        float f = stickY / 32766f;
        if (f > 1f) f = 1f;
        if (f < -1f) f = -1f;
        return Math.round(-f * MAX_STEP_PX);
    }
}
