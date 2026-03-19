package com.example.metawearvideo.mobile.wear

import android.util.Log
import com.example.metawearvideo.mobile.AppGraph
import com.example.metawearvideo.mobile.common.WearCommands
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class WatchMessageHandler(
    private val appGraph: AppGraph,
    private val scope: CoroutineScope,
) {
    fun handleCommand(nodeId: String, command: String) {
        scope.launch {
            when (command) {
                WearCommands.VIDEO_START -> appGraph.recordingController.startRecording()
                WearCommands.VIDEO_STOP -> appGraph.recordingController.stopRecording()
                WearCommands.STATE_REQUEST -> appGraph.wearCoordinator.pushState(appGraph.recordingController.status.value)
                else -> Log.w("WatchMessageHandler", "Unknown command: $command")
            }
            runCatching {
                appGraph.wearCoordinator.sendAck(
                    nodeId = nodeId,
                    message = appGraph.recordingController.status.value.state.name
                )
                appGraph.wearCoordinator.pushState(appGraph.recordingController.status.value)
            }.onFailure {
                Log.e("WatchMessageHandler", "Failed to ack watch command", it)
            }
        }
    }
}
