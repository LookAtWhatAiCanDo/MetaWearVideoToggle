package com.example.metaglassesrecorder.wear

import android.util.Log
import com.example.metaglassesrecorder.MetaGlassesApp
import com.example.metaglassesrecorder.service.RecordingController
import com.example.metaglassesrecorder.service.RecordingState
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WearMessageListenerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val recordingController: RecordingController
        get() = (application as MetaGlassesApp).recordingController

    override fun onCreate() {
        super.onCreate()
        // Push every state change back to all connected watch nodes
        recordingController.recordingState
            .onEach { state -> broadcastStateToWatch(state) }
            .launchIn(serviceScope)
    }

    override fun onMessageReceived(event: MessageEvent) {
        val command = String(event.data, Charsets.UTF_8)
        Log.i(TAG, "Watch → Phone  [${event.path}] $command")

        if (event.path == PATH_VIDEO_COMMAND) {
            recordingController.handleCommand(command)
            // Echo current state immediately to the sender
            serviceScope.launch {
                sendStateToNode(event.sourceNodeId, recordingController.recordingState.value)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun broadcastStateToWatch(state: RecordingState) {
        try {
            val nodes = Wearable.getNodeClient(this).connectedNodes.await()
            nodes.forEach { sendStateToNode(it.id, state) }
        } catch (e: Exception) {
            Log.e(TAG, "broadcastStateToWatch failed", e)
        }
    }

    private suspend fun sendStateToNode(nodeId: String, state: RecordingState) {
        try {
            Wearable.getMessageClient(this)
                .sendMessage(nodeId, PATH_STATE_UPDATE, state.name.toByteArray(Charsets.UTF_8))
                .await()
            Log.d(TAG, "State sync → $nodeId : $state")
        } catch (e: Exception) {
            Log.e(TAG, "sendStateToNode failed", e)
        }
    }

    companion object {
        const val PATH_VIDEO_COMMAND = "/video/command"
        const val PATH_STATE_UPDATE  = "/video/state"
        private const val TAG = "WearMsgListener"
    }
}
