# Все версии MicUp и MicUp Lite

**Первоисточник:** [papergray/MicUp v1.0.6](https://github.com/papergray/MicUp/tree/v1.0.6). MIT, Copyright (c) 2026 Gray. Это каталог модифицированных сборок, не официальный проект автора.

[Подробнее об авторстве и зависимостях](../../THIRD_PARTY_NOTICES.md).

| Версия | Описание | APK |
|---|---|---|
| MicUp Lite 1.0.2 | [Изменения](lite-v1.0.2.md) | [Скачать](https://github.com/projectvlad51-maker/MicUp-upgrade/releases/download/lite-v1.0.2/MicUp-Lite-1.0.2.apk) |
| MicUp 1.0.14 | [Изменения](micup-v1.0.14.md) | [Скачать](https://github.com/projectvlad51-maker/MicUp-upgrade/releases/download/micup-v1.0.14/MicUp-1.0.14-original-audio.apk) |
| MicUp Lite 1.0.1 | [Изменения](lite-v1.0.1.md) | [Скачать](https://github.com/projectvlad51-maker/MicUp-upgrade/releases/download/lite-v1.0.1/MicUp-Lite-1.0.1.apk) |
| MicUp 1.0.13 | [Изменения](micup-v1.0.13.md) | [Скачать](https://github.com/projectvlad51-maker/MicUp-upgrade/releases/download/micup-v1.0.13/MicUp-1.0.13-release.apk) |
| MicUp Lite 1.0.0 | [Изменения](lite-v1.0.0.md) | [Скачать](https://github.com/projectvlad51-maker/MicUp-upgrade/raw/f9351cfd681a6c85b1dbbe9690876cf916ab5c90/MicUp-Lite-1.0.0.apk) |
| MicUp 1.0.12 | [Изменения](micup-v1.0.12.md) | [Скачать](https://github.com/projectvlad51-maker/MicUp-upgrade/raw/bb2a86f2da634e56bd69437d031b3777599b18a5/MicUp-1.0.12-volume-controls-debug.apk) |
| MicUp 1.0.11 | [Изменения](micup-v1.0.11.md) | [Скачать](https://github.com/projectvlad51-maker/MicUp-upgrade/raw/47a20e74ef67f43bc12ea4abfdffc51fe6abb1e6/MicUp-1.0.11-shizuku-debug.apk) |
| MicUp 1.0.10 | [Изменения](micup-v1.0.10.md) | [Скачать](https://github.com/projectvlad51-maker/MicUp-upgrade/raw/661be9972318452483eec9cbd28fe383ccce6cbe/MicUp-1.0.10-audio-controls-debug.apk) |
| MicUp 1.0.9 | [Изменения](micup-v1.0.9.md) | [Скачать](https://github.com/projectvlad51-maker/MicUp-upgrade/raw/eb03688e8334ee538babdfa05d0211cbd2f46cc2/MicUp-1.0.9-mic-boost-debug.apk) |
| MicUp 1.0.8 | [Изменения](micup-v1.0.8.md) | [Скачать](https://github.com/projectvlad51-maker/MicUp-upgrade/raw/06d66902329a346a2c419ef6abb1cf10db7cacaf/MicUp-1.0.8-voip-fix-debug.apk) |
| MicUp 1.0.7 | [Изменения](micup-v1.0.7.md) | [Скачать](https://github.com/projectvlad51-maker/MicUp-upgrade/raw/8e825f57964da41ec52da7115126711b0fbfef8e/MicUp-1.0.7-compressor-fix-debug.apk) |
| MicUp 1.0.6 — исходный архив | [Изменения](upstream-archive-v1.0.6.md) | Исходники, без APK |

Полные версии 1.0.7–1.0.14 устанавливаются друг поверх друга. Lite — отдельное приложение и может стоять рядом с полной версией. Все APK подписаны ключом разработческой сборки; SHA-256 указан в описании каждой версии.

Сохранение настроек в Lite рассчитано на закрытие/открытие приложения и перезапуск телефона; очистка данных приложения или удаление приложения стирает настройки средствами Android.

[Установка, Play Защита и ограничения звонков](../INSTALLATION.md).

## Публикация GitHub Releases

Метаданные и контрольные суммы: [release-catalog.json](../release-catalog.json). Команда `python scripts/publish_releases.py` из корня проверяет описания и APK без сетевых изменений. Для реальной публикации после авторизации: `python scripts/publish_releases.py --publish --make-public`.

Workflow `Publish version catalog` запускает публикацию проверенных APK при изменении каталога в `main`, на теге `lite-v*` или вручную из Actions. Если репозиторий закрытый, релизы сначала доступны только его участникам; публичными они становятся после изменения видимости репозитория на Public. Успех или ошибка workflow записывается в ветке `publication-status`.
