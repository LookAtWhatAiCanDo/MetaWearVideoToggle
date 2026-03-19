package com.example.metawearvideo.mobile.recording

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RecordingRepository {
    private val _status = MutableStateFlow(RecordingStatus())
    val status: StateFlow<RecordingStatus> = _status.asStateFlow()

    fun update(transform: (RecordingStatus) -> RecordingStatus) {
        _status.value = transform(_status.value)
    }
}
