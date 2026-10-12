package com.limelight.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CornerMaskTest {
    private static final float ASPECT = 16.0f / 9.0f;
    private static final float R = CornerMask.RADIUS;

    @Test
    public void centerIsOpaque() {
        assertEquals(1.0f, CornerMask.alpha(0.5f, 0.5f, R, ASPECT), 0.001f);
    }

    @Test
    public void edgeMidpointsAreOpaque() {
        assertEquals(1.0f, CornerMask.alpha(0.5f, 0.005f, R, ASPECT), 0.001f);
        assertEquals(1.0f, CornerMask.alpha(0.005f, 0.5f, R, ASPECT), 0.001f);
        assertEquals(1.0f, CornerMask.alpha(0.995f, 0.5f, R, ASPECT), 0.001f);
        assertEquals(1.0f, CornerMask.alpha(0.5f, 0.995f, R, ASPECT), 0.001f);
        // Exact frame edge straddles the 1px feather.
        assertEquals(0.5f, CornerMask.alpha(0.5f, 0.0f, R, ASPECT), 0.001f);
    }

    @Test
    public void farCornersAreTransparent() {
        assertEquals(0.0f, CornerMask.alpha(0.0f, 0.0f, R, ASPECT), 0.001f);
        assertEquals(0.0f, CornerMask.alpha(1.0f, 0.0f, R, ASPECT), 0.001f);
        assertEquals(0.0f, CornerMask.alpha(0.0f, 1.0f, R, ASPECT), 0.001f);
        assertEquals(0.0f, CornerMask.alpha(1.0f, 1.0f, R, ASPECT), 0.001f);
    }

    @Test
    public void arcCenterIsOpaque() {
        // Center of the corner arc circle: d = -radius, fully inside.
        float u = R / ASPECT;
        assertEquals(1.0f, CornerMask.alpha(u, R, R, ASPECT), 0.001f);
    }

    @Test
    public void zeroRadiusIsSquare() {
        assertEquals(1.0f, CornerMask.alpha(0.0f, 0.0f, 0.0f, ASPECT), 0.001f);
    }

    @Test
    public void symmetric() {
        float a = CornerMask.alpha(0.002f, 0.005f, R, ASPECT);
        assertEquals(a, CornerMask.alpha(0.998f, 0.005f, R, ASPECT), 0.0001f);
        assertEquals(a, CornerMask.alpha(0.002f, 0.995f, R, ASPECT), 0.0001f);
        assertTrue(a < 1.0f);
    }

    @Test
    public void haloZeroInsideAndOnSilhouette() {
        assertEquals(0.0f, CornerMask.haloT(0.5f, 0.5f, R, ASPECT), 0.001f);
        assertEquals(0.0f, CornerMask.haloT(0.5f, 1.0f, R, ASPECT), 0.001f);
        assertEquals(0.0f, CornerMask.haloT(1.0f, 0.5f, R, ASPECT), 0.001f);
    }

    @Test
    public void haloOneAtAxisRim() {
        // Glow rim in picture space: 0.5*scale+0.5 along each axis.
        float rim = 0.5f * CornerMask.GLOW_SCALE + 0.5f;
        assertEquals(1.0f, CornerMask.haloT(0.5f, rim, R, ASPECT), 0.001f);
        assertEquals(1.0f, CornerMask.haloT(rim, 0.5f, R, ASPECT), 0.001f);
    }

    @Test
    public void haloSmallInCutCorner() {
        // Picture corner tip sits just off the arc: nearly full glow there,
        // which is why the hard corner cut stays on top of the halo.
        float t = CornerMask.haloT(0.0f, 0.0f, R, ASPECT);
        assertTrue(t > 0.0f && t < 0.15f);
    }

    @Test
    public void haloFollowsRoundedDiagonal() {
        // Same distance out along the diagonal fades equally: a ring,
        // not the square halo's longer diagonal reach.
        float a = CornerMask.haloT(-0.1f, -0.1f, R, ASPECT);
        float b = CornerMask.haloT(1.1f, -0.1f, R, ASPECT);
        assertEquals(a, b, 0.0001f);
        assertTrue(a > 0.0f && a < 1.0f);
    }
}
