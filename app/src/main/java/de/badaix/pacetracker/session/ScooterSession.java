package de.badaix.pacetracker.session;

import android.content.Context;
import android.util.Pair;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.settings.SessionSettings;

public class ScooterSession extends Session {

    public ScooterSession(SessionListener listener, SessionSettings settings) {
        super(listener, settings);
    }

    @Override
    protected void initMET() {
        super.initMET();
        vMET.add(new Pair<Double, Double>(30.0, 3.5));
    }

    @Override
    public int getCaloriesInternal() {
        return getCaloriesByMET();
    }

    @Override
    public String getName(Context context) {
        if (context != null)
            return context.getResources().getString(R.string.sessionScooter);
        return "Rollerfahren";
    }

    @Override
    public int getDrawable() {
        return R.drawable.scooter;
    }

    @Override
    public int getLightDrawable() {
        return R.drawable.scooter_light;
    }
}
