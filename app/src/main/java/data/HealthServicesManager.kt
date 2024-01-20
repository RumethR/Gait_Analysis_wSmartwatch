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

import android.content.ContentValues.TAG
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventCallback
import android.hardware.SensorManager
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.SampleDataPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.runBlocking

/**
 * Entry point for [HealthServicesClient] APIs. This also provides suspend functions around
 * those APIs to enable use in coroutines.
 */
class HealthServicesRepository(context: Context) {
    private val healthServicesClient = HealthServices.getClient(context)
    private val measureClient = healthServicesClient.measureClient
    private var sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null

    suspend fun hasStepDetectionCapability(): Boolean {
        val capabilities = measureClient.getCapabilitiesAsync().await()
        return (DataType.STEPS_PER_MINUTE in capabilities.supportedDataTypesMeasure)
    }

    /**
     * Returns a cold flow. When activated, the flow will register a callback for heart rate data
     * and start to emit messages. When the consuming coroutine is cancelled, the measure callback
     * is unregistered.
     *
     * [callbackFlow] is used to bridge between a callback-based API and Kotlin flows.
     */
    fun walkingDetector() = callbackFlow {
        val callback = object : MeasureCallback {
            override fun onAvailabilityChanged(
                dataType: DeltaDataType<*, *>,
                availability: Availability
            ) {
                // Only send back DataTypeAvailability (not LocationAvailability)
                if (availability is DataTypeAvailability) {
                    trySendBlocking(MeasureMessage.MeasureAvailability(availability))
                }
            }

            override fun onDataReceived(data: DataPointContainer) {
                val heartRateBpm = data.getData(DataType.STEPS_PER_MINUTE)
                trySendBlocking(MeasureMessage.MeasureData(heartRateBpm))
            }
        }

        Log.d(TAG, "Registering for steps per minute data")
        measureClient.registerMeasureCallback(DataType.STEPS_PER_MINUTE, callback)

        awaitClose {
            Log.d(TAG, "Unregistering for steps per minute data")
            runBlocking {
                measureClient.unregisterMeasureCallbackAsync(DataType.STEPS_PER_MINUTE, callback)
                    .await()
            }
        }
    }

    /**
     * Returns a cold flow. When activated, the flow will register a callback for heart rate data
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
    class MeasureAvailability(val availability: DataTypeAvailability) : MeasureMessage()
    class MeasureData(val data: List<SampleDataPoint<Long>>) : MeasureMessage()
    class MeasureAccelData(val accelData: FloatArray) : MeasureMessage() // There will be 3 elements for X, Y and Z
    class MeasureGyroData(val gyroData: FloatArray) : MeasureMessage()
    class MeasureMagData(val magData: FloatArray) : MeasureMessage()
}