package com.micplugin.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.micplugin.service.ShizukuState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w320dp-h640dp")
class ShizukuConnectionRowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun deniedPermissionShowsAccessSettingsAction() {
        var clicked = false
        compose.setContent {
            MicPluginTheme { ShizukuConnectionRow(ShizukuState.NEED_GRANT, true) { clicked = true } }
        }
        compose.onNodeWithText("Разрешите MicUp в Shizuku").assertIsDisplayed()
        compose.onNodeWithText("Открыть доступ").performClick()
        compose.runOnIdle { assertEquals(true, clicked) }
    }

    @Test fun shizukuEntryStaysVisibleAndActionableInEveryConnectionState() {
        val state = mutableStateOf(ShizukuState.UNAVAILABLE)
        var clicks = 0
        compose.setContent { MicPluginTheme { ShizukuConnectionRow(state.value) { clicks++ } } }
        compose.onNodeWithText("Shizuku").assertIsDisplayed()
        compose.onNodeWithText("Открыть Shizuku").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, clicks); state.value = ShizukuState.NEED_GRANT }
        compose.onNodeWithText("Нужно разрешение для MicUp").assertIsDisplayed()
        compose.onNodeWithText("Разрешить").performClick()
        compose.runOnIdle { assertEquals(2, clicks); state.value = ShizukuState.READY }
        compose.onNodeWithText("Подключён · доступ разрешён").assertIsDisplayed()
        compose.onNodeWithText("Проверить").performClick()
        compose.runOnIdle { assertEquals(3, clicks) }
    }
}
