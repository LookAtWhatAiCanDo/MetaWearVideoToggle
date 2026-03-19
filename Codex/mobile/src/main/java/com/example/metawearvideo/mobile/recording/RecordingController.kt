package com.example.metawearvideo.mobile.recording

import android.app.Application
import android.util.Log
import com.example.metawearvideo.mobile.meta.MetaDeviceManager
import com.example.metawearvideo.mobile.wear.PhoneWearCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingController(
    private val application: Application,
    private val appScope: CoroutineScope,
    private val repository: RecordingRepository,
    private val metaDeviceManager: MetaDeviceManager,
    private val wearCoordinator: PhoneWearCoordinator,
) {
    val status: StateFlow<RecordingStatus> = repository.status

    private val mutex = Mutex()
    private var activeSessionStopper: (suspend () -> Unit)? = null
    private var recorder: Mp4Recorder? = null

    fun startRecordingAsync() {
        appScope.launch { startRecording() }
    }

    fun stopRecordingAsync() {
        appScope.launch { stopRecording() }
    }

    suspend fun startRecording() {
        mutex.withLock {
            if (status.value.state == RecordingState.RECORDING) return
            try {
                RecordingForegroundService.start(application)
                val session = metaDeviceManager.startVideoSession()
                val outFile = createOutputFile()
                recorder = Mp4Recorder(
                    outputFile = outFile,
                    session = session,
                    scope = appScope,
                    onFailure = { onRecorderFailure(it) }
                )
                activeSessionStopper = { session.stop() }
                repository.update {
                    it.copy(
                        state = RecordingState.RECORDING,
                        lastFilePath = outFile.absolutePath,
                        errorMessage = null
                    )
                }
                wearCoordinator.pushStateAsync(status.value)
                Log.i("RecordingController", "Recording started: ${outFile.absolutePath}")
            } catch (t: Throwable) {
                repository.update {
                    it.copy(
                        state = RecordingState.IDLE,
                        errorMessage = t.message ?: "Unknown start failure"
                    )
                }
                wearCoordinator.pushStateAsync(status.value)
                RecordingForegroundService.stop(application)
                Log.e("RecordingController", "Failed to start recording", t)
            }
        }
    }

    suspend fun stopRecording() {
        mutex.withLock {
            if (status.value.state != RecordingState.RECORDING) return
            try {
                recorder?.stop()
                activeSessionStopper?.invoke()
                Log.i("RecordingController", "Recording stopped")
            } catch (t: Throwable) {
                Log.e("RecordingController", "Failed to stop cleanly", t)
            } finally {
                recorder = null
                activeSessionStopper = null
                RecordingForegroundService.stop(application)
                repository.update { it.copy(state = RecordingState.IDLE) }
                wearCoordinator.pushStateAsync(status.value)
            }
        }
    }

    private fun onRecorderFailure(t: Throwable) {
        appScope.launch {
            mutex.withLock {
                Log.e("RecordingController", "Recorder failure", t)
                repository.update {
                    it.copy(
                        state = RecordingState.IDLE,
                        errorMessage = t.message ?: "Recorder failure"
                    )
                }
                recorder = null
                activeSessionStopper = null
                RecordingForegroundService.stop(application)
                wearCoordinator.pushStateAsync(status.value)
            }
        }
    }

    private fun createOutputFile(): File {
        val dir = File(application.getExternalFilesDir(null), "videos").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "meta_video_$stamp.mp4")
    }
}
