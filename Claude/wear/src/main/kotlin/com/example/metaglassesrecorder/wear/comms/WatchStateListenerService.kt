package com.example.metaglassesrecorder.wear.comms

import android.util.Log
import com.example.metaglassesrecorder.wear.RecordingState
import com.example.metaglassesrecorder.wear.WearApp
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receives state-sync messages pushed from the phone's WearMessageListenerService
 * and forwards them to WatchMessageHandler so the Compose UI updates automatically.
 */
class WatchStateListenerService : WearableListenerService() {

    private val handler get() = (application as WearApp).watchMessageHandler

    override fun onMessageReceived(event: MessageEvent) {
        val payload = String(event.data, Charsets.UTF_8)
        Log.i(TAG, "Phone → Watch [${event.path}]: $payload")

        when (event.path) {
            WatchMessageHandler.PATH_STATE_UPDATE -> {
                val state = runCatching { RecordingState.valueOf(payload) }.getOrElse {
                    Log.e(TAG, "Unknown state payload: $payload"); return
                }
                handler.updateState(state)
            }
            else -> Log.w(TAG, "Unhandled path: ${event.path}")
        }
    }

    companion object { private const val TAG = "WatchStateListener" }
}
