# MicUp Lite

A separate small Android app (`com.micplugin.lite`) for local VoIP-style microphone monitoring. It installs alongside the full MicUp app.

- Microphone boost: 0 to +36 dB, with smooth gain changes and a peak limiter.
- Output level: 0–100%; session loudness enhancement: 0 to +24 dB when supported by Android.
- Separate input/output device selection, including wired, USB and Bluetooth communication headsets supported by the phone.
- The original MicUp Noise Gate, with a saved enable switch and threshold from −70 to −20 dB, suppresses noise in pauses even if the phone does not provide a NoiseSuppressor. System noise suppression is additionally used when available. Acoustic echo cancellation uses the capture session when Android provides it.
- Microphone check/monitoring defaults to off and is saved independently. Turn it on only when you want to hear your microphone locally.
- Ten fixed level-only presets. No EQ, pitch shifter, reverb, plugin hosts, Shizuku or plugin libraries. Capture, the gate, microphone gain and limiter use the same native engine as full MicUp.
- The last levels, preset and stable device identities are stored immediately in app preferences. Presets and level resets never change device choices, microphone-check or noise-suppression switches. Missing devices use a temporary automatic route without erasing the saved choice; reconnect restores it when Android accepts that route.
- Audio starts only by tapping the start button. Settings persist; microphone capture is not automatically restarted on app launch or reboot.
- One quiet notification while the microphone is active, with a stop action.

MicUp Lite 1.0.2 uses the same Oboe voice-communication capture and callback-to-SoftwareLoopback playback path as full MicUp 1.0.14. It no longer blocks start in communication mode or automatically stops when Android reports a recording conflict. Native output stays silent; one queued AudioTrack provides local playback, controlled by the saved microphone-check switch. Android may still restrict concurrent recording. Real calls on Pixel 9 Pro have not been tested in this cloud environment. [Comparison with the original](../docs/AUDIO_BASELINE.md).

VoIP playback does **not** provide a virtual microphone to Telegram, WhatsApp or other apps. This app does not raise the phone's hardware volume limit. Gain boosts noise too; the limiter and Android's loudness effect prevent simple numeric overflow but cannot guarantee distortion-free sound at every setting. Headphones help avoid acoustic feedback when monitoring a microphone.

## Build and test

Use JDK 17, Android SDK 34, NDK 26.3.11579264 and CMake 3.22.1. Lite builds only the shared audio engine and Oboe; it does not fetch CLAP/LV2 or compile plugin hosts.

```sh
./gradlew :lite:testDebugUnitTest :lite:assembleRelease
```

APK: `lite/build/outputs/apk/release/lite-release.apk`.

The release variant has debugging disabled and is shrunk with R8 and signed with the development/debug key for distribution here; it is not a production-key release. The complete app remains in `:app`; its regression suite is `./gradlew :app:testDebugUnitTest`.
