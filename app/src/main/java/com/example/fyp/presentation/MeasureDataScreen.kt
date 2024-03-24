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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.fyp.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MeasureDataScreen(
    walkingStatus: Boolean,
    timerStatus: String,
    dataEnrolled: Boolean,
    onButtonClick: () -> Unit,
    permissionState: PermissionState
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Is user walking: $walkingStatus",
            style = MaterialTheme.typography.body1,
            fontSize = 13.sp
        )

        Text(
            text = "Authentication Status: N/A",
            style = MaterialTheme.typography.body1,
            fontSize = 14.sp
        )

        Text(
            modifier = Modifier.padding(bottom = 10.dp),
            text = timerStatus,
            style = MaterialTheme.typography.caption1,
            fontSize = 10.sp
        )

        if (dataEnrolled){
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .padding(vertical = 10.dp)
                            .height(30.dp),
                        onClick = { onButtonClick() }
            ) {
                Text(
                    fontSize = 12.sp,
                    text = stringResource(R.string.reset)
                )
            }
        }

        Text(
            text = "Enrollment status: $dataEnrolled",
            style = MaterialTheme.typography.caption1,
            fontSize = 8.sp
        )
    }
}
