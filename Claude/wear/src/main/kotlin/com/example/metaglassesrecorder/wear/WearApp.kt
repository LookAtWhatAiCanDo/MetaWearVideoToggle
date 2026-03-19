package com.example.metaglassesrecorder.wear

import android.app.Application
import android.util.Log
import com.example.metaglassesrecorder.wear.comms.WatchMessageHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class WearApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var watchMessageHandler: WatchMessageHandler
        private set

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "WearApp starting")
        watchMessageHandler = WatchMessageHandler(this, applicationScope)
    }

    companion object { private const val TAG = "WearApp" }
}
