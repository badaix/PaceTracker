package de.badaix.pacetracker.session;

import android.content.Context;

import de.badaix.pacetracker.settings.SessionSettings;

public class TestSession extends Session {

    public TestSession(SessionListener listener, SessionSettings settings) {
        super(listener, settings);
    }

    @Override
    public int getCaloriesInternal() {
        // TODO Auto-generated method stub
        return 42;
    }

    @Override
    public String getName(Context context) {
        return "Test";
    }
}
