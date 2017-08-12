package de.badaix.pacetracker.sensor;

import de.badaix.pacetracker.sensor.Sensor.SensorType;

public class SensorProviderNone extends SensorProvider {

    public SensorProviderNone(String sensorName) {
        super(sensorName);
    }

    @Override
    public void start(SensorListener listener) throws Exception {
    }

    @Override
    public void stop() {
    }

    @Override
    public Sensor getSensor() {
        return new Sensor(SensorType.NONE, sensorName);
    }

}
