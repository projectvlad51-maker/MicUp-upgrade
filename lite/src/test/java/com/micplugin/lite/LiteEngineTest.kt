package com.micplugin.lite

import android.Manifest
import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.micplugin.audio.NativeAudioDriver
import com.micplugin.service.SpeakerBoostState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = LiteApplication::class)
class LiteEngineTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private class Driver : NativeAudioDriver {
        var running = false
        var starts = 0
        var stops = 0
        var callback: ((FloatArray, Int) -> Unit)? = null
        val params = mutableMapOf<Pair<Int,Int>,Float>()
        override fun start(): Boolean { starts++; running = true; return true }
        override fun stop() { stops++; running = false }
        override fun isRunning() = running
        override fun setParam(effectId: Int, paramId: Int, value: Float) { params[effectId to paramId] = value }
        override fun getLevels() = floatArrayOf(-24f,-18f,0f)
        override fun setMonitorCallback(callback: ((FloatArray, Int) -> Unit)?) { this.callback = callback }
        override fun getInputSessionId() = -1
        override fun getSampleRate() = 48000
        override fun getFramesPerBurst() = 192
        override fun setInputDevice(id: Int) {}
    }
    private class Playback : LitePlayback {
        override val boost = MutableStateFlow(SpeakerBoostState())
        override var running = false
        var settings: LiteSettings? = null
        var deviceId: Int = 0
        var received = floatArrayOf()
        override fun configure(settings: LiteSettings, deviceId: Int) { this.settings = settings; this.deviceId = deviceId }
        override fun start(monitoring: Boolean) { running = true }
        override fun write(samples: FloatArray, count: Int) { received = samples.copyOf(count) }
        override fun stop() { running = false }
    }
    @Before fun reset() {
        context.getSharedPreferences("lite_audio_settings",Context.MODE_PRIVATE).edit().clear().commit()
        shadowOf(context as Application).grantPermissions(Manifest.permission.RECORD_AUDIO)
    }
    @Test fun communicationModeDoesNotRejectCaptureOrStopItAsInPreviousRelease() {
        context.getSystemService(AudioManager::class.java).mode = AudioManager.MODE_IN_COMMUNICATION
        val d = Driver(); val p = Playback()
        val engine = LiteEngine(context,LiteSettingsStore(context),LiteDeviceCatalog(context),d,p)
        try {
            assertTrue(engine.start())
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(150))
            assertTrue(engine.state.value.running)
            assertEquals(1,d.starts); assertEquals(0,d.stops)
            assertNull(engine.state.value.error)
        } finally { engine.stop() }
    }
    @Test fun nativeCallbackUsesSinglePlaybackPathAndStopClearsIt() {
        val d = Driver(); val p = Playback()
        val engine = LiteEngine(context,LiteSettingsStore(context),LiteDeviceCatalog(context),d,p)
        assertTrue(engine.start())
        assertEquals(-1,p.deviceId)
        d.callback!!(floatArrayOf(0.1f,0.2f,0.3f),2)
        assertArrayEquals(floatArrayOf(0.1f,0.2f),p.received,0f)
        engine.stop()
        assertNull(d.callback); assertFalse(p.running); assertFalse(d.running)
    }
    @Test fun allPresetsSetNativeGainWithoutEnablingToneProcessorsOrMonitoring() {
        val d = Driver(); val p = Playback(); val s = LiteSettingsStore(context)
        s.noiseThreshold(-62f); s.monitoring(false)
        val engine = LiteEngine(context,s,LiteDeviceCatalog(context),d,p)
        try {
            assertTrue(engine.start())
            LitePresets.all.forEach { preset ->
                s.preset(preset.id)
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(60))
                assertEquals(preset.microphoneDb,d.params[5 to 0])
                for (key in listOf(1 to 10,2 to 5,3 to 3,4 to 1,6 to 0,6 to 2)) assertEquals(0f,d.params[key])
                assertEquals(-62f,d.params[0 to 0])
                assertFalse(p.settings!!.monitoring)
            }
        } finally { engine.stop() }
    }
    @Test fun savedNoiseToggleChangesOriginalGateImmediatelyWithoutRestartingCapture() {
        val d = Driver(); val p = Playback(); val s = LiteSettingsStore(context)
        val engine = LiteEngine(context,s,LiteDeviceCatalog(context),d,p)
        try {
            assertTrue(engine.start())
            assertEquals(1f,d.params[0 to 3])
            s.noiseReduction(false); s.microphone(21f); s.monitoring(true)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            assertEquals(0f,d.params[0 to 3]); assertEquals(21f,d.params[5 to 0])
            assertTrue(p.settings!!.monitoring)
            assertEquals(1,d.starts); assertEquals(0,d.stops)
        } finally { engine.stop() }
    }
    @Test fun actualNativeFailureReleasesPlaybackInsteadOfLeavingFalseActiveStatus() {
        val d = Driver(); val p = Playback()
        val engine = LiteEngine(context,LiteSettingsStore(context),LiteDeviceCatalog(context),d,p)
        var failures = 0; engine.onFailure = { failures++ }
        assertTrue(engine.start())
        d.running = false
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        assertFalse(engine.state.value.running); assertFalse(p.running)
        assertEquals(1,failures)
        assertTrue(engine.state.value.error.orEmpty().contains("Аудиоустройство"))
        engine.stop()
    }
    @Test fun unimplementedBackendCannotStartCaptureOrPretendToTransmit() {
        val d = Driver(); val p = Playback(); val s = LiteSettingsStore(context)
        s.mode(com.micplugin.service.VirtualMicTier.SHIZUKU_ADB)
        val engine = LiteEngine(context,s,LiteDeviceCatalog(context),d,p)
        assertFalse(engine.start())
        assertEquals(0,d.starts); assertFalse(p.running)
        assertFalse(engine.state.value.running)
        assertTrue(engine.state.value.error.orEmpty().contains("Shizuku"))
    }
}
