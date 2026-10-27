package com.micplugin.lite

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = LiteApplication::class)
class LiteSettingsTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun reset() { context.getSharedPreferences("lite_audio_settings", Context.MODE_PRIVATE).edit().clear().commit() }

    @Test fun exactlyTenDistinctPresetsWithLevelParametersOnly() {
        assertEquals(10, LitePresets.all.size)
        assertEquals(10, LitePresets.all.map { it.id }.distinct().size)
        LitePresets.all.forEach { assertTrue(it.microphoneDb in 0f..36f); assertTrue(it.outputDb in 0f..24f); assertTrue(it.volume in 0f..1f) }
    }

    @Test fun manualLevelsAndBothDevicesSurviveStoreRecreation() {
        val store = LiteSettingsStore(context)
        store.microphone(17f); store.output(9f); store.volume(0.72f)
        store.input("usb-mic"); store.outputDevice("headphones")
        assertNull(store.state.value.presetId)
        assertEquals(store.state.value, LiteSettingsStore(context).state.value)
    }

    @Test fun allPresetsKeepDeviceChoicesAndRestoreSelectedPreset() {
        val store = LiteSettingsStore(context)
        store.input("saved-mic"); store.outputDevice("saved-headphones")
        LitePresets.all.forEach { preset ->
            store.preset(preset.id)
            val restored = LiteSettingsStore(context).state.value
            assertEquals(preset.id, restored.presetId)
            assertEquals(preset.microphoneDb, restored.microphoneDb)
            assertEquals(preset.outputDb, restored.outputDb)
            assertEquals(preset.volume, restored.volume)
            assertEquals("saved-mic", restored.inputKey)
            assertEquals("saved-headphones", restored.outputKey)
        }
    }

    @Test fun absentDeviceDoesNotEraseSavedSelectionAndResetTouchesOnlyLevels() {
        val store = LiteSettingsStore(context)
        store.outputDevice("temporarily-disconnected")
        assertNull(LiteDeviceCatalog.selected(store.state.value.outputKey, emptyList()))
        store.preset("natural")
        assertEquals("temporarily-disconnected", LiteSettingsStore(context).state.value.outputKey)
    }

    @Test fun invalidGainValuesAreSanitizedInsteadOfSavedAsNaN() {
        val store = LiteSettingsStore(context)
        store.microphone(100f); store.output(-1f); store.volume(Float.NaN)
        assertEquals(36f, store.state.value.microphoneDb)
        assertEquals(0f, store.state.value.outputDb)
        assertEquals(1f, store.state.value.volume)
        assertEquals(store.state.value, LiteSettingsStore(context).state.value)
    }
}
