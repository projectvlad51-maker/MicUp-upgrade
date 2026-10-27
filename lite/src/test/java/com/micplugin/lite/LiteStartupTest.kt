package com.micplugin.lite

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = LiteApplication::class)
class LiteStartupTest {
    private val compose = createAndroidComposeRule<LiteActivity>()
    private val permission = TestRule { base, _ -> object : Statement() {
        override fun evaluate() {
            shadowOf(ApplicationProvider.getApplicationContext<Application>()).grantPermissions(Manifest.permission.RECORD_AUDIO)
            base.evaluate()
        }
    } }
    @get:Rule val rules = RuleChain.outerRule(permission).around(compose)
    @Test fun openingWithPermissionRequestsTheOriginalAudioService() {
        compose.runOnIdle {
            val context = ApplicationProvider.getApplicationContext<Context>()
            assertEquals(com.micplugin.lite.LiteAudioService::class.java.name, shadowOf(context as Application).nextStartedService.component?.className)
        }
    }
}
