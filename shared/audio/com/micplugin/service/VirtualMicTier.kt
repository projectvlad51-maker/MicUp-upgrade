package com.micplugin.service

enum class VirtualMicTier {
    VOIP_STREAM, MEDIA_PROJECTION, SHIZUKU_ADB, ROOT_MAGISK;

    val displayName: String get() = when (this) {
        VOIP_STREAM      -> "VoIP Compatible"
        MEDIA_PROJECTION -> "Software Loopback"
        SHIZUKU_ADB      -> "Shizuku — ADB Level"
        ROOT_MAGISK      -> "Root Mode — System-Wide"
    }
    val description: String get() = when (this) {
        VOIP_STREAM      -> "Исходная схема MicUp: голосовой захват и локальное воспроизведение."
        MEDIA_PROJECTION -> "Исходная схема MicUp: голосовой захват и локальное воспроизведение."
        SHIZUKU_ADB      -> "Нужны служба Shizuku и системный аудиобэкенд. В исходном архиве аудиобэкенд отсутствует."
        ROOT_MAGISK      -> "Нужны root и системный аудиомодуль. Указанного Root-модуля в исходном архиве нет."
    }
    val badgeColorHex: Long get() = when (this) {
        VOIP_STREAM      -> 0xFF3DFCACL
        MEDIA_PROJECTION -> 0xFF7C5CFCL
        SHIZUKU_ADB      -> 0xFF00B4D8L
        ROOT_MAGISK      -> 0xFFFFD700L
    }
    val unavailableReason: String? get() = when (this) {
        SHIZUKU_ADB -> "Shizuku: подключение службы само по себе не создаёт виртуальный микрофон. Системный аудиобэкенд отсутствует."
        ROOT_MAGISK -> "Root: нужны root-права и системный аудиомодуль. Модуля нет в исходном архиве."
        else -> null
    }
    companion object {
        fun saved(value: String?): VirtualMicTier = values().firstOrNull { it.name == value }
            ?: if (android.os.Build.VERSION.SDK_INT >= 34) MEDIA_PROJECTION else VOIP_STREAM
    }
}
