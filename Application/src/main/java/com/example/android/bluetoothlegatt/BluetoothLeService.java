/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;


/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public final static String EXTRA_DATA_POW =
            "com.example.bluetooth.le.EXTRA_DATA_POW";
    public final static String EXTRA_DATA_SPD =
            "com.example.bluetooth.le.EXTRA_DATA_SPD";
    public final static String EXTRA_DATA_CAD =
            "com.example.bluetooth.le.EXTRA_DATA_CAD";
    public final static String EXTRA_DATA_ELITE_OOR_FLAG =
            "com.example.bluetooth.le.EXTRA_DATA_ELITE_OOR_FLAG";
    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);
    public final static UUID UUID_CSC_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.CSC_MEASUREMENT);
    public final static UUID UUID_CYCLING_POWER_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.CYCLING_POWER_MEASUREMENT);
    public final static UUID UUID_CYCLING_POWER_FEATURE =
            UUID.fromString(SampleGattAttributes.CYCLING_POWER_FEATURE);
    public final static UUID UUID_ELITE_OUT_OF_RANGE_FLAG =
            UUID.fromString(SampleGattAttributes.ELITE_TRAINER_OOR_FLAG);
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private final static String TAG_SET_POWER = "BluetoothLeService.potenza";
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    public static int WheelCircumference = 2070;
    private final IBinder mBinder = new LocalBinder();
    public int PrevCumulativeWheelRev;
    public int PrevLastWheelEventTime;
    public int PrevCumulativeCrankRev;
    public int PrevLastCrankEventTime;
    public int PrevCumulativeWheelRevPw;
    public int PrevLastWheelEventTimePw;
    public int PrevCumulativeCrankRevPw;
    public int PrevLastCrankEventTimePw;
    public double Speed;
    public double Cadence;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        boolean SpeedDataPresent = false;
        boolean CadenceDataPresent = false;
        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d("DATA", "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d("DATA", "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d("DATA", String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));

        } else if (UUID_CSC_MEASUREMENT.equals(characteristic.getUuid())) {
            final byte[] Data = characteristic.getValue();
            int format32 = -1;
            int format16 = -1;
            int flagValue = Data[0];
            if (flagValue == 1) {
                // solo velocita
                format32 = BluetoothGattCharacteristic.FORMAT_UINT32;
                format16 = BluetoothGattCharacteristic.FORMAT_UINT16;
                int CumulativeWheelRev = characteristic.getIntValue(format32, 1);
                Log.d("DATA", String.format("Cumulat. Wheel Rev.: %d", CumulativeWheelRev));
                int LastWheelEventTime = characteristic.getIntValue(format16, 5);
                Log.d("DATA", String.format("Last Wheel Event Time: %d", LastWheelEventTime));
                double temp = CumulativeWheelRev;
                final double Speed = ((temp - PrevCumulativeWheelRev) * 1024 * 3600 * WheelCircumference) / (LastWheelEventTime - PrevLastWheelEventTime) / 1000000;
                // TODO gestione overflow
                PrevCumulativeWheelRev = CumulativeWheelRev;
                PrevLastWheelEventTime = LastWheelEventTime;
                Log.d("DATA", String.format("Speed: %.1f", Speed));
                intent.putExtra(EXTRA_DATA, "vel: " + String.valueOf((double) Math.round(Speed * 10) / 10));
                intent.putExtra(EXTRA_DATA_SPD, Speed);
            } else if (flagValue == 2) {
                // solo cadenza  ((flag & 0x10) = 1)
                format32 = BluetoothGattCharacteristic.FORMAT_UINT32;
                format16 = BluetoothGattCharacteristic.FORMAT_UINT16;
                int CumulativeCrankRev = characteristic.getIntValue(format16, 1);
                Log.d("DATA", String.format("Cumulat. Crank Rev.: %d", CumulativeCrankRev));
                int LastCrankEventTime = characteristic.getIntValue(format16, 3);
                Log.d("DATA", String.format("Last Crank Event Time: %d", LastCrankEventTime));
                double temp = CumulativeCrankRev;
                if (temp != PrevCumulativeCrankRev){
                    Cadence = ((temp - PrevCumulativeCrankRev) * 1024 * 60) / (LastCrankEventTime - PrevLastCrankEventTime);
                    // TODO gestione overflow
                    PrevCumulativeCrankRev = CumulativeCrankRev;
                    PrevLastCrankEventTime = LastCrankEventTime;
                }
                Log.d("DATA", String.format("Speed: %.1f", Cadence));
                intent.putExtra(EXTRA_DATA, "Cad: " + String.valueOf((int) Cadence));
                intent.putExtra(EXTRA_DATA_CAD, (int) Cadence);



            } else if (flagValue == 3) {
                // vel e  cadenza
                format32 = BluetoothGattCharacteristic.FORMAT_UINT32;
                format16 = BluetoothGattCharacteristic.FORMAT_UINT16;
                int CumulativeWheelRev = characteristic.getIntValue(format32, 1);
                Log.d("DATA", String.format("Cumulat. Wheel Rev.: %d", CumulativeWheelRev));
                int LastWheelEventTime = characteristic.getIntValue(format16, 5);
                Log.d("DATA", String.format("Last Wheel Event Time: %d", LastWheelEventTime));
                double temp = CumulativeWheelRev;
                //fatto perch� altrimenti la divisione di interi da intero e non double
                final double Speed = ((temp - PrevCumulativeWheelRev) * 1024 * 3600 * WheelCircumference)
                        / (LastWheelEventTime - PrevLastWheelEventTime) / 1000000;
                // TODO gestione overflow

                int CumulativeCrankRev = characteristic.getIntValue(format16, 7);
                Log.d("DATA", String.format("Cumulat. Crank Rev.: %d", CumulativeCrankRev));
                int LastCrankEventTime = characteristic.getIntValue(format16, 9);
                Log.d("DATA", String.format("Last Crank Event Time: %d", LastCrankEventTime));
                temp = CumulativeCrankRev;
                if (LastCrankEventTime != PrevLastCrankEventTime) {
                    Cadence = ((temp - PrevCumulativeCrankRev) * 1024 * 60) / (LastCrankEventTime - PrevLastCrankEventTime);
                    // TODO gestione overflow
                    PrevCumulativeCrankRev = CumulativeCrankRev;
                    PrevLastCrankEventTime = LastCrankEventTime;
                    PrevCumulativeWheelRev = CumulativeWheelRev;
                    PrevLastWheelEventTime = LastWheelEventTime;
                }
                Log.d("DATA", String.format("Speed: %.1f; Cad: %f", Speed, Cadence));
                intent.putExtra(EXTRA_DATA, "vel: " + String.valueOf((double) Math.round(Speed * 10) / 10) + "; Cad: " + String.valueOf((int) Cadence));
                intent.putExtra(EXTRA_DATA_SPD, Speed);
                intent.putExtra(EXTRA_DATA_CAD, (int) Cadence);


            }
        } else if (UUID_CYCLING_POWER_MEASUREMENT.equals(characteristic.getUuid())) {
            final byte[] Data = characteristic.getValue();
            int format32 = -1;
            int format16 = -1;
            int formatS16 = -1;
            double SpeedPw = 0;
            int CadencePw = 0;
            formatS16 = BluetoothGattCharacteristic.FORMAT_SINT16;
            int Flags = characteristic.getIntValue(formatS16, 0);
            Log.d("DATA", String.format("Pow Meas Flag: %d", Flags));
            int InstaPow = characteristic.getIntValue(formatS16, 2);
            Log.d("DATA", String.format("Instantaneaous power: %d", InstaPow));

            if ((Flags & 0x10) == 0x10) {     // caso che ci sia il dato di velocità incorporato
                SpeedDataPresent = true;
                format32 = BluetoothGattCharacteristic.FORMAT_UINT32;
                format16 = BluetoothGattCharacteristic.FORMAT_UINT16;
                int CumulativeWheelRevPw = characteristic.getIntValue(format32, 4);
                Log.d("DATA", String.format("Cumulat. Wheel Rev.: %d", CumulativeWheelRevPw));
                int LastWheelEventTimePw = characteristic.getIntValue(format16, 8);
                Log.d("DATA", String.format("Last Wheel Event Time: %d", LastWheelEventTimePw));
                double temp = CumulativeWheelRevPw;
                SpeedPw = (double) ((temp - PrevCumulativeWheelRevPw) * 2048 * 3600 * WheelCircumference) / (LastWheelEventTimePw - PrevLastWheelEventTimePw) / 1000000;
                // TODO gestione overflow
                PrevCumulativeWheelRevPw = CumulativeWheelRevPw;
                PrevLastWheelEventTimePw = LastWheelEventTimePw;
                Log.d("DATA", String.format("Speed: %.1f", SpeedPw));
            }
            if ((Flags & 0x20) == 0x20) {     // caso che ci sia il dato di cadenza incorporato
                CadenceDataPresent = true;
                format32 = BluetoothGattCharacteristic.FORMAT_UINT32;
                format16 = BluetoothGattCharacteristic.FORMAT_UINT16;
                int CumulativeCrankRevPw = characteristic.getIntValue(format16, 10);
                Log.d("DATA", String.format("Cumulat. Crank Rev.: %d", CumulativeCrankRevPw));
                int LastCrankEventTimePw = characteristic.getIntValue(format16, 12);
                Log.d("DATA", String.format("Last Crank Event Time: %d", LastCrankEventTimePw));
                double temp = CumulativeCrankRevPw;
                CadencePw = (int) ((temp - PrevCumulativeCrankRevPw) * 1024 * 60) / (LastCrankEventTimePw - PrevLastCrankEventTimePw);
                // TODO gestione overflow
                PrevCumulativeCrankRevPw = CumulativeCrankRevPw;
                PrevLastCrankEventTimePw = LastCrankEventTimePw;
                Log.d("DATA", String.format("Cad: %d", CadencePw));

            }
            if ((SpeedDataPresent == false) && (CadenceDataPresent == false)) {
                intent.putExtra(EXTRA_DATA, "Pow: " + String.valueOf(InstaPow) + "Watt");
                intent.putExtra(EXTRA_DATA_POW, InstaPow);
            }
            if ((SpeedDataPresent == true) && (CadenceDataPresent == false)) {
                intent.putExtra(EXTRA_DATA, "Pow: " + String.valueOf(InstaPow) + "Watt; vel: " + String.valueOf((double) Math.round(SpeedPw * 10) / 10));
                intent.putExtra(EXTRA_DATA_POW, InstaPow);
                intent.putExtra(EXTRA_DATA_SPD, SpeedPw);
            }
            if ((SpeedDataPresent == false) && (CadenceDataPresent == true)) {
                intent.putExtra(EXTRA_DATA, "Pow: " + String.valueOf(InstaPow) + "Watt; Cad: " + String.valueOf(CadencePw));
                intent.putExtra(EXTRA_DATA_POW, InstaPow);
                intent.putExtra(EXTRA_DATA_CAD, (int) CadencePw);
            }
            if ((SpeedDataPresent == true) && (CadenceDataPresent = true)) {
                intent.putExtra(EXTRA_DATA, "Pow: " + String.valueOf(InstaPow) + "Watt; vel: " + String.valueOf((double) Math.round(SpeedPw * 10) / 10) + "; Cad: " + String.valueOf(CadencePw));
                intent.putExtra(EXTRA_DATA_POW, InstaPow);
                intent.putExtra(EXTRA_DATA_SPD, SpeedPw);
                intent.putExtra(EXTRA_DATA_CAD, (int) CadencePw);
            }
        } else if (UUID_CYCLING_POWER_FEATURE.equals(characteristic.getUuid())) {
            int formatS16 = -1;
            formatS16 = BluetoothGattCharacteristic.FORMAT_SINT16;
            int Flags = characteristic.getIntValue(formatS16, 0);
            Log.d("DATA", String.format("Pow feature: %d", Flags));
            intent.putExtra(EXTRA_DATA, "Pow feat.: " + String.valueOf(Flags));

        } else if (UUID_ELITE_OUT_OF_RANGE_FLAG.equals(characteristic.getUuid())) {
            int formatS16 = -1;
            formatS16 = BluetoothGattCharacteristic.FORMAT_SINT8;
            int Flags = characteristic.getIntValue(formatS16, 0);
            Log.d("DATA", String.format("Elite OoR Flag: %d", Flags));
            intent.putExtra(EXTRA_DATA_ELITE_OOR_FLAG, Flags);


        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }

        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }

        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void WriteCharacteristic(BluetoothGattCharacteristic characteristic, Integer brakeMode, Integer powerLevelValue) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        if (brakeMode.equals(0)) {
            byte[] dataToSend = new byte[3];
            dataToSend[0] = 0;
            dataToSend[1] = (byte) (powerLevelValue % 256);
            dataToSend[2] = (byte) (powerLevelValue / 256);

            characteristic.setValue(dataToSend);
            mBluetoothGatt.writeCharacteristic(characteristic);
            Log.w(TAG_SET_POWER, "Set Power: " + powerLevelValue.toString());
        } else {
            if (powerLevelValue > 200) {
                powerLevelValue = 200;
            }
            byte[] dataToSend = new byte[2];
            dataToSend[0] = 1;
            dataToSend[1] = (byte) (powerLevelValue % 256);

            characteristic.setValue(dataToSend);
            mBluetoothGatt.writeCharacteristic(characteristic);
            Log.w(TAG_SET_POWER, "Set Level: " + powerLevelValue.toString());
        }

    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
        // This is specific to CSC Measurement.
        if (UUID_CSC_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
        // This is specific to Power Measurement.
        if (UUID_CYCLING_POWER_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
        // This is specific to Power Measurement.
        if (UUID_ELITE_OUT_OF_RANGE_FLAG.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }
}
