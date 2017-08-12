package de.badaix.pacetracker.sensor;

import java.util.Date;

public class SensorData {
    private Date creationTime;
    private short heartRate = -1;
    private short power = -1;
    private short cadence = -1;
    private float batteryLevel = -1.f;

    public SensorData() {
        creationTime = new Date();
    }

    public boolean hasHeartRate() {
        return heartRate != -1;
    }

    public boolean hasPower() {
        return power != -1;
    }

    public boolean hasCadence() {
        return cadence != -1;
    }

    public boolean hasBatteryLevel() {
        return batteryLevel > -1.f;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public SensorData setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
        return this;
    }

    public short getHeartRate() {
        return heartRate;
    }

    public SensorData setHeartRate(int heartRate) {
        this.heartRate = (short) heartRate;
        return this;
    }

    public int getPower() {
        return power;
    }

    public SensorData setPower(int power) {
        this.power = (short) power;
        return this;
    }

    public int getCadence() {
        return cadence;
    }

    public SensorData setCadence(int cadence) {
        this.cadence = (short) cadence;
        return this;
    }

    public float getBatteryLevel() {
        return batteryLevel;
    }

    public SensorData setBatteryLevel(float percent) {
        this.batteryLevel = percent;
        return this;
    }

    public SensorData setBatteryLevel(int level, int maxLevel) {
        this.batteryLevel = (float) level / (float) maxLevel;
        return this;
    }

}
