package com.example.metawearvideo.mobile.meta

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

/**
 * Buildable mock adapter with the same shape your real Meta Device Access Toolkit bridge should expose.
 *
 * Replace internals of this class with the official SDK calls, but keep the adapter contract intact:
 * - initialize(): init toolkit/session manager
 * - discoverAndConnect(): discover/pair/select AI glasses and connect
 * - startVideoSession(): start glasses camera stream and deliver encoded samples + formats
 *
 * Current implementation emits synthetic H264/AVC-like bytes so the app compiles and the recording pipeline runs.
 */
class MockMetaWearablesSdkAdapter(
    private val context: Context
) : MetaWearablesSdkAdapter {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _connectionState = MutableStateFlow<MetaConnectionState>(MetaConnectionState.Disconnected)
    override val connectionState: Flow<MetaConnectionState> = _connectionState.asStateFlow()

    override suspend fun initialize() {
        delay(250)
    }

    override suspend fun discoverAndConnect(): MetaDevice {
        _connectionState.value = MetaConnectionState.Connecting
        delay(500)
        val device = MetaDevice(id = "mock-glasses-1", displayName = "Meta AI Glasses (Mock)")
        _connectionState.value = MetaConnectionState.Connected(device)
        return device
    }

    override suspend fun startVideoSession(context: Context): MetaVideoSession {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1280, 720).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 4_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        return object : MetaVideoSession {
            override val videoFormat = EncodedTrackFormat(format)
            override val audioFormat: EncodedTrackFormat? = null

            override val samples: Flow<EncodedSample> = callbackFlow {
                val job = scope.launch {
                    var ptsUs = 0L
                    repeat(1200) {
                        val payload = ByteArray(2048) { (it % 127).toByte() }
                        val buffer = ByteBuffer.allocateDirect(payload.size)
                        buffer.put(payload)
                        buffer.flip()
                        val info = MediaCodec.BufferInfo().apply {
                            offset = 0
                            size = payload.size
                            presentationTimeUs = ptsUs
                            flags = 0
                        }
                        trySend(EncodedSample(TrackType.VIDEO, buffer, info))
                        ptsUs += 33_333L
                        delay(33)
                    }
                }
                awaitClose { job.cancel() }
            }

            override suspend fun stop() = Unit
        }
    }
}
