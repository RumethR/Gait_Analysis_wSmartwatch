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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.health.services.client.data.DataTypeAvailability
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import data.HealthServicesRepository
import data.MeasureMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch


/* */

class MeasureDataViewModel(
    private val healthServicesRepository: HealthServicesRepository
) : ViewModel() {
    val enabled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val userWalking: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isUserWalking: MutableState<Boolean> = mutableStateOf(false)

    val stepsPerMinute: MutableState<Long> = mutableLongStateOf(0)
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
            val supported = healthServicesRepository.hasStepDetectionCapability()
            uiState.value = if (supported) {
                UiState.Supported
            } else {
                UiState.NotSupported
            }
        }

        viewModelScope.launch {
            healthServicesRepository.detectWalking()
            .collect { measureMessage ->
                val eventTimeStamp = measureMessage.timestamp
                if (stepCount == 0) {
                    // Record the start time on the first step event
                    startTime = eventTimeStamp
                }

                // Increment the step count
                stepCount++

                if (stepCount >= 5 && (eventTimeStamp - startTime) < 7_000_000_000L) {
                    // startCollectingSensorReadings()
                    userWalking.value = true
                    isUserWalking.value = true

                    // Reset the counters
                    stepCount = 0
                    startTime = 0
                } else if ((eventTimeStamp - startTime) > 20_000_000_000L) {
                    //reset to default (user might be standing still)
                    stepCount = 0
                    startTime = 0
                    isUserWalking.value = false
                }
            }
        }

        viewModelScope.launch {
            healthServicesRepository.sensorReadings()
                .takeWhile { userWalking.value }
                .collect { measureMessage ->
                    // Handle sensor readings here
                    when (measureMessage) {
                        is MeasureMessage.MeasureAccelData -> {
                            Log.d("AccData", "Timestamp: ${measureMessage.timestamp}, X: ${measureMessage.accelData[0]},  Y: ${measureMessage.accelData[1]}, Z: ${measureMessage.accelData[2]}")
                        }
                        is MeasureMessage.MeasureGyroData -> {
                            // Handle gyroscope data
                        }
                        is MeasureMessage.MeasureMagData -> {
                            // Handle magnetometer data
                        }
                        // Add more cases as needed
                        else -> {
                            Log.d("MeasureDataViewModel", "Unknown MeasureMessage Returned from Sensor Reading Callback")
                        }
                    }
                }
        }
    }

    private fun startCollectingSensorReadings() {
        // Launch a new coroutine to collect sensor readings using the sensorReadings flow
        viewModelScope.launch {
            healthServicesRepository.sensorReadings()
                .takeWhile { userWalking.value }
                .collect { measureMessage ->
                    // Handle sensor readings here
                    when (measureMessage) {
                        is MeasureMessage.MeasureAccelData -> {
                            // Handle accelerometer data
                        }
                        is MeasureMessage.MeasureGyroData -> {
                            // Handle gyroscope data
                        }
                        is MeasureMessage.MeasureMagData -> {
                            // Handle magnetometer data
                        }
                        // Add more cases as needed
                        else -> {
                            // Because the condition needs to be exhaustive, add a default case
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
                healthServicesRepository = healthServicesRepository
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
