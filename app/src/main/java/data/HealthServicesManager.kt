/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventCallback
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking

class HealthServicesRepository(context: Context) {
    private var sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var stepDetector: Sensor? = null

    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null

    fun hasStepDetectionCapability(): Boolean {
        // If there is a step detector sensor, then the device probably has an accelerometer and the other necessary sensors as well
        return sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null
    }

    fun detectWalking() = callbackFlow {
        val stepDetectorCallback = object : SensorEventCallback() {
            override fun onSensorChanged(event: SensorEvent) {
                trySendBlocking(MeasureMessage.MeasureStepDetection(true))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Do something here if sensor accuracy changes.
            }
        }

        stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        sensorManager.registerListener(stepDetectorCallback, stepDetector, SensorManager.SENSOR_DELAY_NORMAL)

        awaitClose {
            runBlocking {
                sensorManager.unregisterListener(stepDetectorCallback)
            }
        }
    }

    /**
     * Returns a cold flow. When activated, the flow will register a callback for sensor data
     * and start to emit messages. When the consuming coroutine is cancelled, the measure callback
     * is unregistered.
     *
     * [callbackFlow] is used to bridge between a callback-based API and Kotlin flows.
     */
    fun sensorReadings() = callbackFlow {
        //Get sensor data from accelerometer, gyroscope and magnetometer
        val accelerometerCallback = object : SensorEventCallback() {
            override fun onSensorChanged(event: SensorEvent) {
                trySendBlocking(MeasureMessage.MeasureAccelData(event.values))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Do something here if sensor accuracy changes.
            }
        }

        val gyroscopeCallback = object : SensorEventCallback() {
            override fun onSensorChanged(event: SensorEvent) {
                trySendBlocking(MeasureMessage.MeasureGyroData(event.values))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Do something here if sensor accuracy changes.
            }
        }

        val magnetometerCallback = object : SensorEventCallback() {
            override fun onSensorChanged(event: SensorEvent) {
                trySendBlocking(MeasureMessage.MeasureMagData(event.values))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Do something here if sensor accuracy changes.
            }
        }


        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        // Register sensor callbacks
        sensorManager.registerListener(accelerometerCallback, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(gyroscopeCallback, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(magnetometerCallback, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)

        awaitClose {
            // Unregister sensor callbacks
            sensorManager.unregisterListener(accelerometerCallback)
            sensorManager.unregisterListener(gyroscopeCallback)
            sensorManager.unregisterListener(magnetometerCallback)
        }
    }
}

sealed class MeasureMessage {
    class MeasureAccelData(val accelData: FloatArray) : MeasureMessage() // There will be 3 elements for X, Y and Z
    class MeasureGyroData(val gyroData: FloatArray) : MeasureMessage()
    class MeasureMagData(val magData: FloatArray) : MeasureMessage()
    class MeasureStepDetection(val stepDetected: Boolean) : MeasureMessage() // Returns true if a step is detected
}