package com.micplugin.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.micplugin.service.VirtualMicTier
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PlaybackTierRowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun tappingVoipRowSelectsTheMode() {
        val selected = mutableStateOf(VirtualMicTier.MEDIA_PROJECTION)
        compose.setContent {
            TierRow(VirtualMicTier.VOIP_STREAM,
                isActive = selected.value == VirtualMicTier.VOIP_STREAM,
                isLocked = false,
                onSelect = { selected.value = VirtualMicTier.VOIP_STREAM })
        }
        compose.onNodeWithText(VirtualMicTier.VOIP_STREAM.displayName).performClick()
        compose.runOnIdle { assertEquals(VirtualMicTier.VOIP_STREAM, selected.value) }
    }
}
