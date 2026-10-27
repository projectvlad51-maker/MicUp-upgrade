# MicUp Lite

A separate small Android app (`com.micplugin.lite`) for local VoIP-style microphone monitoring. It installs alongside the full MicUp app.

- Microphone boost: 0 to +36 dB, with smooth gain changes and a peak limiter.
- Output level: 0–100%; session loudness enhancement: 0 to +24 dB when supported by Android.
- Separate input/output device selection, including wired, USB and Bluetooth communication headsets supported by the phone.
- System noise suppression with a saved toggle and an explicit unsupported-device status. Acoustic echo cancellation uses the capture session when Android provides it.
- Microphone check/monitoring defaults to off and is saved independently. Turn it on only when you want to hear your microphone locally.
- Ten fixed level-only presets. No EQ, pitch shifter, reverb, plugin hosts, Shizuku or native libraries.
- The last levels, preset and stable device identities are stored immediately in app preferences. Presets and level resets never change device choices, microphone-check or noise-suppression switches. Missing devices use a temporary automatic route without erasing the saved choice; reconnect restores it when Android accepts that route.
- Audio starts only by tapping the start button. Settings persist; microphone capture is not automatically restarted on app launch or reboot.
- One quiet notification while the microphone is active, with a stop action.

MicUp Lite declines capture when another app already owns communication mode. Android recording-conflict callbacks stop Lite if either recording is silenced, leaving the microphone to the call. It does not automatically restart afterward. Real Discord/Telegram/WhatsApp calls on Pixel 9 Pro have not been tested in this cloud environment.

VoIP playback does **not** provide a virtual microphone to Telegram, WhatsApp or other apps. This app does not raise the phone's hardware volume limit. Gain boosts noise too; the limiter and Android's loudness effect prevent simple numeric overflow but cannot guarantee distortion-free sound at every setting. Headphones help avoid acoustic feedback when monitoring a microphone.

## Build and test

Use JDK 17 and Android SDK 34. No NDK/CMake is needed for Lite.

```sh
./gradlew :lite:testDebugUnitTest :lite:assembleRelease
```

APK: `lite/build/outputs/apk/release/lite-release.apk`.

The release variant has debugging disabled and is shrunk with R8 and signed with the development/debug key for distribution here; it is not a production-key release. The complete app remains in `:app`; its regression suite is `./gradlew :app:testDebugUnitTest`.
