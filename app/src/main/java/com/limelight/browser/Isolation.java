package com.limelight.browser;

// The bar's content lock: while it is on, presses, scrolls and hover stay
// off the page and only the furniture (bar, panels, keyboard) answers.
// Typing stays exempt by design: BrowserActivity never consults the lock
// from onVrKey, since key presses are deliberate.
public final class Isolation {
    private boolean on;

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean on) {
        this.on = on;
    }

    public boolean blocksContent() {
        return on;
    }
}
