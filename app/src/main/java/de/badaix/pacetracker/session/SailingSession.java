package de.badaix.pacetracker.session;

import android.content.Context;
import android.util.Pair;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.settings.SessionSettings;

public class SailingSession extends Session {

    public SailingSession(SessionListener listener, SessionSettings settings) {
        super(listener, settings);
    }

    @Override
    protected void initMET() {
        super.initMET();
        vMET.add(new Pair<Double, Double>(5.0, 3.3));
    }

    @Override
    public int getCaloriesInternal() {
        return getCaloriesByMET();
    }

    @Override
    public String getName(Context context) {
        if (context != null)
            return context.getResources().getString(R.string.sessionSailing);
        return "Segeln";
    }

    @Override
    public int getDrawable() {
        return R.drawable.sailing;
    }

    @Override
    public int getLightDrawable() {
        return R.drawable.sailing_light;
    }
}
