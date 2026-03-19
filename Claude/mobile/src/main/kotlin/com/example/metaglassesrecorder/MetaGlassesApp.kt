package com.example.metaglassesrecorder

import android.app.Application
import android.util.Log
import com.example.metaglassesrecorder.meta.MetaDeviceManager
import com.example.metaglassesrecorder.service.RecordingController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MetaGlassesApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var metaDeviceManager: MetaDeviceManager
        private set

    lateinit var recordingController: RecordingController
        private set

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Application starting")
        metaDeviceManager   = MetaDeviceManager(this, applicationScope)
        recordingController = RecordingController(this, metaDeviceManager, applicationScope)
        metaDeviceManager.initialize()
    }

    companion object {
        private const val TAG = "MetaGlassesApp"
    }
}
