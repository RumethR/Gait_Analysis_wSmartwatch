/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.example.fyp.presentation

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.fyp.R
import com.example.fyp.presentation.theme.FYPTheme

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var scrollView: ScrollView
    private lateinit var sensorLayout: LinearLayout

    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null

    private lateinit var accelerometerValues: TextView
    private lateinit var gyroscopeValues: TextView
    private lateinit var magnetometerValues: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        scrollView = ScrollView(this)
        sensorLayout = LinearLayout(this)
        sensorLayout.orientation = LinearLayout.VERTICAL

        layout.addView(scrollView)
        scrollView.addView(sensorLayout)

        accelerometerValues = createTextView(sensorLayout, "Accelerometer Values: X=0, Y=0, Z=0")
        gyroscopeValues = createTextView(sensorLayout, "Gyroscope Values: X=0, Y=0, Z=0")
        magnetometerValues = createTextView(sensorLayout, "Magnetometer Values: X=0, Y=0, Z=0")

        setContentView(layout)

        // Initialize Sensor Manager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // Initialize Sensors
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    }

    private fun createTextView(layout: LinearLayout, text: String): TextView {
        val textView = TextView(this)
        textView.text = text
        textView.textSize = 12f
        textView.isSingleLine = false // Allowing multiple lines
        textView.maxLines = 4 // Limiting the max lines
        textView.ellipsize = TextUtils.TruncateAt.END
        layout.addView(textView)
        return textView
    }

    override fun onResume() {
        super.onResume()

        //reset text views
        accelerometerValues.text = "Accelerometer Values: X=0, Y=0, Z=0"
        gyroscopeValues.text = "Gyroscope Values X=0, Y=0, Z=0"
        magnetometerValues.text = "Magnetometer Values X=0, Y=0, Z=0"

        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun updateSensorValues(sensorType: String, sensorValues: String) {
        when(sensorType){
            "Accelerometer" -> accelerometerValues.text = sensorValues
            "Gyroscope" -> gyroscopeValues.text = sensorValues
            "Magnetometer" -> magnetometerValues.text = sensorValues
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val sensorType = event.sensor.type
        val values = event.values
        val x = values[0]
        val y = values[1]
        val z = values[2]

        val sensorName = when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
            Sensor.TYPE_GYROSCOPE -> "Gyroscope"
            Sensor.TYPE_MAGNETIC_FIELD -> "Magnetometer"
            else -> "Unknown Sensor"
        }

        val sensorValues = "$sensorName Values: X=$x, Y=$y, Z=$z"
        Log.d("Data", sensorValues)
        updateSensorValues(sensorName, sensorValues)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do something here if sensor accuracy changes.
    }
}

@Composable
fun WearApp(greetingName: String) {
    FYPTheme {
        /* If you have enough items in your list, use [ScalingLazyColumn] which is an optimized
         * version of LazyColumn for wear devices with some added features. For more information,
         * see d.android.com/wear/compose.
         */
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                text = ""
            )
            //Greeting(greetingName = greetingName)
        }
    }
}

@Composable
fun Greeting(greetingName: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = stringResource(R.string.hello_world, greetingName)
    )
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("Preview Android")
}