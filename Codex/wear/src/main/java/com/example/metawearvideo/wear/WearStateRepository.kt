package com.example.metawearvideo.wear

import android.content.Context
import android.net.Uri
import com.example.metawearvideo.wear.common.RecordingState
import com.example.metawearvideo.wear.common.RecordingStatus
import com.example.metawearvideo.wear.common.WearCommands
import com.example.metawearvideo.wear.common.WearDataKeys
import com.example.metawearvideo.wear.common.WearPaths
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WearStateRepository(
    context: Context
) : DataClient.OnDataChangedListener {

    private val appContext = context.applicationContext
    private val dataClient = Wearable.getDataClient(appContext)
    private val messageClient = Wearable.getMessageClient(appContext)
    private val nodeClient = Wearable.getNodeClient(appContext)

    @Volatile
    var status: RecordingStatus = RecordingStatus()
        private set

    suspend fun connect() = withContext(Dispatchers.IO) {
        dataClient.addListener(this@WearStateRepository)
        pullLatestState()
        requestState()
    }

    fun disconnect() {
        dataClient.removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            val item = event.dataItem
            if (item.uri.path == WearPaths.RECORDING_STATE) {
                val mapItem = com.google.android.gms.wearable.DataMapItem.fromDataItem(item)
                status = RecordingStatus(
                    state = runCatching {
                        RecordingState.valueOf(mapItem.dataMap.getString(WearDataKeys.RECORDING_STATE) ?: RecordingState.IDLE.name)
                    }.getOrDefault(RecordingState.IDLE),
                    lastFilePath = mapItem.dataMap.getString(WearDataKeys.LAST_FILE_PATH),
                    errorMessage = mapItem.dataMap.getString(WearDataKeys.ERROR_MESSAGE),
                )
            }
        }
    }

    suspend fun sendCommand(command: String) = withContext(Dispatchers.IO) {
        val nodes = Tasks.await(nodeClient.connectedNodes)
        require(nodes.isNotEmpty()) { "No connected phone node found" }

        var lastError: Throwable? = null
        repeat(3) { attempt ->
            for (node in nodes) {
                try {
                    Tasks.await(messageClient.sendMessage(node.id, WearPaths.VIDEO_COMMAND, command.toByteArray()))
                    return@withContext
                } catch (t: Throwable) {
                    lastError = t
                }
            }
            kotlinx.coroutines.delay((attempt + 1) * 250L)
        }
        throw lastError ?: IllegalStateException("Command send failed")
    }

    suspend fun requestState() {
        sendCommand(WearCommands.STATE_REQUEST)
    }

    private suspend fun pullLatestState() = withContext(Dispatchers.IO) {
        val uri = Uri.Builder()
            .scheme("wear")
            .path(WearPaths.RECORDING_STATE)
            .build()
        val items = Tasks.await(dataClient.getDataItems(uri, DataClient.FILTER_LITERAL))
        items.use { buffer ->
            if (buffer.count == 0) return@use
            val first = buffer[0]
            val mapItem = com.google.android.gms.wearable.DataMapItem.fromDataItem(first)
            status = RecordingStatus(
                state = runCatching {
                    RecordingState.valueOf(mapItem.dataMap.getString(WearDataKeys.RECORDING_STATE) ?: RecordingState.IDLE.name)
                }.getOrDefault(RecordingState.IDLE),
                lastFilePath = mapItem.dataMap.getString(WearDataKeys.LAST_FILE_PATH),
                errorMessage = mapItem.dataMap.getString(WearDataKeys.ERROR_MESSAGE),
            )
        }
    }
}
