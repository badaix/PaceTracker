package de.badaix.pacetracker.activity;

public abstract interface SessionGUI extends SessionUI {
    public abstract void onGuiTimer(boolean resumed);
}
