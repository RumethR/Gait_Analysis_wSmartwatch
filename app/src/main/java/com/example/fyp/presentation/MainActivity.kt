/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.example.fyp.presentation

import android.Manifest.permission.ACTIVITY_RECOGNITION
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val healthServicesRepository = (application as MainApplication).healthServicesRepository

        // Initialize the ActivityResultLauncher for requesting permissions
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                setContent {
                    MeasureDataApp(healthServicesRepository = healthServicesRepository)
                }
            } else {
                // Permission denied, Inform the user
                Log.e("MainActivity", "Permission denied: $ACTIVITY_RECOGNITION")
            }
        }

        // Check if the ACTIVITY_RECOGNITION permission is not granted
        if (ContextCompat.checkSelfPermission(this, ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            // Request the ACTIVITY_RECOGNITION permission
            requestPermissionLauncher.launch(ACTIVITY_RECOGNITION)
        } else {
            setContent {
                MeasureDataApp(healthServicesRepository = healthServicesRepository)
            }
        }
    }
}