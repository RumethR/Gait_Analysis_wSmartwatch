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
package com.example.fyp.presentation

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.health.services.client.data.DataTypeAvailability
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import data.HealthServicesRepository
import data.MeasureMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch


/* */

@OptIn(ExperimentalCoroutinesApi::class)
class MeasureDataViewModel(
    private val sensorServicesRepository: HealthServicesRepository
) : ViewModel() {
    val enabled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val userWalking: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val remainingTime: MutableState<String> = mutableStateOf("Seconds left: 0")

    val availability: MutableState<DataTypeAvailability> =
        mutableStateOf(DataTypeAvailability.UNKNOWN)

    val uiState: MutableState<UiState> = mutableStateOf(UiState.Startup)

    // Create maps to store accelerometer and gyroscope data (These need to be reset once inferred with)
    private val accelerometerData: MutableMap<Long, FloatArray> = HashMap()
    private val gyroscopeData: MutableMap<Long, FloatArray> = HashMap()

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

            // Coroutine that creates a 7 second timer for checking walking pace
            viewModelScope.launch(Dispatchers.Main) {
                while (true) {
                    for (i in 8 downTo 1) {
                        remainingTime.value = "Seconds left: $i"
                        delay(1000)
                    }

                    if(stepCount>=5){
                        Log.d("Walking", "User is walking.....")
                        userWalking.value = true
                        remainingTime.value = "Timer Paused. User is walking..."

                        // 12 Seconds to Collect Acc and Gyro Data
                        for (j in 12 downTo 1) delay(1000)
                    }
                    else{
                        Log.d("Data", "Data Collection window closed")
                        userWalking.value = false
                        remainingTime.value = "Timer finished! Resetting Steps..."
                        stepCount = 0
                        delay(2000)
                    }
                }

                // Check how much time has passed since start time

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
                    .collect { measureMessage ->
                        // Handle sensor readings here
                        when (measureMessage) {
                            is MeasureMessage.MeasureAccelData -> {
                                Log.d("AccData", "Timestamp: ${measureMessage.timestamp}, X: ${measureMessage.accelData[0]},  Y: ${measureMessage.accelData[1]}, Z: ${measureMessage.accelData[2]}")
                            }
                            is MeasureMessage.MeasureGyroData -> {
                                Log.d("GyroData", "Timestamp: ${measureMessage.timestamp}, X: ${measureMessage.accelData[0]},  Y: ${measureMessage.accelData[1]}, Z: ${measureMessage.accelData[2]}")
                                // Handle gyroscope data
                            }
                            // Add more cases as needed
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

}

class MeasureDataViewModelFactory(
    private val healthServicesRepository: HealthServicesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MeasureDataViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MeasureDataViewModel(
                sensorServicesRepository = healthServicesRepository
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
