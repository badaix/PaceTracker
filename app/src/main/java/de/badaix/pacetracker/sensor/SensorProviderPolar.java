package de.badaix.pacetracker.sensor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;

import de.badaix.pacetracker.sensor.Sensor.SensorType;
import de.badaix.pacetracker.util.Hint;

public class SensorProviderPolar extends SensorProvider implements Callback {
    private BluetoothConnectionManager connectionManager;
    private Handler handler;
    private PolarMessageParser parser;
    private Runnable spawner = null;
    private BluetoothDevice device = null;
    private boolean started = false;

    // private boolean firstStart = true;
    // private boolean wasBtEnabled = false;

    public SensorProviderPolar(String sensorName) {
        super(sensorName);
        parser = new PolarMessageParser();
        handler = new Handler(this);
        connectionManager = new BluetoothConnectionManager(handler, parser);
    }

    @Override
    public Sensor getSensor() {
        return new Sensor(SensorType.POLAR, sensorName);
    }

    @Override
    public boolean supportsBattery() {
        return true;
    }

    @Override
    public boolean supportsHeartrate() {
        return true;
    }

    private boolean enableBt(boolean enable) throws Exception {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null)
            throw new Exception("Bluetooth not available");

        // if (firstStart) {
        // wasBtEnabled = adapter.isEnabled();
        // firstStart = false;
        // }

        if (enable == adapter.isEnabled())
            return true;

        if (enable)
            return adapter.enable();
        else
            // if (!wasBtEnabled)
            return adapter.disable();

        // return true;
    }

    @Override
    public void start(SensorListener listener) throws Exception {
        this.listener = listener;

        if ((connectionManager.getState() != SensorState.DISCONNECTED)
                && (connectionManager.getState() != SensorState.NONE))
            return;
        started = true;
        enableBt(true);
        device = SensorManager.getBluetoothSensor(sensorName);
        connectionManager.connect(device);
    }

    @Override
    public void stop() {
        started = false;
        try {
            enableBt(false);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        handler.removeCallbacks(spawner);
        if (connectionManager.getState() == SensorState.NONE)
            return;

        if (connectionManager != null) {
            connectionManager.stop();
        }
        this.listener = null;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case BluetoothConnectionManager.MESSAGE_STATE_CHANGE:
                // TODO should we update the SensorManager state var?
                if (listener != null)
                    listener.onSensorStateChanged(this, true, SensorState.fromNumber(msg.arg1));

                if (started
                        && ((SensorState.fromNumber(msg.arg1) == SensorState.DISCONNECTED) || (SensorState
                        .fromNumber(msg.arg1) == SensorState.NODEVICE))) {
                    this.handler.removeCallbacks(spawner);
                    spawner = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                enableBt(true);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            device = SensorManager.getBluetoothSensor(sensorName);
                            connectionManager.connect(device);
                            spawner = null;
                        }
                    };
                    this.handler.postDelayed(spawner, 2000 + (int) (2000 * Math.random()));
                }
            /*
			 * if (SensorState.fromNumber(msg.arg1) == SensorState.DISCONNECTED)
			 * {
			 * 
			 * timerTask = new TimerTask() {
			 * 
			 * @Override public void run() { try { Thread.sleep(1000); } catch
			 * (InterruptedException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); } connectionManager.connect(sensor); } };
			 * timerTask.run(); }
			 */
                Hint.log(this, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                break;
            case BluetoothConnectionManager.MESSAGE_WRITE:
                break;
            case BluetoothConnectionManager.MESSAGE_READ:
                byte[] readBuf = null;
                try {
                    readBuf = (byte[]) msg.obj;
                    // Hint.log(this, "MESSAGE_READ: " + sensorDataSet.toString());
                    if (listener != null)
                        listener.onSensorData(this, parser.parseBuffer(readBuf));
                    // tv.setText("HR: " + sensorData.getHeartRate() + ", Battery: "
                    // + sensorData.getBatteryLevel() * 100);
                } catch (IllegalArgumentException iae) {
                    Hint.log(this, "Got bad sensor data: " + new String(readBuf, 0, readBuf.length));
                } catch (RuntimeException re) {
                    Hint.log(this, re);
                }
                break;
            case BluetoothConnectionManager.MESSAGE_DEVICE_NAME:
                // save the connected device's name
                String connectedDeviceName = msg.getData().getString(BluetoothConnectionManager.DEVICE_NAME);
                Hint.log(this, "Connected to " + connectedDeviceName);
                break;
        }
        return true;
    }

}
