package com.example.metawearvideo.mobile.meta

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.io.IOException

class MetaDeviceManager(
    private val context: Context,
    appScope: CoroutineScope,
    private val sdkAdapter: MetaWearablesSdkAdapter,
) {
    val connectionState: StateFlow<MetaConnectionState> =
        sdkAdapter.connectionState.stateIn(
            appScope,
            SharingStarted.Eagerly,
            MetaConnectionState.Disconnected
        )

    suspend fun initialize() {
        sdkAdapter.initialize()
    }

    suspend fun ensureConnected(): MetaDevice {
        initialize()
        val current = connectionState.value
        if (current is MetaConnectionState.Connected) return current.device
        return try {
            sdkAdapter.discoverAndConnect()
        } catch (t: Throwable) {
            Log.e("MetaDeviceManager", "Failed to connect device", t)
            throw IOException("Could not connect to Meta glasses: ${t.message}", t)
        }
    }

    suspend fun startVideoSession(): MetaVideoSession {
        ensureConnected()
        return sdkAdapter.startVideoSession(context)
    }
}
