# MetaGlassesRecorder

Press a button on your Wear OS watch → start/stop video recording on Meta AI Glasses.

## Project structure

```
MetaGlassesRecorder/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
│
├── mobile/                                          Phone app
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── kotlin/com/example/metaglassesrecorder/
│           ├── MetaGlassesApp.kt                   Application; wires singletons
│           ├── meta/MetaDeviceManager.kt            SDK interfaces + stub impls
│           ├── service/
│           │   ├── RecordingController.kt           State machine; owns session
│           │   └── RecordingForegroundService.kt    Keeps process alive
│           ├── ui/
│           │   ├── MainActivity.kt
│           │   └── MainViewModel.kt
│           └── wear/
│               └── WearMessageListenerService.kt    Receives watch commands
│
└── wear/                                            Wear OS app
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        └── kotlin/com/example/metaglassesrecorder/wear/
            ├── WearApp.kt
            ├── comms/
            │   ├── WatchMessageHandler.kt           Node discovery, send, retry
            │   └── WatchStateListenerService.kt     Receives phone state pushes
            └── ui/
                ├── WearMainActivity.kt              Compose UI
                └── WearMainViewModel.kt
```

## Architecture

```
WEAR OS
  WearMainActivity (Compose)
    └── WearMainViewModel
          └── WatchMessageHandler ──► MessageClient.sendMessage(/video/command)
          ◄── WatchStateListenerService ◄── /video/state

                    [ Wearable Data Layer ]

PHONE
  WearMessageListenerService ◄── /video/command
    └── RecordingController (StateFlow<RecordingState>) ──► /video/state
          └── MetaDeviceManager ──► Meta SDK (stub → real)
          └── RecordingForegroundService (notification)
```

## State machine

```
IDLE ──[VIDEO_START]──► STARTING ──[SDK callback]──► RECORDING
                                                          │
IDLE ◄──[SDK callback]── STOPPING ◄──[VIDEO_STOP]────────┘
```

Button is disabled during STARTING / STOPPING. Both sides debounce (500 ms phone, 600 ms watch).

## Setup

### 1. Prerequisites
- Android Studio Hedgehog 2023.1.1+, JDK 17, SDK 35
- Physical Wear OS 3+ watch paired to phone (API 26+)

### 2. Integrate the Meta Wearables SDK

`MetaDeviceManager.kt` contains realistic interfaces and `Stub*` implementations
so the project compiles and runs immediately (simulates a connected device after 2 s).

When the real SDK is available:

1. Add to `mobile/build.gradle.kts`:
   ```kotlin
   implementation("com.facebook.wearables:device-access-toolkit:<version>")
   ```
2. Add repo to `settings.gradle.kts`:
   ```kotlin
   maven { url = uri("https://sdk.developer.facebook.com/android/maven") }
   ```
3. In `MetaDeviceManager.kt` replace:
   - `initialize()` → `DeviceAccessToolkit.initialize(context, CLIENT_ID, SECRET)`
   - `startDiscovery()` → `DeviceAccessToolkit.getInstance().startDiscovery(listener)`
   - `StubMetaCameraSession.startRecording()` → `cameraManager.startCapture(file, options, listener)`
   - `StubMetaCameraSession.stopRecording()` → `cameraManager.stopCapture()`

4. Store `CLIENT_ID` / `CLIENT_SECRET` in `local.properties`, never hardcode.

### 3. Run
- Deploy `:mobile` to phone, `:wear` to watch
- Grant all permission prompts on first launch
- Press the button on the watch

## Output files

```
/data/data/com.example.metaglassesrecorder/files/recordings/META_VIDEO_yyyyMMdd_HHmmss.mp4
```
