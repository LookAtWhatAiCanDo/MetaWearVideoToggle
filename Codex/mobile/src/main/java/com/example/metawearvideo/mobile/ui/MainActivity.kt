package com.example.metawearvideo.mobile.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.metawearvideo.mobile.MetaWearVideoApplication
import com.example.metawearvideo.mobile.recording.RecordingState
import com.example.metawearvideo.mobile.wear.PhoneWearCoordinator

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestRuntimePermissions()
        val graph = (application as MetaWearVideoApplication).appGraph
        graph.wearCoordinator.pushStateAsync(graph.recordingController.status.value)

        setContent {
            val status by graph.recordingController.status.collectAsState()
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Meta AI Glasses Video Controller", style = MaterialTheme.typography.headlineSmall)
                    Text("State: ${status.state}")
                    status.lastFilePath?.let { Text("Last file: $it") }
                    status.errorMessage?.let { Text("Error: $it") }

                    Button(
                        onClick = {
                            if (status.state == RecordingState.IDLE) {
                                graph.recordingController.startRecordingAsync()
                            } else {
                                graph.recordingController.stopRecordingAsync()
                            }
                        }
                    ) {
                        Text(if (status.state == RecordingState.IDLE) "Start Video" else "Stop Video")
                    }
                }
            }
        }
    }

    private fun requestRuntimePermissions() {
        val permissions = buildList {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
        permissionLauncher.launch(permissions)
    }
}
