package com.example.metawearvideo.mobile.meta

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

data class MetaDevice(val id: String, val displayName: String)

sealed interface MetaConnectionState {
    data object Disconnected : MetaConnectionState
    data object Connecting : MetaConnectionState
    data class Connected(val device: MetaDevice) : MetaConnectionState
    data class Error(val message: String) : MetaConnectionState
}

data class EncodedTrackFormat(
    val mediaFormat: MediaFormat
)

data class EncodedSample(
    val trackType: TrackType,
    val buffer: ByteBuffer,
    val bufferInfo: MediaCodec.BufferInfo
)

enum class TrackType { VIDEO, AUDIO }

interface MetaVideoSession {
    val videoFormat: EncodedTrackFormat
    val audioFormat: EncodedTrackFormat?
    val samples: Flow<EncodedSample>
    suspend fun stop()
}

interface MetaWearablesSdkAdapter {
    val connectionState: Flow<MetaConnectionState>
    suspend fun initialize()
    suspend fun discoverAndConnect(): MetaDevice
    suspend fun startVideoSession(context: Context): MetaVideoSession
}
