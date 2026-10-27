package com.micplugin

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.micplugin.service.AudioProcessingService
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = MicPluginApp::class)
class MainActivityControlsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun openingAppDoesNotStartAudioOrOpenSettingsButExplicitStartDoes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = shadowOf(context as Application)
        compose.runOnIdle {
            assertNull(app.nextStartedService)
            var intent = app.nextStartedActivity
            while (intent != null) {
                assertNotEquals(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION, intent.action)
                assertNotEquals(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, intent.action)
                intent = app.nextStartedActivity
            }
            shadowOf(context).grantPermissions(Manifest.permission.RECORD_AUDIO)
            compose.activity.requestAudioStart()
            assertEquals(AudioProcessingService::class.java.name, app.nextStartedService.component?.className)
        }
    }
}
