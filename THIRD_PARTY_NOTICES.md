# Первоисточник и авторство

**Оригинальный проект MicUp:** https://github.com/papergray/MicUp

Исходные файлы MicUp были предоставлены пользователем в архивах `MicUp-1.0.6 (1).zip` и `MicUp-1.0.6.tar.gz`; их содержимое совпадает. В исходном README указан проект `papergray/MicUp`. Все 66 исходных файлов архива сверены по содержимому с тегом `v1.0.6` (`f3a8832f4a114da8a8ebff29dc1479d3b07aa012`), отличий нет. Его README сохранён без изменений в [docs/UPSTREAM_README.md](docs/UPSTREAM_README.md).

Оригинальная лицензия — MIT, **Copyright (c) 2026 Gray**. Полный текст сохранён в [LICENSE](LICENSE). Авторство исходного проекта не присваивается автору этого репозитория. Эти сборки являются модификациями, а не официальными релизами `papergray/MicUp`.

Версии полной сборки и отдельная MicUp Lite описаны в [каталоге версий](docs/releases/README.md). Lite не содержит хостов CLAP/LV2/VST3 или Shizuku. Начиная с Lite 1.0.2 обе версии используют общий нативный AudioEngine (сборка Lite исключает хосты плагинов) и общий JNI/SoftwareLoopback в `shared/audio`.

## Основные зависимости Lite

- AndroidX / Jetpack Compose: https://android.googlesource.com/platform/frameworks/support/ — Apache License 2.0.
- Kotlin: https://github.com/JetBrains/kotlin — Apache License 2.0.
- Oboe 1.8.1: https://github.com/google/oboe/tree/1.8.1 — Apache License 2.0.
- kotlinx.coroutines: https://github.com/Kotlin/kotlinx.coroutines — Apache License 2.0.

Копии MIT и Apache 2.0 и краткие уведомления об авторстве включены в assets APK Lite. Состав полной версии и версии библиотек указаны в Gradle-манифестах и `gradle/libs.versions.toml`.
