package de.badaix.pacetracker.session;

import android.content.Context;
import android.util.Pair;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.settings.SessionSettings;

public class HikingSession extends Session {

    public HikingSession(SessionListener listener, SessionSettings settings) {
        super(listener, settings);
    }

    @Override
    protected void initMET() {
        super.initMET();
        vMET.add(new Pair<Double, Double>(5.0, 6.0));
    }

    @Override
    public int getCaloriesInternal() {
        return getCaloriesByMET();
    }

    @Override
    public String getName(Context context) {
        if (context != null)
            return context.getResources().getString(R.string.sessionHiking);
        return "Wandern";
    }

    @Override
    public int getDrawable() {
        return R.drawable.hiking;
    }

    @Override
    public int getLightDrawable() {
        return R.drawable.hiking_light;
    }
}
