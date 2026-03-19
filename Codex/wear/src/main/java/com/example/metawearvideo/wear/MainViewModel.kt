package com.example.metawearvideo.wear

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.metawearvideo.wear.common.RecordingState
import com.example.metawearvideo.wear.common.RecordingStatus
import com.example.metawearvideo.wear.common.WearCommands
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WearStateRepository(application)

    private val _uiState = MutableStateFlow(RecordingStatus())
    val uiState: StateFlow<RecordingStatus> = _uiState

    private var syncJob: Job? = null
    private var lastTapAt = 0L

    init {
        viewModelScope.launch {
            repository.connect()
            _uiState.value = repository.status
            syncJob = launch {
                while (true) {
                    delay(1000)
                    _uiState.value = repository.status
                }
            }
        }
    }

    fun onTogglePressed() {
        val now = System.currentTimeMillis()
        if (now - lastTapAt < 600) return
        lastTapAt = now

        viewModelScope.launch {
            val nextCommand = when (_uiState.value.state) {
                RecordingState.IDLE -> WearCommands.VIDEO_START
                RecordingState.RECORDING -> WearCommands.VIDEO_STOP
            }
            runCatching {
                repository.sendCommand(nextCommand)
                delay(200)
                repository.requestState()
                delay(200)
                _uiState.value = repository.status
            }.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = it.message)
            }
        }
    }

    override fun onCleared() {
        syncJob?.cancel()
        repository.disconnect()
        super.onCleared()
    }
}
