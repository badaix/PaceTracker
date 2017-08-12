package de.badaix.pacetracker.sensor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.util.Set;

import de.badaix.pacetracker.sensor.Sensor.SensorType;
import de.badaix.pacetracker.util.Hint;

public class SensorManager {

    private static SensorProvider provider = new SensorProviderNone("");

    public static boolean hasBluetooth() {
        return (BluetoothAdapter.getDefaultAdapter() != null);
    }

    public static boolean isBluetoothEnabled() {
        if (!hasBluetooth())
            return false;
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    public static BluetoothDevice getBluetoothSensor(String sensorName) {
        Hint.log("SensorManager", "getBluetoothSensor " + sensorName);
        BluetoothDevice[] devices = getBluetoothSensors();
        for (int i = 0; i < devices.length; ++i)
            if (devices[i].getName().equals(sensorName))
                return devices[i];
        return null;
    }

    public static BluetoothDevice[] getBluetoothSensors() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        BluetoothDevice[] devices = null;
        if (bluetoothAdapter != null) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

            devices = new BluetoothDevice[pairedDevices.size()];
            int i = 0;
            // If there are paired devices
            for (BluetoothDevice pairedDevice : pairedDevices) {
                devices[i++] = pairedDevice;
            }
        }
        return devices;
    }

    public static void stopSensor() {
        if (provider != null)
            provider.stop();
    }

    public static SensorProvider getSensorProvider(Sensor sensor) {
        Hint.log("SensorManager", "getSensorProvider Name: " + sensor.getName() + ", Type: "
                + sensor.getType().getType());
        if ((provider != null) && provider.getSensor().equals(sensor))
            return provider;

        stopSensor();
        if (sensor.getType() == SensorType.POLAR)
            provider = new SensorProviderPolar(sensor.getName());
        else
            provider = new SensorProviderNone(sensor.getName());

        return provider;
    }
}
