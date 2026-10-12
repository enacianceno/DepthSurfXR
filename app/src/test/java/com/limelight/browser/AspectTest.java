package com.limelight.browser;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AspectTest {
    @Test
    public void pixelsForPresetsAreExact() {
        assertArrayEquals(new int[] { 1280, 720 }, Aspect.pixelsFor(9.0f / 16.0f));
        assertArrayEquals(new int[] { 720, 1280 }, Aspect.pixelsFor(16.0f / 9.0f));
    }

    @Test
    public void pixelsForSquareKeepsAreaEven() {
        assertArrayEquals(new int[] { 960, 960 }, Aspect.pixelsFor(1.0f));
    }

    @Test
    public void pixelsForClampsAndStaysEven() {
        int[] low = Aspect.pixelsFor(0.1f);
        assertEquals(0, low[0] % 2);
        assertEquals(0, low[1] % 2);
        assertTrue(low[0] > low[1]);
        int[] high = Aspect.pixelsFor(9.0f);
        assertEquals(0, high[0] % 2);
        assertEquals(0, high[1] % 2);
        assertTrue(high[1] > high[0]);
    }

    @Test
    public void pixelsForDragExtremesAreWideAndTall() {
        int[] wide = Aspect.pixelsFor(Aspect.MIN_ASPECT);
        assertEquals(0, wide[0] % 2);
        assertEquals(0, wide[1] % 2);
        assertTrue(wide[0] > wide[1]);
        int[] tall = Aspect.pixelsFor(Aspect.MAX_ASPECT);
        assertEquals(0, tall[0] % 2);
        assertEquals(0, tall[1] % 2);
        assertTrue(tall[1] > tall[0]);
    }
}
