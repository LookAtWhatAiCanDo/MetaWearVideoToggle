package com.example.metaglassesrecorder.service

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.metaglassesrecorder.meta.MetaCameraSession
import com.example.metaglassesrecorder.meta.MetaDeviceError
import com.example.metaglassesrecorder.meta.MetaDeviceManager
import com.example.metaglassesrecorder.meta.MetaRecordingListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class RecordingState { IDLE, STARTING, RECORDING, STOPPING }

class RecordingController(
    private val context: Context,
    private val metaDeviceManager: MetaDeviceManager,
    private val scope: CoroutineScope
) {
    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private var activeSession: MetaCameraSession? = null
    private var currentOutputFile: File? = null

    private var lastCommandTime = 0L
    private val debounceMs = 500L
    private var commandJob: Job? = null

    fun handleCommand(command: String) {
        val now = System.currentTimeMillis()
        if (now - lastCommandTime < debounceMs) {
            Log.w(TAG, "Debounced: $command")
            return
        }
        lastCommandTime = now
        commandJob?.cancel()
        commandJob = scope.launch { processCommand(command) }
    }

    private suspend fun processCommand(command: String) {
        Log.i(TAG, "Command: $command  state=${_recordingState.value}")
        when (command) {
            CMD_START -> startRecording()
            CMD_STOP  -> stopRecording()
            else      -> Log.w(TAG, "Unknown command: $command")
        }
    }

    private suspend fun startRecording() {
        when (_recordingState.value) {
            RecordingState.RECORDING, RecordingState.STARTING -> {
                Log.w(TAG, "Already recording/starting — ignoring START"); return
            }
            RecordingState.STOPPING -> {
                // Wait up to 3 s for IDLE
                repeat(30) {
                    delay(100)
                    if (_recordingState.value == RecordingState.IDLE) return@repeat
                }
                if (_recordingState.value != RecordingState.IDLE) {
                    Log.e(TAG, "Timed out waiting for IDLE"); return
                }
            }
            else -> Unit
        }

        if (!metaDeviceManager.isConnected()) {
            Log.e(TAG, "Not connected — cannot start recording"); return
        }

        _recordingState.value = RecordingState.STARTING
        startForegroundService()

        val session = metaDeviceManager.openCameraSession()
        if (session == null) {
            Log.e(TAG, "Failed to open camera session")
            _recordingState.value = RecordingState.IDLE
            stopForegroundService()
            return
        }

        val outputFile = metaDeviceManager.createOutputFile()
        activeSession     = session
        currentOutputFile = outputFile

        session.startRecording(outputFile, object : MetaRecordingListener {
            override fun onRecordingStarted(outputFile: File) {
                Log.i(TAG, "Recording started → ${outputFile.absolutePath}")
                _recordingState.value = RecordingState.RECORDING
            }
            override fun onRecordingStopped(outputFile: File) {
                Log.i(TAG, "Recording finalised → ${outputFile.absolutePath}")
                cleanup()
            }
            override fun onRecordingError(error: MetaDeviceError) {
                Log.e(TAG, "Recording error [${error.code}]: ${error.message}")
                cleanup()
            }
        })
    }

    private fun stopRecording() {
        when (_recordingState.value) {
            RecordingState.IDLE, RecordingState.STOPPING -> {
                Log.w(TAG, "Not recording — ignoring STOP"); return
            }
            else -> Unit
        }
        _recordingState.value = RecordingState.STOPPING
        val session = activeSession ?: run {
            Log.e(TAG, "No active session")
            cleanup(); return
        }
        scope.launch(Dispatchers.IO) { session.stopRecording() }
    }

    private fun cleanup() {
        _recordingState.value = RecordingState.IDLE
        metaDeviceManager.closeCameraSession()
        activeSession     = null
        currentOutputFile = null
        stopForegroundService()
    }

    private fun startForegroundService() {
        context.startForegroundService(
            Intent(context, RecordingForegroundService::class.java)
                .setAction(RecordingForegroundService.ACTION_START)
        )
    }

    private fun stopForegroundService() {
        context.startService(
            Intent(context, RecordingForegroundService::class.java)
                .setAction(RecordingForegroundService.ACTION_STOP)
        )
    }

    companion object {
        const val CMD_START = "VIDEO_START"
        const val CMD_STOP  = "VIDEO_STOP"
        private const val TAG = "RecordingController"
    }
}
