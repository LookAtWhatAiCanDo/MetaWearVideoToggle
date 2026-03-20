package com.example.metaglassesrecorder.wear.comms

import android.content.Context
import android.util.Log
import com.example.metaglassesrecorder.wear.RecordingState
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class WatchMessageHandler(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _phoneReachable = MutableStateFlow(false)
    val phoneReachable: StateFlow<Boolean> = _phoneReachable.asStateFlow()

    private var lastSendTime = 0L
    private val debounceMs = 600L

    /** Called by the watch UI button. */
    fun onVideoButtonPressed() {
        val now = System.currentTimeMillis()
        if (now - lastSendTime < debounceMs) {
            Log.w(TAG, "Button press debounced")
            return
        }
        lastSendTime = now

        val command = when (_recordingState.value) {
            RecordingState.IDLE      -> CMD_START
            RecordingState.RECORDING -> CMD_STOP
            else -> {
                Log.w(TAG, "Press during transient state ${_recordingState.value} — ignored")
                return
            }
        }
        scope.launch { sendCommandWithRetry(command) }
    }

    /** Called by WatchStateListenerService when the phone pushes a state update. */
    fun updateState(state: RecordingState) {
        Log.i(TAG, "State update from phone: $state")
        _recordingState.value = state
    }

    private suspend fun sendCommandWithRetry(command: String, maxAttempts: Int = 3) {
        repeat(maxAttempts) { attempt ->
            if (trySendCommand(command)) {
                _phoneReachable.value = true
                return
            }
            Log.w(TAG, "Attempt ${attempt + 1}/$maxAttempts failed for $command")
            _phoneReachable.value = false
            if (attempt < maxAttempts - 1) delay(RETRY_DELAY_MS)
        }
        Log.e(TAG, "All $maxAttempts attempts failed — phone unreachable")
    }

    private suspend fun trySendCommand(command: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val nodeId = findPhoneNodeId() ?: return@withContext false
                Wearable.getMessageClient(context)
                    .sendMessage(nodeId, PATH_VIDEO_COMMAND, command.toByteArray(Charsets.UTF_8))
                    .await()
                Log.i(TAG, "Sent '$command' to $nodeId")
                true
            } catch (e: Exception) {
                Log.e(TAG, "sendMessage failed", e)
                false
            }
        }

    private suspend fun findPhoneNodeId(): String? {
        val nodes = try {
            Wearable.getNodeClient(context).connectedNodes.await()
        } catch (e: Exception) {
            Log.e(TAG, "getConnectedNodes failed", e)
            return null
        }
        if (nodes.isEmpty()) { Log.w(TAG, "No connected nodes"); return null }
        // Prefer a nearby (direct BT) node over a cloud-relay node
        return (nodes.firstOrNull { it.isNearby } ?: nodes.first()).also {
            Log.d(TAG, "Using node: ${it.displayName} nearby=${it.isNearby}")
        }.id
    }

    companion object {
        const val PATH_VIDEO_COMMAND = "/video/command"
        const val PATH_STATE_UPDATE  = "/video/state"
        const val CMD_START          = "VIDEO_START"
        const val CMD_STOP           = "VIDEO_STOP"
        private const val RETRY_DELAY_MS = 1_000L
        private const val TAG = "WatchMsgHandler"
    }
}
