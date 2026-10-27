package com.micplugin.lite

import android.media.AudioDeviceInfo
import org.junit.Assert.*
import org.junit.Test

class LiteDevicesTest {
    @Test fun autoPrefersHeadphonesOverSpeakerAndKeepsExplicitChoice() {
        val speaker = LiteDevice("speaker", "Динамик", AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
        val headphones = LiteDevice("headphones", "Наушники", AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
        assertEquals(headphones, LiteDeviceCatalog.automaticOutput(listOf(speaker, headphones)))
        assertEquals(speaker, LiteDeviceCatalog.selected("speaker", listOf(speaker, headphones)))
    }
    @Test fun missingSelectionReturnsOnlyTemporaryFallback() {
        val speaker = LiteDevice("speaker", "Динамик", AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
        assertNull(LiteDeviceCatalog.selected("unplugged", listOf(speaker)))
        assertEquals(speaker, LiteDeviceCatalog.automaticOutput(listOf(speaker)))
        assertNull(LiteDeviceCatalog.automaticOutput(emptyList()))
    }
}
