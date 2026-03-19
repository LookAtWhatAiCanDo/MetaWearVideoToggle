package com.example.metaglassesrecorder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.metaglassesrecorder.MetaGlassesApp
import com.example.metaglassesrecorder.meta.DeviceConnectionState
import com.example.metaglassesrecorder.service.RecordingState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val metaApp = app as MetaGlassesApp

    val recordingState: StateFlow<RecordingState> =
        metaApp.recordingController.recordingState
            .stateIn(viewModelScope, SharingStarted.Eagerly, RecordingState.IDLE)

    val connectionState: StateFlow<DeviceConnectionState> =
        metaApp.metaDeviceManager.connectionState
            .stateIn(viewModelScope, SharingStarted.Eagerly, DeviceConnectionState.DISCONNECTED)

    fun onStartClicked() = metaApp.recordingController.handleCommand("VIDEO_START")
    fun onStopClicked()  = metaApp.recordingController.handleCommand("VIDEO_STOP")
}
