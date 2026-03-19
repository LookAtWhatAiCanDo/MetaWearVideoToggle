package com.example.metaglassesrecorder.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.metaglassesrecorder.databinding.ActivityMainBinding
import com.example.metaglassesrecorder.meta.DeviceConnectionState
import com.example.metaglassesrecorder.service.RecordingState
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val denied = grants.filterValues { !it }.keys
        if (denied.isNotEmpty()) {
            binding.statusText.text = "Permissions denied: ${denied.joinToString()}"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestRequiredPermissions()
        setupButtons()
        observeState()
    }

    private fun setupButtons() {
        binding.btnStart.setOnClickListener { viewModel.onStartClicked() }
        binding.btnStop.setOnClickListener  { viewModel.onStopClicked()  }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.recordingState.collect  { updateRecordingUi(it) } }
                launch { viewModel.connectionState.collect { updateConnectionUi(it) } }
            }
        }
    }

    private fun updateConnectionUi(state: DeviceConnectionState) {
        binding.connectionStatus.text = when (state) {
            DeviceConnectionState.CONNECTED    -> "🟢 Glasses connected"
            DeviceConnectionState.CONNECTING   -> "🟡 Connecting…"
            DeviceConnectionState.DISCONNECTED -> "🔴 Glasses disconnected"
            DeviceConnectionState.ERROR        -> "🔴 Connection error"
        }
    }

    private fun updateRecordingUi(state: RecordingState) {
        when (state) {
            RecordingState.IDLE -> {
                binding.btnStart.isEnabled  = true
                binding.btnStart.visibility = View.VISIBLE
                binding.btnStop.visibility  = View.GONE
                binding.statusText.text     = "Ready"
                binding.recordingIndicator.visibility = View.GONE
            }
            RecordingState.STARTING -> {
                binding.btnStart.isEnabled = false
                binding.btnStop.isEnabled  = false
                binding.statusText.text    = "Starting…"
            }
            RecordingState.RECORDING -> {
                binding.btnStart.visibility = View.GONE
                binding.btnStop.visibility  = View.VISIBLE
                binding.btnStop.isEnabled   = true
                binding.statusText.text     = "● Recording"
                binding.recordingIndicator.visibility = View.VISIBLE
            }
            RecordingState.STOPPING -> {
                binding.btnStop.isEnabled  = false
                binding.statusText.text    = "Stopping…"
                binding.recordingIndicator.visibility = View.GONE
            }
        }
    }

    private fun requestRequiredPermissions() {
        val needed = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            } else {
                add(Manifest.permission.BLUETOOTH)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }
}
