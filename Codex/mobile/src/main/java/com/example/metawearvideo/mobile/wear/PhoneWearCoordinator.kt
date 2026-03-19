package com.example.metawearvideo.mobile.wear

import android.content.Context
import android.util.Log
import com.example.metawearvideo.mobile.common.WearDataKeys
import com.example.metawearvideo.mobile.common.WearPaths
import com.example.metawearvideo.mobile.recording.RecordingStatus
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhoneWearCoordinator(
    private val context: Context,
    private val appScope: CoroutineScope,
) {
    suspend fun pushState(status: RecordingStatus) {
        withContext(Dispatchers.IO) {
            val request = PutDataMapRequest.create(WearPaths.RECORDING_STATE).apply {
                dataMap.putString(WearDataKeys.RECORDING_STATE, status.state.name)
                status.lastFilePath?.let { dataMap.putString(WearDataKeys.LAST_FILE_PATH, it) }
                status.errorMessage?.let { dataMap.putString(WearDataKeys.ERROR_MESSAGE, it) }
                dataMap.putLong(WearDataKeys.UPDATED_AT, System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            Tasks.await(Wearable.getDataClient(context).putDataItem(request))
        }
    }

    fun pushStateAsync(status: RecordingStatus) {
        appScope.launch {
            runCatching { pushState(status) }
                .onFailure { Log.e("PhoneWearCoordinator", "Failed to push state", it) }
        }
    }

    suspend fun sendAck(nodeId: String, message: String) = withContext(Dispatchers.IO) {
        Tasks.await(
            Wearable.getMessageClient(context)
                .sendMessage(nodeId, MessagePaths.PHONE_ACK, message.toByteArray())
        )
    }
}

object MessagePaths {
    const val PHONE_ACK = "/phone_ack"
}
