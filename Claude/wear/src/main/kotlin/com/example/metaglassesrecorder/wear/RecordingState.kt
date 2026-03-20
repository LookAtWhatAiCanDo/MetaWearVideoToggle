package com.example.metaglassesrecorder.wear

/**
 * Mirror of the phone-side RecordingState.
 * The phone serialises this enum by name over the Wearable MessageClient;
 * the watch deserialises it here. Keep the values in sync with
 * mobile/.../service/RecordingController.kt.
 */
enum class RecordingState {
    IDLE,
    STARTING,
    RECORDING,
    STOPPING
}
