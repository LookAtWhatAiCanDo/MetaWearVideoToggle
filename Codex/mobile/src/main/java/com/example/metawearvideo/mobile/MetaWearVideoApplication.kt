package com.example.metawearvideo.mobile

import android.app.Application
import com.example.metawearvideo.mobile.meta.MetaDeviceManager
import com.example.metawearvideo.mobile.meta.MockMetaWearablesSdkAdapter
import com.example.metawearvideo.mobile.recording.RecordingController
import com.example.metawearvideo.mobile.recording.RecordingRepository
import com.example.metawearvideo.mobile.wear.PhoneWearCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MetaWearVideoApplication : Application() {

    lateinit var appGraph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        appGraph = AppGraph(this)
    }
}

class AppGraph(application: Application) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository = RecordingRepository()
    private val sdkAdapter = MockMetaWearablesSdkAdapter(application)
    val metaDeviceManager = MetaDeviceManager(application, appScope, sdkAdapter)
    val wearCoordinator = PhoneWearCoordinator(application, appScope)
    val recordingController = RecordingController(
        application = application,
        appScope = appScope,
        repository = repository,
        metaDeviceManager = metaDeviceManager,
        wearCoordinator = wearCoordinator,
    )
}
