package com.example.metawearvideo.mobile.recording

import android.media.MediaCodec
import android.media.MediaMuxer
import com.example.metawearvideo.mobile.meta.EncodedSample
import com.example.metawearvideo.mobile.meta.MetaVideoSession
import com.example.metawearvideo.mobile.meta.TrackType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import java.io.File

class Mp4Recorder(
    private val outputFile: File,
    session: MetaVideoSession,
    scope: CoroutineScope,
    private val onFailure: (Throwable) -> Unit,
) {
    private val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    private var videoTrack = -1
    private var audioTrack = -1
    private var started = false

    private val job: Job

    init {
        videoTrack = muxer.addTrack(session.videoFormat.mediaFormat)
        session.audioFormat?.let { audioTrack = muxer.addTrack(it.mediaFormat) }
        muxer.start()
        started = true

        job = scope.launch {
            session.samples
                .catch { onFailure(it) }
                .onCompletion { safelyStopMuxer() }
                .collect { writeSample(it) }
        }
    }

    private fun writeSample(sample: EncodedSample) {
        val trackIndex = when (sample.trackType) {
            TrackType.VIDEO -> videoTrack
            TrackType.AUDIO -> audioTrack
        }
        if (trackIndex < 0 || !started) return
        muxer.writeSampleData(trackIndex, sample.buffer, copyInfo(sample.bufferInfo))
    }

    private fun copyInfo(info: MediaCodec.BufferInfo): MediaCodec.BufferInfo =
        MediaCodec.BufferInfo().also {
            it.set(info.offset, info.size, info.presentationTimeUs, info.flags)
        }

    suspend fun stop() {
        job.cancel()
        safelyStopMuxer()
    }

    private fun safelyStopMuxer() {
        if (!started) return
        started = false
        runCatching { muxer.stop() }
        runCatching { muxer.release() }
    }
}
