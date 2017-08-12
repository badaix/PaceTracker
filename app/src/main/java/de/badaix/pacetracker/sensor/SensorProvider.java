package de.badaix.pacetracker.sensor;

public abstract class SensorProvider {

    // protected Context context;
    protected SensorListener listener;
    protected String sensorName;

    public SensorProvider(String sensorName) {
        // this.context = context;
        this.sensorName = sensorName;
    }

    public abstract Sensor getSensor();

    public boolean supportsHeartrate() {
        return false;
    }

    public boolean supportsCadence() {
        return false;
    }

    public boolean supportsBattery() {
        return false;
    }

    public boolean supportsPower() {
        return false;
    }

    public abstract void start(SensorListener listener) throws Exception;

    public abstract void stop();

    public enum ConnectionType {
        NONE, BLUETOOTH;
    }

    public enum SensorState {
        NONE(0), CONNECTING(1), CONNECTED(2), DISCONNECTED(3), SENDING(4), NODEVICE(5);

        private int number;

        SensorState(int number) {
            this.number = number;
        }

        public static SensorState fromNumber(int number) {
            switch (number) {
                case 0:
                    return NONE;
                case 1:
                    return CONNECTING;
                case 2:
                    return CONNECTED;
                case 3:
                    return DISCONNECTED;
                case 4:
                    return SENDING;
                case 5:
                    return NODEVICE;
            }
            return NONE;
        }

        public boolean isConnected() {
            return this.equals(CONNECTED) || this.equals(SENDING);
        }

        public boolean isConnecting() {
            return this.equals(CONNECTING);
        }

        public boolean isDisconnected() {
            return this.equals(NODEVICE) || this.equals(NONE) || this.equals(DISCONNECTED);
        }

        public int getNumber() {
            return number;
        }
    }

    public abstract interface SensorListener {
        public void onSensorData(SensorProvider provider, SensorData sensorData);

        public void onSensorStateChanged(SensorProvider provider, boolean active, SensorState sensorState);
    }
}
