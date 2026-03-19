package com.example.metawearvideo.wear

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class PhoneStateListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("PhoneStateListener", "Ack from phone: ${messageEvent.path} ${messageEvent.data.toString(Charsets.UTF_8)}")
    }
}
