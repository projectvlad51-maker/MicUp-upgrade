package com.micplugin.ui

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DashboardTabsTest {
    @get:Rule val compose = createComposeRule()
    @Test fun tabsSelectEffectsPluginsAndReturnToVolume() {
        val selected = mutableIntStateOf(0)
        compose.setContent { DashboardTabs(selected.intValue) { selected.intValue = it } }
        compose.onNodeWithText("Эффекты").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(1, selected.intValue) }
        compose.onNodeWithText("Плагины").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(2, selected.intValue) }
        compose.onNodeWithText("Громкость").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(0, selected.intValue) }
    }
}
