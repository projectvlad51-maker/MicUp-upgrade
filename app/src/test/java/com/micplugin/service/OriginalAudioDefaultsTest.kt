package com.micplugin.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OriginalAudioDefaultsTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Test fun fullMigrationRestoresTransportOnceAndPreservesSoundSettings() {
        val p = context.getSharedPreferences("original-full-test", 0)
        p.edit().clear().putBoolean("monitoring_enabled",false).putString("playback_tier","VOIP_STREAM")
            .putInt("output_device_id",37).putFloat("microphone_gain_db",21f).putFloat("speaker_boost_db",9f).commit()
        OriginalAudioDefaults.full(p)
        assertTrue(p.getBoolean("monitoring_enabled",false))
        assertEquals(-1,p.getInt("output_device_id",0)); assertFalse(p.contains("playback_tier"))
        assertEquals(21f,p.getFloat("microphone_gain_db",0f),0f)
        assertEquals(9f,p.getFloat("speaker_boost_db",0f),0f)
        p.edit().putBoolean("monitoring_enabled",false).putInt("output_device_id",42).commit()
        OriginalAudioDefaults.full(p)
        assertFalse(p.getBoolean("monitoring_enabled",true)); assertEquals(42,p.getInt("output_device_id",0))
    }
    @Test fun liteMigrationRestoresTransportOnceWithoutChangingPresetsOrNoise() {
        val p = context.getSharedPreferences("original-lite-test",0)
        p.edit().clear().putBoolean("monitoring",false).putString("input","usb")
            .putString("preset","quiet").putFloat("microphone_db",12f).putBoolean("noise_reduction",false).commit()
        OriginalAudioDefaults.lite(p)
        assertTrue(p.getBoolean("monitoring",false)); assertEquals("auto",p.getString("input",null))
        assertEquals("quiet",p.getString("preset",null)); assertEquals(12f,p.getFloat("microphone_db",0f),0f)
        assertFalse(p.getBoolean("noise_reduction",true))
        p.edit().putBoolean("monitoring",false).putString("input","headset").commit()
        OriginalAudioDefaults.lite(p)
        assertFalse(p.getBoolean("monitoring",true)); assertEquals("headset",p.getString("input",null))
    }
}
