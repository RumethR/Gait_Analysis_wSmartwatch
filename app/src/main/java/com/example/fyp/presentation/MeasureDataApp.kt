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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import com.example.fyp.presentation.theme.FYPTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import data.SensorDataManager
import data.SensorServicesRepository
import viewModel.MeasureDataViewModel
import viewModel.MeasureDataViewModelFactory
import viewModel.UiState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MeasureDataApp(sensorServicesRepository: SensorServicesRepository, sensorDataManager: SensorDataManager) {
    FYPTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            timeText = { TimeText() }
        ) {
            val viewModel: MeasureDataViewModel = viewModel(
                factory = MeasureDataViewModelFactory(
                    sensorDataManager = sensorDataManager,
                    sensorServicesRepository = sensorServicesRepository
                )
            )
            val userActivityState by viewModel.userWalking.collectAsState()
            val uiState by viewModel.uiState
            val enrollmentState by viewModel.enrolledData
            val timerState by viewModel.remainingTime
            val PERMISSION = android.Manifest.permission.BODY_SENSORS


            if (uiState == UiState.Supported) {
                val permissionState = rememberPermissionState(
                    permission = PERMISSION,
                    onPermissionResult = { granted ->
                        if (granted) viewModel.toggleEnabled()
                    }
                )
                MeasureDataScreen(
                    walkingStatus = userActivityState,
                    dataEnrolled = enrollmentState,
                    onButtonClick = { viewModel.resetData() },
                    timerStatus = timerState,
                    permissionState = permissionState
                )
            } else if (uiState == UiState.NotSupported) {
                NotSupportedScreen()
            }
        }
    }
}
