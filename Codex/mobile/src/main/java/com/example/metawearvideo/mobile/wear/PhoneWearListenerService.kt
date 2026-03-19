package com.example.metawearvideo.mobile.wear

import com.example.metawearvideo.mobile.MetaWearVideoApplication
import com.example.metawearvideo.mobile.common.WearPaths
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class PhoneWearListenerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WearPaths.VIDEO_COMMAND) return
        val command = messageEvent.data.toString(Charsets.UTF_8)
        val graph = (application as MetaWearVideoApplication).appGraph
        WatchMessageHandler(graph, serviceScope).handleCommand(
            nodeId = messageEvent.sourceNodeId,
            command = command
        )
    }
}
