package de.badaix.pacetracker.session;

import android.content.Context;
import android.util.Pair;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.settings.SessionSettings;

public class InlineSession extends Session {

    public InlineSession(SessionListener listener, SessionSettings settings) {
        super(listener, settings);
    }

    @Override
    protected void initMET() {
        super.initMET();
        vMET.add(new Pair<Double, Double>(1.0, 0.5));
        vMET.add(new Pair<Double, Double>(14.4, 7.5));
        vMET.add(new Pair<Double, Double>(17.7, 9.8));
        vMET.add(new Pair<Double, Double>(21.0, 12.3));
        vMET.add(new Pair<Double, Double>(24.0, 14.0));
        vMET.add(new Pair<Double, Double>(100.0, 60.0));
    }

    @Override
    public int getCaloriesInternal() {
        return getCaloriesByMET();
    }

    @Override
    public String getName(Context context) {
        if (context != null)
            return context.getResources().getString(R.string.sessionInline);
        return "Inline";
    }

    @Override
    public int getDrawable() {
        return R.drawable.inline_skating;
    }

    @Override
    public int getLightDrawable() {
        return R.drawable.inline_skating_light;
    }
}
