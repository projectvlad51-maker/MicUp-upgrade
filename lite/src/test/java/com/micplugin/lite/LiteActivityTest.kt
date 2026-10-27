package com.micplugin.lite

import android.app.Application
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = LiteApplication::class)
class LiteActivityTest {
    @get:Rule val compose = createAndroidComposeRule<LiteActivity>()
    @Test fun openingAndRecreatingScreenDoesNotStartSoundOrResetSavedSettings() {
        val app = compose.activity.application as LiteApplication
        compose.runOnIdle {
            assertNull(shadowOf(app as Application).nextStartedService)
            app.settings.preset("headphones_loud")
            app.settings.microphone(15f)
            app.settings.outputDevice("saved-usb-headset")
        }
        val before = app.settings.state.value
        compose.activityRule.scenario.recreate()
        compose.runOnIdle {
            assertEquals(before, (compose.activity.application as LiteApplication).settings.state.value)
            assertNull(shadowOf(app as Application).nextStartedService)
        }
    }
}
