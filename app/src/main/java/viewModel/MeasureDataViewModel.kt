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
package viewModel

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.health.services.client.data.DataTypeAvailability
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import data.MeasureMessage
import data.ModelManager
import data.SensorDataManager
import data.SensorServicesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes


/* */

@OptIn(ExperimentalCoroutinesApi::class)
class MeasureDataViewModel(
    private val sensorServicesRepository: SensorServicesRepository,
    private val sensorDataManager: SensorDataManager,
    private val modelManager: ModelManager
) : ViewModel() {
    val enabled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val userWalking: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val remainingTime: MutableState<String> = mutableStateOf("Seconds left: 0")

    val availability: MutableState<DataTypeAvailability> =
        mutableStateOf(DataTypeAvailability.UNKNOWN)

    val uiState: MutableState<UiState> = mutableStateOf(UiState.Startup)

    val isDataEnrolled: MutableState<Boolean> = mutableStateOf(false)
    private var enrolledData: Map<Long, FloatArray> = HashMap()

    // Create maps to store accelerometer and gyroscope data (These need to be reset once inferred with)
    private val accelerometerData: MutableMap<Long, FloatArray> = HashMap()
    private val gyroscopeData: MutableMap<Long, FloatArray> = HashMap()

    // A map to store the preprocessed sensor data for inference
    private var preprocessedSensorData: MutableMap<Long, FloatArray> = HashMap()

    //To be used by the walking detector Coroutine
    private var stepCount = 0
    private var startTime: Long = 0

    init {
        viewModelScope.launch {
            val supported = sensorServicesRepository.hasStepDetectionCapability()
            uiState.value = if (supported) {
                UiState.Supported
            } else {
                UiState.NotSupported
            }
        }

        if (uiState.value == UiState.Supported) {

            viewModelScope.launch {
                Log.d("Data", "Checking if data is enrolled")
                sensorDataManager.fetchSensorDataFromDataStore().collect { sensorData ->
                    Log.d("Data", "$sensorData")
                    if (sensorData.isNotEmpty()) {
                        isDataEnrolled.value = true
                        enrolledData = sensorData
                    } else {
                        isDataEnrolled.value = false
                    }
                }
            }

            // Coroutine that creates a 7 second timer for checking walking pace
            viewModelScope.launch(Dispatchers.Main) {
                while (true) {
                    for (i in 8 downTo 1) {
                        remainingTime.value = "Idle timer ($i)secs left"
                        delay(1000)
                    }

                    if(stepCount>=5){
                        Log.d("Walking", "User is walking.....")
                        userWalking.value = true

                        // 12 Seconds to Collect Acc and Gyro Data
                        Log.d("Data", "Started To collect data")
                        for (j in 13 downTo 1) {
                            delay(1000)
                            remainingTime.value = "Collecting data ($j)secs left"
                        }

                    }
                    Log.d("Data", "Data Collection window closed")
                    remainingTime.value = "Resetting Timer"
                    stepCount = 0
                    delay(2000)
                    userWalking.value = false

                }

            }

            // Coroutine to collect sensor readings using the detectWalking flow
            viewModelScope.launch {
                sensorServicesRepository.detectWalking()
                    .collect { measureMessage ->
                        //Log.d("Message Received ", "Event returned to coroutine")
                        val eventTimeStamp = measureMessage.timestamp
                        if (stepCount == 0) {
                            // Record the start time on the first step event
                            startTime = eventTimeStamp
                        }

                        // Increment the recorded step count
                        stepCount++
                    }
            }

            // Coroutine to collect sensor readings using the sensorReadings flow
            viewModelScope.launch {
                userWalking
                    .filter { it } // Only proceed when userWalking is true
                    .flatMapLatest {
                        sensorServicesRepository.sensorReadings()
                    }
                    .takeWhile {
                        val shouldContinue = !(accelerometerData.size >= 200 && gyroscopeData.size >= 200)
                        if (!shouldContinue) {
                            Log.d("SensorData", "Data Collection Limit Reached, Preprocessing Data...")
                            preprocessedSensorData = sensorDataManager.preprocessSensorData(accelerometerData, gyroscopeData)
                            if (isDataEnrolled.value) {
                                // Run inference on the preprocessed data
                                val enrolledForInference = modelManager.prepareInputsForInference(enrolledData.toMutableMap())
                                val realTimeSensorDataForInference = modelManager.prepareInputsForInference(preprocessedSensorData)
                                modelManager.runInference(enrolledForInference, realTimeSensorDataForInference)
                                delay(1.minutes)
                            } else {
                                sensorDataManager.saveSensorDataToDataStore(preprocessedSensorData)
                            }
                            accelerometerData.clear()
                            gyroscopeData.clear()
                        }
                        shouldContinue
                    }
                    .collect { measureMessage ->
                        // Handle sensor readings here
                        when (measureMessage) {
                            is MeasureMessage.MeasureAccelData -> {
                                Log.d("AccData", "Timestamp: ${measureMessage.timestamp}, X: ${measureMessage.accelData[0]},  Y: ${measureMessage.accelData[1]}, Z: ${measureMessage.accelData[2]}")
                                accelerometerData[measureMessage.timestamp] = floatArrayOf(measureMessage.accelData[0], measureMessage.accelData[1], measureMessage.accelData[2])
                                println("Current AccelData Length: ${accelerometerData.size}")
                                //sensorDataManager.preprocessSensorData()
                            }
                            is MeasureMessage.MeasureGyroData -> {
                                Log.d("GyroData", "Timestamp: ${measureMessage.timestamp}, X: ${measureMessage.accelData[0]},  Y: ${measureMessage.accelData[1]}, Z: ${measureMessage.accelData[2]}")
                                gyroscopeData[measureMessage.timestamp] = floatArrayOf(measureMessage.accelData[0], measureMessage.accelData[1], measureMessage.accelData[2])
                            }
                            else -> {
                                Log.d("MeasureDataViewModel", "Unknown MeasureMessage Returned from Sensor Reading Callback")
                            }
                        }
                    }
            }
        }


    }


    fun toggleEnabled() {
        enabled.value = !enabled.value
        if (!enabled.value) {
            availability.value = DataTypeAvailability.UNKNOWN
        }
    }

    fun resetData() {
        viewModelScope.launch {
            sensorDataManager.resetSensorData()
            Log.d("Data", "Enrolled Data is Deleted")
            isDataEnrolled.value = false
        }
    }

}

class MeasureDataViewModelFactory(
    private val sensorServicesRepository: SensorServicesRepository,
    private val sensorDataManager: SensorDataManager,
    private val modelManager: ModelManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MeasureDataViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MeasureDataViewModel(
                sensorServicesRepository = sensorServicesRepository,
                sensorDataManager = sensorDataManager,
                modelManager = modelManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

sealed class UiState {
    object Startup : UiState()
    object NotSupported : UiState()
    object Supported : UiState()
}
