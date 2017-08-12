package de.badaix.pacetracker.posprovider;

import android.content.Context;

public class ManualPositionProvider extends PositionProvider {

    ManualPositionProvider(Context context, boolean offline) {
        super(context, offline);
        hasLocationInfo = false;
    }

    @Override
    protected void startInternal() throws Exception {
    }

    @Override
    protected void stopInternal() {
    }

}
