/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.badaix.pacetracker.sensor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import de.badaix.pacetracker.sensor.SensorProvider.SensorState;
import de.badaix.pacetracker.util.Hint;

/**
 * Manages bluetooth connection. It has a thread for connecting with a bluetooth
 * device and a thread for performing data transmission when connected.
 *
 * @author Sandor Dornbush
 */
public class BluetoothConnectionManager {

    // Message types sent from the BluetoothSenorService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    // Key names received from the BluetoothSenorService Handler
    public static final String DEVICE_NAME = "device_name";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // Member fields
    private final BluetoothAdapter adapter;
    private final Handler handler;
    private MessageParser parser;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private SensorState state;

    /**
     * Constructor. Prepares a new BluetoothSensor session.
     *
     * @param handler A Handler to send messages back to the UI Activity
     * @param parser  A message parser
     */
    public BluetoothConnectionManager(Handler handler, MessageParser parser) {
        this.adapter = BluetoothAdapter.getDefaultAdapter();
        this.state = SensorState.NONE;
        this.handler = handler;
        this.parser = parser;
    }

    /**
     * Return the current connection state.
     */
    public SensorState getState() {
        return state;
    }

    /**
     * Set the current state of the sensor connection
     *
     * @param state An integer defining the current connection state
     */
    private void setState(SensorState state) {
        // TODO pretty print this.
        Hint.log(this, "setState(" + state + ")");
        this.state = state;

        // Give the new state to the Handler so the UI Activity can update
        handler.obtainMessage(MESSAGE_STATE_CHANGE, state.getNumber(), -1).sendToTarget();
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (device == null) {
            Hint.log(this, "Device is null");
            setState(SensorState.NODEVICE);
            return;
        }

        Hint.log(this, "connect to: " + device);

        stop(null);
        // Start the thread to connect with the given device
        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(SensorState.CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Hint.log(this, "connected");

        stop(null);
        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = handler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, device.getName());
        msg.setData(bundle);
        handler.sendMessage(msg);

        setState(SensorState.CONNECTED);
    }

    private void stop(SensorState state) {
        Hint.log(this, "stop()");
        if (connectThread != null) {
            connectThread.cancel();
            try {
                connectThread.join(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            connectThread = null;
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            try {
                connectedThread.join(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            connectedThread = null;
        }
        if (state != null)
            setState(state);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        stop(SensorState.NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (state != SensorState.CONNECTED) {
                return;
            }
            r = connectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * This thread runs while attempting to make an outgoing connection with a
     * device. It runs straight through; the connection either succeeds or
     * fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            setName("ConnectThread-" + device.getName());
            this.device = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = getSocket();
            } catch (IOException e) {
                Hint.log(this, e);
            }
            socket = tmp;
        }

        private BluetoothSocket getSocket() throws IOException {
            return device.createRfcommSocketToServiceRecord(SPP_UUID);
        }

        @Override
        public void run() {
            Hint.log(this, "BEGIN mConnectThread");

            // Always cancel discovery because it will slow down a connection
            adapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket.connect();
                } catch (IOException e) {
                    Hint.log(this, e);
                    setState(SensorState.DISCONNECTED);
                    // Close the socket
                    try {
                        socket.close();
                    } catch (IOException e2) {
                        Hint.log(this, e2);
                    }
                    return;
                }
            } catch (Exception e) {
                Hint.log(this, e);
                setState(SensorState.DISCONNECTED);
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothConnectionManager.this) {
                connectThread = null;
            }

            // Start the connected thread
            connected(socket, device);
        }

        public void cancel() {
            interrupt();
            try {
                socket.close();
            } catch (IOException e) {
                Hint.log(this, e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device. It handles all
     * incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket btSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Hint.log(this, "create ConnectedThread");
            btSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Hint.log(this, e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            Hint.log(this, "BEGIN mConnectedThread");

            byte[] buffer = new byte[16];
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    int read = mmInStream.read(buffer, 0, buffer.length);
                    if (read < 0)
                        throw new IOException("EOF reached");

                    for (int i = 0; i < read; ++i) {
                        byte[] result = parser.processChar(buffer[i]);
                        if (result != null) {
                            handler.obtainMessage(MESSAGE_READ, -1, -1, result).sendToTarget();
                        }
                    }
                } catch (IOException e) {
                    Hint.log(this, e);
                    setState(SensorState.DISCONNECTED);
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                handler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Hint.log(this, e);
            }
        }

        public void cancel() {
            try {
                interrupt();
                try {
                    mmInStream.close();
                } catch (IOException e) {
                    Hint.log(this, e);
                }
                try {
                    mmOutStream.close();
                } catch (IOException e) {
                    Hint.log(this, e);
                }
                btSocket.close();
            } catch (IOException e) {
                Hint.log(this, e);
            }
        }
    }
}
