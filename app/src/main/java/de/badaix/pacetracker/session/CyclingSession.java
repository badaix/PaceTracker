package de.badaix.pacetracker.session;

import android.content.Context;
import android.util.Pair;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.settings.SessionSettings;

public class CyclingSession extends Session {

    public CyclingSession(SessionListener listener, SessionSettings settings) {
        super(listener, settings);
    }

    @Override
    protected void initMET() {
        super.initMET();
        vMET.add(new Pair<Double, Double>(1.0, 0.39));
        vMET.add(new Pair<Double, Double>(8.851392, 3.5));
        vMET.add(new Pair<Double, Double>(15.1278336, 5.8));
        vMET.add(new Pair<Double, Double>(16.09344, 6.8));
        vMET.add(new Pair<Double, Double>(19.312128, 8.0));
        vMET.add(new Pair<Double, Double>(22.530816, 10.0));
        vMET.add(new Pair<Double, Double>(25.749504, 12.0));
        vMET.add(new Pair<Double, Double>(32.18688, 15.8));
        vMET.add(new Pair<Double, Double>(100.0, 50.0));
    }

    @Override
    public int getCaloriesInternal() {
        return getCaloriesByMET();
    }

    @Override
    public String getName(Context context) {
        if (context != null)
            return context.getResources().getString(R.string.sessionCycling);
        return "Fahrradfahren";
    }

    @Override
    public int getDrawable() {
        return R.drawable.cycling;
    }

    @Override
    public int getLightDrawable() {
        return R.drawable.cycling_light;
    }
}
