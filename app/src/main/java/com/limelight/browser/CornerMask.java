package com.limelight.browser;

/**
 * Pure mirror of the warp shader's cornerAlpha(): rounded-box alpha over
 * frame uv. The shader stays the real implementation; this pins its math so
 * a future edit that breaks the shape fails the suite first.
 */
public final class CornerMask {
    private CornerMask() {
    }

    /** Radius the renderer uses, as a fraction of frame height. */
    public static final float RADIUS = 0.0425f;
    /** Glow quad size over picture size (shader's scale const). */
    public static final float GLOW_SCALE = 1.7f;
    /** Halo border along the axes, in picture heights. */
    public static final float GLOW_BORDER = 0.5f * (GLOW_SCALE - 1.0f);

    public static float alpha(float u, float v, float radius, float aspect) {
        if (radius < 0.0005f) {
            return 1.0f;
        }
        float px = u * aspect;
        float cx = aspect * 0.5f;
        float cy = 0.5f;
        float qx = Math.abs(px - cx) - (cx - radius);
        float qy = Math.abs(v - cy) - (cy - radius);
        float ax = Math.max(qx, 0.0f);
        float ay = Math.max(qy, 0.0f);
        float d = (float) (Math.sqrt(ax * ax + ay * ay)
                + Math.min(Math.max(qx, qy), 0.0)) - radius;
        return 1.0f - smoothstep(-0.0015f, 0.0015f, d);
    }

    static float smoothstep(float e0, float e1, float x) {
        float t = (x - e0) / (e1 - e0);
        t = Math.max(0.0f, Math.min(1.0f, t));
        return t * t * (3.0f - 2.0f * t);
    }

    /**
     * Mirror of the glow shader's halo fade: distance from the rounded
     * picture silhouette over picture space (fuv, 0..1 across the picture),
     * 0 on the silhouette, 1 at the rim along the axes.
     */
    public static float haloT(float fu, float fv, float radius, float aspect) {
        float px = fu * aspect;
        float cx = aspect * 0.5f;
        float cy = 0.5f;
        float qx = Math.abs(px - cx) - (cx - radius);
        float qy = Math.abs(fv - cy) - (cy - radius);
        float ax = Math.max(qx, 0.0f);
        float ay = Math.max(qy, 0.0f);
        float d = (float) (Math.sqrt(ax * ax + ay * ay)
                + Math.min(Math.max(qx, qy), 0.0)) - radius;
        return Math.max(0.0f, Math.min(1.0f, d / GLOW_BORDER));
    }
}
