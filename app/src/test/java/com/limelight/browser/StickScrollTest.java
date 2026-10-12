package com.limelight.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class StickScrollTest {
    @Test public void deadzoneIsStill() {
        assertEquals(0, StickScroll.deltaYPx(0));
        assertEquals(0, StickScroll.deltaYPx(5000));
        assertEquals(0, StickScroll.deltaYPx(-5000));
    }
    @Test public void upScrollsUpDownScrollsDown() {
        assertTrue(StickScroll.deltaYPx(32766) < 0);
        assertTrue(StickScroll.deltaYPx(-32766) > 0);
        assertEquals(-StickScroll.MAX_STEP_PX, StickScroll.deltaYPx(32766));
    }
}
