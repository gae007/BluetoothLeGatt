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

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String CSC_MEASUREMENT = "00002a5b-0000-1000-8000-00805f9b34fb";
    public static String CSC_FEATURE = "00002a5c-0000-1000-8000-00805f9b34fb";
    public static String CSC_SENSOR_LOCATION = "00002a5d-0000-1000-8000-00805f9b34fb";
    public static String CYCLING_POWER_CONTROL_POINT = "00002a66-0000-1000-8000-00805f9b34fb";
    public static String CYCLING_POWER_FEATURE = "00002a65-0000-1000-8000-00805f9b34fb";
    public static String CYCLING_POWER_MEASUREMENT = "00002a63-0000-1000-8000-00805f9b34fb";
    public static String CYCLING_POWER_VECTOR = "00002a64-0000-1000-8000-00805f9b34fb";
    public static String DATE_TIME = "00002a08-0000-1000-8000-00805f9b34fb";
    public static String HEART_RATE_MAX = "00002a8d-0000-1000-8000-00805f9b34fb";
    public static String HUMIDITY = "00002a6f-0000-1000-8000-00805f9b34fb";
    public static String LOCATION_AND_SPEED = "00002a67-0000-1000-8000-00805f9b34fb";
    public static String ELITE_TRAINER_TRAINER_BRAKE = "347b0010-7635-408b-8918-8ff3949ce592";
    public static String ELITE_TRAINER_OOR_FLAG = "347b0011-7635-408b-8918-8ff3949ce592";
    private static HashMap<String, String> attributes = new HashMap();

    static {
        // Sample Services.
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        attributes.put("00001816-0000-1000-8000-00805f9b34fb", "Cycling Speed and Cadence Service");
        attributes.put("00001800-0000-1000-8000-00805f9b34fb", "Generic Access Service");
        attributes.put("00001801-0000-1000-8000-00805f9b34fb", "Generic Attribute Service");
        attributes.put("0000180f-0000-1000-8000-00805f9b34fb", "Battery Service");
        attributes.put("00001811-0000-1000-8000-00805f9b34fb", "Alert Notification Service");
        attributes.put("00001810-0000-1000-8000-00805f9b34fb", "Blood Pressure Service");
        attributes.put("00001805-0000-1000-8000-00805f9b34fb", "Current Time Service");
        attributes.put("0000180f-0000-1000-8000-00805f9b34fb", "Battery Service");
        attributes.put("00001818-0000-1000-8000-00805f9b34fb", "Cycling Power Service");
        attributes.put("00001809-0000-1000-8000-00805f9b34fb", "Health Thermometer Service");
        attributes.put("0000181d-0000-1000-8000-00805f9b34fb", "Weight Scale Service");
        attributes.put("00001804-0000-1000-8000-00805f9b34fb", "Tx Power Service");
        attributes.put("00001814-0000-1000-8000-00805f9b34fb", "Running Speed and Cadence Service");
        attributes.put("00001819-0000-1000-8000-00805f9b34fb", "Location and Navigation Service");
        attributes.put("347b0001-7635-408b-8918-8ff3949ce592", "Elite Real Trainer Service");

        // Sample Characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
        attributes.put(CSC_MEASUREMENT, "CSC Measurement");
        attributes.put(CSC_FEATURE, "CSC Feature");
        attributes.put(CSC_SENSOR_LOCATION, "CSC Sensor Location");
        attributes.put(ELITE_TRAINER_TRAINER_BRAKE, "Elite Trainer Brake");
        attributes.put(ELITE_TRAINER_OOR_FLAG, "Elite Out of Range Flag");
        attributes.put(CYCLING_POWER_CONTROL_POINT, "Cycling Power Control Point");
        attributes.put(CYCLING_POWER_FEATURE, "Cycling Power Feature");
        attributes.put(CYCLING_POWER_MEASUREMENT, "Cycling Power Measurement");
        attributes.put(CYCLING_POWER_VECTOR, "Cycling Power Vector");

    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
