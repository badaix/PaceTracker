package de.badaix.pacetracker.session;

import de.badaix.pacetracker.sensor.SensorData;

public class HxmData {
    public short heartRate = -1;
    public short cadence = -1;
    public long time;
    public long duration;
    public double distance;

    HxmData(SensorData sensorData, long duration, double distance) {
        this.time = sensorData.getCreationTime().getTime();
        if (sensorData.hasHeartRate())
            this.heartRate = (short) sensorData.getHeartRate();
        if (sensorData.hasCadence())
            this.cadence = (short) sensorData.getCadence();
        this.distance = distance;
        this.duration = duration;
    }

	/*
     * public boolean hasHeartRate() { return (heartRate != -1); }
	 * 
	 * public boolean hasCadence() { return (cadence != -1); }
	 */
}
