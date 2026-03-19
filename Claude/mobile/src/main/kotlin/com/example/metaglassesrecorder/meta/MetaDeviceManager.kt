package com.example.metaglassesrecorder.meta

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// META SDK INTERFACES
// These mirror the Meta Wearables Device Access Toolkit surface.
// Replace the Stub* implementations with real SDK calls when the AAR is added.
// ─────────────────────────────────────────────────────────────────────────────

interface MetaDevice {
    val deviceId: String
    val displayName: String
    val isConnected: Boolean
}

interface MetaDeviceListener {
    fun onDeviceConnected(device: MetaDevice)
    fun onDeviceDisconnected(device: MetaDevice)
    fun onDeviceError(device: MetaDevice?, error: MetaDeviceError)
}

data class MetaDeviceError(val code: Int, val message: String)

interface MetaCameraSession {
    fun startRecording(outputFile: File, listener: MetaRecordingListener)
    fun stopRecording()
}

interface MetaRecordingListener {
    fun onRecordingStarted(outputFile: File)
    fun onRecordingStopped(outputFile: File)
    fun onRecordingError(error: MetaDeviceError)
}

// ─────────────────────────────────────────────────────────────────────────────
// STUB IMPLEMENTATIONS
// ─────────────────────────────────────────────────────────────────────────────

private class StubMetaDevice(
    override val deviceId: String    = "meta-glasses-stub-001",
    override val displayName: String = "Meta Ray-Ban Glasses",
    override var isConnected: Boolean = false
) : MetaDevice

private class StubMetaCameraSession : MetaCameraSession {
    private var thread: Thread? = null

    override fun startRecording(outputFile: File, listener: MetaRecordingListener) {
        thread = Thread {
            try {
                Thread.sleep(300)
                outputFile.writeText("STUB_VIDEO_DATA")
                listener.onRecordingStarted(outputFile)
                Log.d(TAG, "STUB recording started → ${outputFile.absolutePath}")
            } catch (_: InterruptedException) { }
        }.also { it.start() }
    }

    override fun stopRecording() {
        thread?.interrupt()
        thread = null
        Log.d(TAG, "STUB recording stopped")
    }

    companion object { private const val TAG = "StubCameraSession" }
}

// ─────────────────────────────────────────────────────────────────────────────
// DEVICE MANAGER
// ─────────────────────────────────────────────────────────────────────────────

enum class DeviceConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

class MetaDeviceManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _connectionState = MutableStateFlow(DeviceConnectionState.DISCONNECTED)
    val connectionState: StateFlow<DeviceConnectionState> = _connectionState.asStateFlow()

    private var connectedDevice: MetaDevice? = null
    private var cameraSession: MetaCameraSession? = null

    /**
     * REAL SDK: DeviceAccessToolkit.initialize(context, CLIENT_ID, CLIENT_SECRET)
     */
    fun initialize() {
        Log.i(TAG, "Initializing Meta Device Access Toolkit (stub)")
        startDiscovery()
    }

    /**
     * REAL SDK: DeviceAccessToolkit.getInstance().startDiscovery(listener)
     */
    private fun startDiscovery() {
        _connectionState.value = DeviceConnectionState.CONNECTING
        scope.launch(Dispatchers.IO) {
            Thread.sleep(2_000) // simulate BLE scan
            connectedDevice = StubMetaDevice(isConnected = true)
            _connectionState.value = DeviceConnectionState.CONNECTED
            Log.i(TAG, "Device connected: ${connectedDevice?.displayName}")
        }
    }

    fun isConnected(): Boolean =
        connectedDevice?.isConnected == true &&
                _connectionState.value == DeviceConnectionState.CONNECTED

    /**
     * REAL SDK: DeviceAccessToolkit.getInstance().getCameraManager(device).openSession()
     */
    fun openCameraSession(): MetaCameraSession? {
        if (!isConnected()) {
            Log.w(TAG, "openCameraSession: not connected")
            return null
        }
        return StubMetaCameraSession().also { cameraSession = it }
    }

    fun closeCameraSession() { cameraSession = null }

    fun createOutputFile(): File {
        val dir = File(context.filesDir, "recordings").also { it.mkdirs() }
        val ts  = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "META_VIDEO_$ts.mp4")
    }

    companion object { private const val TAG = "MetaDeviceManager" }
}
