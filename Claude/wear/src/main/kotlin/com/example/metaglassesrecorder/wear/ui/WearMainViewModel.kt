package com.example.metaglassesrecorder.wear.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.metaglassesrecorder.service.RecordingState
import com.example.metaglassesrecorder.wear.WearApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class WearMainViewModel(app: Application) : AndroidViewModel(app) {

    private val handler = (app as WearApp).watchMessageHandler

    val recordingState: StateFlow<RecordingState> =
        handler.recordingState.stateIn(
            viewModelScope, SharingStarted.Eagerly, RecordingState.IDLE
        )

    val phoneReachable: StateFlow<Boolean> =
        handler.phoneReachable.stateIn(
            viewModelScope, SharingStarted.Eagerly, false
        )

    fun onVideoButtonPressed() = handler.onVideoButtonPressed()
}
