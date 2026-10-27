package com.micplugin.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BoostSettingsTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun reset() { context.getSharedPreferences("micup_106_volume",0).edit().clear().commit() }
    @Test fun originalSoundIsTheDefaultAndNewLevelsPersistIndependently() {
        val settings=BoostSettings(context)
        assertEquals(0f,settings.microphoneDb,0f); assertEquals(0f,settings.outputDb,0f)
        settings.microphone(18f); settings.output(9f)
        assertEquals(18f,BoostSettings(context).microphoneDb,0f)
        assertEquals(9f,BoostSettings(context).outputDb,0f)
        settings.output(0f)
        assertEquals(18f,settings.microphoneDb,0f)
    }
    @Test fun boostControlsDoNotRewriteOriginalSettingsAndRejectInvalidValues() {
        val original=context.getSharedPreferences("micup_prefs",0)
        original.edit().putString("playback_tier","VOIP_STREAM").putInt("output_device_id",42).commit()
        val settings=BoostSettings(context)
        assertEquals(36f,settings.microphone(100f),0f)
        assertEquals(24f,settings.output(100f),0f)
        assertEquals(0f,settings.microphone(Float.NaN),0f)
        assertEquals(0f,settings.output(Float.POSITIVE_INFINITY),0f)
        assertEquals("VOIP_STREAM",original.getString("playback_tier",null))
        assertEquals(42,original.getInt("output_device_id",0))
    }
}
