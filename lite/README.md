# MicUp Lite

A separate small Android app (`com.micplugin.lite`) for local VoIP-style microphone monitoring. It installs alongside the full MicUp app.

- Microphone boost: 0 to +36 dB, with smooth gain changes and a peak limiter.
- Output level: 0–100%; session loudness enhancement: 0 to +24 dB when supported by Android.
- Separate input/output device selection, including wired, USB and Bluetooth communication headsets supported by the phone.
- The original MicUp Noise Gate, with a saved enable switch and threshold from −70 to −20 dB, suppresses noise in pauses. No extra Android capture effects are attached.
- Microphone check/monitoring defaults to on, matching the original and is saved independently. Turn it on only when you want to hear your microphone locally.
- Four original mode choices, saved independently: VoIP Compatible, Software Loopback, Shizuku — ADB Level, and Root Mode — System-Wide. The first two retain the original local audio path. Shizuku/Root show the missing-backend reason and cannot start fake transmission; their system virtual-input implementation is absent from the original archive.
- Ten fixed level-only presets. No EQ, pitch shifter, reverb, plugin hosts, Shizuku or plugin libraries. Capture, the gate, microphone gain and limiter use the same native engine as full MicUp.
- The last levels, preset and stable device identities are stored immediately in app preferences. Presets and level resets never change device choices, microphone-check or noise-suppression switches. Missing devices use a temporary automatic route without erasing the saved choice; reconnect restores it when Android accepts that route.
- Audio starts on opening the app after microphone permission, matching the original. The start/stop button remains available. An active service uses the original sticky behavior; there is no boot receiver.
- One quiet notification while the microphone is active, with a stop action.

MicUp Lite 1.0.3 and full MicUp 1.0.15 use the original 1.0.6 stream configuration: VoiceCommunication capture, low latency, exclusive request, float mono, no forced capture session ID, and MEDIA/SPEECH AudioTrack playback. They do not request MODE_IN_COMMUNICATION or insert AEC/NoiseSuppressor. Original device-error recovery and opening-app startup behavior are restored.

On the first upgrade to this baseline, playback is reset once to the original defaults: monitoring on, output level 100%, devices Auto. Microphone/output gain, preset and noise controls remain saved. Later monitoring/device/level edits persist normally. Auto uses the original headset-then-earpiece selection; explicitly selected devices still override it. [Exact baseline comparison](../docs/AUDIO_BASELINE.md).

VoIP playback does **not** provide a virtual microphone to Telegram, WhatsApp or other apps. This app does not raise the phone's hardware volume limit. Gain boosts noise too; the limiter and Android's loudness effect prevent simple numeric overflow but cannot guarantee distortion-free sound at every setting. Headphones help avoid acoustic feedback when monitoring a microphone.

## Build and test

Use JDK 17, Android SDK 34, NDK 26.3.11579264 and CMake 3.22.1. Lite builds only the shared audio engine and Oboe; it does not fetch CLAP/LV2 or compile plugin hosts.

```sh
./gradlew :lite:testDebugUnitTest :lite:assembleRelease
```

APK: `lite/build/outputs/apk/release/lite-release.apk`.

The release variant has debugging disabled and is shrunk with R8 and signed with the development/debug key for distribution here; it is not a production-key release. The complete app remains in `:app`; its regression suite is `./gradlew :app:testDebugUnitTest`.
