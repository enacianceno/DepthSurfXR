package com.limelight.browser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IsolationTest {
    @Test
    public void startsUnlocked() {
        Isolation isolation = new Isolation();
        assertFalse(isolation.isOn());
        assertFalse(isolation.blocksContent());
    }

    @Test
    public void lockBlocksContent() {
        Isolation isolation = new Isolation();
        isolation.setOn(true);
        assertTrue(isolation.isOn());
        assertTrue(isolation.blocksContent());
    }

    @Test
    public void unlockPassesContentAgain() {
        Isolation isolation = new Isolation();
        isolation.setOn(true);
        isolation.setOn(false);
        assertFalse(isolation.isOn());
        assertFalse(isolation.blocksContent());
    }
}
