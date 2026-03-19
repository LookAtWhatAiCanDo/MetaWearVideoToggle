package com.example.metawearvideo.wear.common

object WearPaths {
    const val VIDEO_COMMAND = "/video_command"
    const val RECORDING_STATE = "/recording_state"
}

object WearCommands {
    const val VIDEO_START = "VIDEO_START"
    const val VIDEO_STOP = "VIDEO_STOP"
    const val STATE_REQUEST = "STATE_REQUEST"
}

object WearDataKeys {
    const val RECORDING_STATE = "recording_state"
    const val LAST_FILE_PATH = "last_file_path"
    const val ERROR_MESSAGE = "error_message"
    const val UPDATED_AT = "updated_at"
}

enum class RecordingState {
    IDLE,
    RECORDING
}

data class RecordingStatus(
    val state: RecordingState = RecordingState.IDLE,
    val lastFilePath: String? = null,
    val errorMessage: String? = null,
)
