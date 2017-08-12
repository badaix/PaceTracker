package de.badaix.pacetracker.activity;

import de.badaix.pacetracker.session.Session;
import de.badaix.pacetracker.session.SessionListener;

public abstract interface SessionUI extends SessionListener {
    public abstract void setSession(Session session);

    public abstract void update();
}
