package de.badaix.pacetracker.session;

import android.content.Context;
import android.util.Pair;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.settings.SessionSettings;

public class CarSession extends Session {

    public CarSession(SessionListener listener, SessionSettings settings) {
        super(listener, settings);
    }

    @Override
    protected void initMET() {
        super.initMET();
        vMET.add(new Pair<Double, Double>(50.0, 1.3));
    }

    @Override
    public int getCaloriesInternal() {
        return getCaloriesByMET();
    }

    @Override
    public String getName(Context context) {
        if (context != null)
            return context.getResources().getString(R.string.sessionCar);
        return "Autofahren";
    }

    @Override
    public int getDrawable() {
        return R.drawable.car;
    }

    @Override
    public int getLightDrawable() {
        return R.drawable.car_light;
    }
}
