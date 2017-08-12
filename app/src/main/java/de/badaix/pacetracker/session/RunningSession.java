package de.badaix.pacetracker.session;

import android.content.Context;
import android.util.Pair;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.settings.SessionSettings;

public class RunningSession extends Session {
    // private float weight;

    public RunningSession(SessionListener listener, SessionSettings settings) {
        super(listener, settings);
    }

    @Override
    protected void initMET() {
        super.initMET();
        vMET.add(new Pair<Double, Double>(1.0, 0.9));
        vMET.add(new Pair<Double, Double>(6.437376, 6.0));
        vMET.add(new Pair<Double, Double>(8.04672, 8.3));
        vMET.add(new Pair<Double, Double>(8.3685888, 9.0));
        vMET.add(new Pair<Double, Double>(9.656064, 9.8));
        vMET.add(new Pair<Double, Double>(10.7826048, 10.5));
        vMET.add(new Pair<Double, Double>(11.265408, 11.0));
        vMET.add(new Pair<Double, Double>(12.07008, 11.8));
        vMET.add(new Pair<Double, Double>(13.8403584, 12.3));
        vMET.add(new Pair<Double, Double>(14.484096, 12.8));
        vMET.add(new Pair<Double, Double>(16.09344, 14.5));
        vMET.add(new Pair<Double, Double>(17.702784, 16.0));
        vMET.add(new Pair<Double, Double>(19.312128, 19.0));
        vMET.add(new Pair<Double, Double>(20.921472, 19.8));
        vMET.add(new Pair<Double, Double>(22.530816, 23.0));
        vMET.add(new Pair<Double, Double>(100.0, 100.0));
    }

    @Override
    public int getCaloriesInternal() {
        return getCaloriesByMET();
        // if (state == State.INIT)
        // return 0;
        //
        // return (int)(getDistance() / 1000. * 0.9 * weight);
    }

    @Override
    public String getName(Context context) {
        if (context != null)
            return context.getResources().getString(R.string.sessionRunning);
        return "Laufen";
    }

    @Override
    public int getDrawable() {
        return R.drawable.running;
    }

    @Override
    public int getLightDrawable() {
        return R.drawable.running_light;
    }

}
