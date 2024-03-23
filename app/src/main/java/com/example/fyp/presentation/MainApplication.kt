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

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import data.SensorDataManager
import data.SensorServicesRepository

private const val SENSOR_DATA_STORE_NAME = "sensor_data"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = SENSOR_DATA_STORE_NAME)

class MainApplication : Application() {
    val dataStoreRepository by lazy { SensorDataManager(this, dataStore) }
    val sensorServicesRepository by lazy { SensorServicesRepository(this) }
}
