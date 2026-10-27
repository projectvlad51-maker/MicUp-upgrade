package com.micplugin.lite

import android.content.Context
import android.media.AudioDeviceInfo
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.micplugin.service.SpeakerBoostState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = LiteApplication::class, qualifiers = "w320dp-h640dp")
class LiteScreenTest {
    @get:Rule val compose = createComposeRule()
    @Before fun resetSettings() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("lite_audio_settings", Context.MODE_PRIVATE).edit().clear().commit()
    }
    private fun render(store: LiteSettingsStore, devices: State<LiteDevices> = mutableStateOf(LiteDevices()), onToggle: () -> Unit = {}) {
        compose.setContent {
            val settings by store.state.collectAsState()
            LiteScreen(settings, LiteRunState(), devices.value, SpeakerBoostState(), onToggle,
                store::microphone, store::output, store::volume, store::input, store::outputDevice, store::preset)
        }
    }
    private fun store() = LiteSettingsStore(ApplicationProvider.getApplicationContext<Context>())

    @Test fun independentSlidersWritePersistedValuesAndStartRemainsReachable() {
        val store = store()
        var toggles = 0
        render(store, onToggle = { toggles++ })
        compose.onNodeWithTag("mic_gain").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(12f) }
        compose.onNodeWithTag("output_gain").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(9f) }
        compose.onNodeWithTag("output_volume").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(70f) }
        compose.onNodeWithText("Включить звук").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(12f, store.state.value.microphoneDb)
            assertEquals(9f, store.state.value.outputDb)
            assertEquals(0.7f, store.state.value.volume)
            assertEquals(1, toggles)
            assertEquals(store.state.value, store().state.value)
        }
    }
    @Test fun headphonePresetAndDeviceChoiceRemainAfterDeviceTemporarilyDisappears() {
        val store = store()
        val phones = LiteDevice("usb-headphones", "USB-наушники", AudioDeviceInfo.TYPE_USB_HEADSET)
        val devices = mutableStateOf(LiteDevices(outputs = listOf(phones)))
        render(store, devices)
        compose.onNodeWithTag("device_Выход звука").performScrollTo().performClick()
        compose.onNodeWithText("USB-наушники").performClick()
        compose.onNodeWithText(LitePresets.all.first { it.id == store.state.value.presetId }.name).performScrollTo().performClick()
        compose.onNodeWithText("09 · Наушники громко").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("usb-headphones", store.state.value.outputKey); devices.value = LiteDevices() }
        compose.onNodeWithTag("device_Выход звука").performScrollTo().assertTextContains("Сохранённое устройство недоступно")
        compose.runOnIdle { devices.value = LiteDevices(outputs = listOf(phones)) }
        compose.onNodeWithTag("device_Выход звука").assertTextContains("USB-наушники")
        compose.runOnIdle { assertEquals(store.state.value, store().state.value) }
    }
}
