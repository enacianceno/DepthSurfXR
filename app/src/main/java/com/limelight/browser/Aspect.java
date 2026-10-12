package com.limelight.browser;

// Producer pixels for the browser display: the XR session starts at the
// landscape preset and free corner drags reshape it live, so there is no
// orientation state to keep, only dims for a height over width.
public final class Aspect {
    public static final int LAND_W = 1280;
    public static final int LAND_H = 720;
    public static final int PORT_W = 720;
    public static final int PORT_H = 1280;
    public static final float MIN_ASPECT = 0.5f;
    public static final float MAX_ASPECT = 2.0f;
    private static final int AREA = LAND_W * LAND_H;

    private Aspect() {
    }

    /**
     * Producer pixels for a live height-over-width: constant area (~one
     * 720p frame), even dims, exact presets when the drag lands on them.
     * Returns {w, h}.
     */
    public static int[] pixelsFor(float aspect) {
        float a = aspect < MIN_ASPECT ? MIN_ASPECT
                : (aspect > MAX_ASPECT ? MAX_ASPECT : aspect);
        if (Math.abs(a - 9.0f / 16.0f) < 0.01f) {
            return new int[] { LAND_W, LAND_H };
        }
        if (Math.abs(a - 16.0f / 9.0f) < 0.01f) {
            return new int[] { PORT_W, PORT_H };
        }
        int h = (int) (Math.sqrt(AREA * a) / 2) * 2;
        int w = (int) (h / a / 2) * 2;
        if (w < 480) {
            w = 480;
        }
        if (w > 1600) {
            w = 1600;
        }
        if (h < 480) {
            h = 480;
        }
        if (h > 1600) {
            h = 1600;
        }
        return new int[] { w, h };
    }
}
