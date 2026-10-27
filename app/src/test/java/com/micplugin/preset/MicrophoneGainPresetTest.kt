package com.micplugin.preset

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MicrophoneGainPresetTest {
    @Test fun oldPresetDefaultsToNormalMicrophoneLevel() {
        val preset = Json.decodeFromString<Preset>(
            """{"name":"Old preset","effects":{"compMakeup":2.0}}""")
        assertEquals(0f, preset.effects.microphoneGainDb)
        assertEquals(2f, preset.effects.compMakeup)
    }

    @Test fun microphoneBoostSurvivesSavingAndReloadingPreset() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = PresetManager(context)
        manager.save(Preset("Loud microphone", effects = EffectState(microphoneGainDb = 36f)))
        val restored = PresetManager(context)
        restored.loadAll()
        val preset = restored.presets.value.single { it.name == "Loud microphone" }
        assertEquals(36f, preset.effects.microphoneGainDb)
    }
}
