<div align="center">

# 🎙️ MicUp и MicUp Lite

**Модифицированные сборки MicUp и отдельная лёгкая версия для Android**

[Все версии и APK](docs/releases/README.md) · [MicUp Lite](lite/README.md) · [Первоисточник: papergray/MicUp](https://github.com/papergray/MicUp) · [Авторство и MIT](THIRD_PARTY_NOTICES.md)

</div>

---

## What is MicUp?

MicUp is a real-time microphone processing app for Android. It captures your mic input, runs it through a DSP effects chain, and plays the processed audio for local monitoring. You can select media playback or Android communication playback. This app does not currently provide a virtual microphone input to other apps such as Discord, Teams, Zoom, or WhatsApp.

No PC required. No monthly subscription.

---

## Features

-  **Playback loudness boost** — 0 to +24 dB on the MicUp playback session using Android LoudnessEnhancer, with explicit unsupported-device status
-  **Audio dashboard** — fixed tabs for volume, effects, and plugins; separate MicUp, media, and call volume controls
-  **Voice filters** — high-pass cut (20–300 Hz) and low-pass cut (3–20 kHz), with preset support
-  **Mic Boost** — independent 0 to +36 dB boost, smooth level changes, reset button, and preset support
-  **Built-in DSP chain** — Noise Gate, 10-band EQ, Compressor, Reverb, Pitch Shifter
-  **Plugin support** — Load LV2, CLAP, and VST3 native plugins (`.so`, `.clap`, `.lv2`)
-  **Open plugin files** — Tap a plugin file in your file manager to import it directly
-  **Noise suppression and echo cancellation** — system effects on the capture session when supported, with a saved noise-suppression switch
-  **Microphone check** — local monitoring defaults to off; the switch is saved independently of presets
-  **Call priority** — decline capture during an existing call and release it on a detected recording conflict
-  **Shizuku connection** — a pinned row on every dashboard tab, direct manager launch, permission request, denied-permission settings action, and live reconnect status
-  **Selectable playback modes** — Local Monitor or VoIP Playback, with Shizuku access managed separately
-  **Live VU meters** — Input, output, and gain reduction metering
-  **Preset system** — Save and load your effect configurations
-  **Quiet foreground status** — one silent service notification, no repeated alert for unchanged status, removed when processing stops
-  **Built-in crash reporter** — Crash logs auto-shared for easy bug reporting

---

## Download

Download the modified full or Lite APK from the [version catalog](docs/releases/README.md). The [original project](https://github.com/papergray/MicUp) is maintained separately.

No Play Store. No sign-in. Just install and go.

> **Enable "Install from unknown sources"** in Android Settings → Apps → Special app access before installing.

---

## How to Use

1. Install the APK
2. Grant microphone permission when prompted
3. Tap the **power button** to grant microphone permission and start audio processing. Opening the app does not start audio or open storage settings automatically
4. Adjust effects with the sliders and knobs. The voice-processing switch is on when effects are active; playback has its own switch and volume/boost controls
5. In Settings, select **Local Monitor** or **VoIP Playback**, then use the power button to start audio. These modes control playback; they do not replace another app’s microphone input

### Loading Plugins

- Open your file manager, navigate to your plugin `.so` / `.clap` / `.lv2` file
- Tap it → select **MicUp** → plugin is imported and added automatically
- Or go to **Settings → Manage Plugin Paths** to add a folder to scan

### Shizuku (optional, no root needed)

MicUp can detect the Shizuku service and request permission. This connection does not create a system virtual microphone. To connect:

1. Install [Shizuku](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) from Play Store
2. Start Shizuku via wireless debugging (Android 11+)
3. Open MicUp → Settings → Shizuku → tap **Grant**. Use the connection check button if needed; the app also detects a service started after MicUp.

Shizuku and root access alone do not install a virtual microphone. The previous shell/ALSA routing placeholders are not presented as working audio modes.

---

## Building from Source

**Requirements:** Android Studio, NDK 26, CMake 3.22, JDK 17

```bash
git clone https://github.com/projectvlad51-maker/MicUp-upgrade.git
cd MicUp-upgrade
RELEASE_TAG=v1.0.13 ./gradlew :app:assembleRelease :lite:assembleRelease
```

APKs are at `app/build/outputs/apk/release/app-release.apk` and `lite/build/outputs/apk/release/lite-release.apk`. Distributed APKs retain the development signing certificate for updates and have debugging disabled. [Installation and Play Protect](docs/INSTALLATION.md).

Run the Android regression tests with `./gradlew testDebugUnitTest`. The native microphone boost tests can also run on a development machine without an Android device:

```bash
g++ -std=c++17 -O3 -ffast-math -Iapp/src/main/cpp app/src/test/cpp/MicrophoneGainTest.cpp -o /tmp/micup-gain-test
/tmp/micup-gain-test
g++ -std=c++17 -O3 -ffast-math -Iapp/src/main/cpp app/src/test/cpp/VoiceFiltersTest.cpp -o /tmp/micup-filter-test
/tmp/micup-filter-test
```

---

## Supported Formats

| Format | Extension | Notes |
|--------|-----------|-------|
| LV2 | `.lv2` `.so` | Requires `liblilv` |
| CLAP | `.clap` `.so` | CLAP 1.2.1 |
| VST3 | `.so` | Experimental |
| APK Plugin | installed app | Via AIDL interface |

---

## Requirements

- Android 8.0+ (API 26)
- ARM64, ARMv7, or x86_64
- Microphone permission
- For Shizuku: start the external Shizuku service first (wireless debugging requires Android 11+)

---

## License

MIT License — see [LICENSE](LICENSE)

---

<div align="center">
Made for Android · Built with Oboe, C++17, Jetpack Compose
</div>

## MicUp Lite

A separate minimal app is available in [`lite/`](lite/README.md): VoIP playback, microphone/output gain, device selection, persistent settings, and ten level-only presets without tone-changing effects. It can be installed alongside the full version. Build with `./gradlew :lite:assembleRelease`.
