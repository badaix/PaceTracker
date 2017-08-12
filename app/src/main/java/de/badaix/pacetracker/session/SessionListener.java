package de.badaix.pacetracker.session;

import de.badaix.pacetracker.PositionListener;
import de.badaix.pacetracker.sensor.SensorProvider.SensorListener;

public abstract interface SessionListener extends PositionListener, SensorListener {
    public abstract void onStateChanged(Session.State oldState, Session.State newState);

    public abstract void onSessionCommand(int command);

    public abstract void onFilteredLocationChanged(GpsPos location);

    public abstract void onSensorDataChanged(HxmData hxmData);
};
