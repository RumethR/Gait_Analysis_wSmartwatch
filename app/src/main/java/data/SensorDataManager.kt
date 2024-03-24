package data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.File

class SensorDataManager(context: Context, private val dataStore: DataStore<Preferences>) {

    /* The key will be the timestamp and the values will be both the gyro and acc values combined */

    private suspend fun saveSensorDataToDataStore(sensorData: MutableMap<Long, FloatArray>) {
        dataStore.edit { preferences ->
            sensorData.forEach { (timestamp, values) ->
                val dataString = values.joinToString(separator = ",") { it.toString() }
                preferences[stringPreferencesKey(timestamp.toString())] = dataString
            }
        }
    }

    fun fetchSensorDataFromDataStore(): Flow<Map<Long, FloatArray>> = dataStore.data.map { preferences ->
        val sensorData = mutableMapOf<Long, FloatArray>()
        preferences.asMap().forEach { (key, value) ->
            val timestampString = key.name
            val timestamp = timestampString.toLong()
            val dataStringValues = value.toString().split(",")
            val dataArray = FloatArray(dataStringValues.size) { i -> dataStringValues[i].toFloat() }
            sensorData[timestamp] = dataArray
        }
        sensorData
    }

    suspend fun resetSensorData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    fun preprocessSensorData(accelerometerData: MutableMap<Long, FloatArray>, gyroscopeData: MutableMap<Long, FloatArray>) {
        // Perform a first-order low-pass filter on the accelerometer and gyroscope data
        val filteredAccData = lowPassFilter(accelerometerData)
        val filteredGyroData = lowPassFilter(gyroscopeData)

        //exportToTxt(accelerometerData, gyroscopeData) // REQUIRES FILE STORAGE PERMISSIONS

        val filteredCombinedSensorData = combineSensorData(filteredGyroData, filteredAccData)

        runBlocking {
            println("Saving sensor data to data store")
            saveSensorDataToDataStore(filteredCombinedSensorData)
        }
    }

    private fun combineSensorData(gyroData: MutableMap<Long, FloatArray>, accData: MutableMap<Long, FloatArray>): MutableMap<Long, FloatArray> {
        // Check for common timestamps in both the gyro and acc data
        val commonTimestamps = gyroData.keys.intersect(accData.keys)

        println("Number of Common timestamps: ${commonTimestamps.size}")

        // Create a new map to store the combined data
        val combinedData = mutableMapOf<Long, FloatArray>()

        // Iterate over the common timestamps
        for (timestamp in commonTimestamps) {
            // Combine the gyro and acc data for the timestamp
            val combinedValues = FloatArray(6)
            combinedValues[0] = accData[timestamp]!![0]
            combinedValues[1] = accData[timestamp]!![1]
            combinedValues[2] = accData[timestamp]!![2]
            combinedValues[3] = gyroData[timestamp]!![0]
            combinedValues[4] = gyroData[timestamp]!![1]
            combinedValues[5] = gyroData[timestamp]!![2]

            // Store the combined values in the output map
            combinedData[timestamp] = combinedValues

            //Store the data in SensorData Objects
            //val sensorData = SensorData(timestamp, accData[timestamp]!![0], accData[timestamp]!![1], accData[timestamp]!![2], gyroData[timestamp]!![0], gyroData[timestamp]!![1], gyroData[timestamp]!![2])
        }

        return combinedData
    }

    // Map the prefs to the SensorData object
//    private fun mapSensorData(preferences: Preferences): SensorData {
//        val sensorData = preferences['sensor_data'] ?: ""
//        return SensorData(sensorData)
//    }

    private fun lowPassFilter(accelerometerData: MutableMap<Long, FloatArray>): MutableMap<Long, FloatArray> {
        val filteredData = mutableMapOf<Long, FloatArray>()

        // SAME ALPHA VALUE FOR ALL AXES AND SENSORS AS PER COLAB NOTEBOOK
        val alpha = 0.854f

        var lastTimestamp = accelerometerData.keys.first()
        // Iterate over each entry in the input map
        for ((timestamp, values) in accelerometerData) {
            val filteredValues = FloatArray(3) // Array to store filtered values for x, y, and z axes

            // If it's the first data point, simply copy it to the filtered data
            if (filteredData.isEmpty()) {
                filteredValues[0] = values[0]
                filteredValues[1] = values[1]
                filteredValues[2] = values[2]
            } else {
                // Apply the low-pass filter
                filteredValues[0] = alpha * values[0] + (1 - alpha) * filteredData[lastTimestamp]!![0]
                filteredValues[1] = alpha * values[1] + (1 - alpha) * filteredData[lastTimestamp]!![1]
                filteredValues[2] = alpha * values[2] + (1 - alpha) * filteredData[lastTimestamp]!![2]

                // Update the last timestamp
                lastTimestamp = timestamp
            }

            // Store the filtered values in the output map
            filteredData[timestamp] = filteredValues
        }

        return filteredData
    }

    private fun exportToTxt(accelerometerData: MutableMap<Long, FloatArray>, gyroscopeData: MutableMap<Long, FloatArray>) {
        //Write the values in each map to a text file
        val file = File("sensor_data_gyro.txt")

        file.printWriter().use { out ->
            out.println("Timestamp, GyroX, GyroY, GyroZ, AccX, AccY, AccZ")
            for ((timestamp, values) in gyroscopeData) {
                out.println("$timestamp, ${values[0]}, ${values[1]}, ${values[2]}")
            }
        }

        val file2 = File("sensor_data_acc.txt")

        file2.printWriter().use { out ->
            out.println("Timestamp, GyroX, GyroY, GyroZ, AccX, AccY, AccZ")
            for ((timestamp, values) in accelerometerData) {
                out.println("$timestamp, ${values[0]}, ${values[1]}, ${values[2]}")
            }
        }

        Log.d("SensorDataManager", "Data exported to text files successfully")
    }

}