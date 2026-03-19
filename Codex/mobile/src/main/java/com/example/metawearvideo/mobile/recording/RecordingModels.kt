package com.example.metawearvideo.mobile.recording

enum class RecordingState {
    IDLE,
    RECORDING
}

data class RecordingStatus(
    val state: RecordingState = RecordingState.IDLE,
    val lastFilePath: String? = null,
    val errorMessage: String? = null,
)
