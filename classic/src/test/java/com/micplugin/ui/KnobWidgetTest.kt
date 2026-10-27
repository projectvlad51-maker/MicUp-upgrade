package com.micplugin.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class KnobWidgetTest {
    @get:Rule val compose = createComposeRule()

    private fun drag(dy: Float) {
        compose.onNodeWithTag("knob").performTouchInput {
            down(Offset(width / 2f, 20f))
            repeat(5) { moveBy(Offset(0f, dy), 20) }
            up()
        }
        compose.waitForIdle()
    }

    @Test fun dragAccumulatesAndNextGestureContinuesFromLatestValue() {
        val value = mutableFloatStateOf(-30f)
        val changes = mutableListOf<Float>()
        compose.setContent {
            Box(Modifier.testTag("knob")) {
                KnobWidget("THRESH", value.floatValue, {
                    changes.add(it)
                    value.floatValue = it
                }, -60f..0f)
            }
        }
        drag(-10f)
        compose.runOnIdle {
            assertTrue("Several drag events must be delivered", changes.size >= 3)
            assertTrue("Every upward movement must increase the value",
                changes.zipWithNext().all { (a, b) -> b > a })
        }
        val first = value.floatValue
        drag(-10f)
        compose.runOnIdle { assertTrue(value.floatValue > first) }
        // Simulate a preset being loaded while the same pointerInput is alive.
        compose.runOnIdle { value.floatValue = -50f }
        compose.waitForIdle()
        drag(-10f)
        compose.runOnIdle { assertTrue(value.floatValue in -50f..-35f) }
    }

    @Test fun gestureUsesUpdatedCallbackAfterRecomposition() {
        val value = mutableFloatStateOf(4f)
        var oldCalls = 0
        var newCalls = 0
        val callback = mutableStateOf<(Float) -> Unit>({ oldCalls++; value.floatValue = it })
        compose.setContent {
            Box(Modifier.testTag("knob")) {
                KnobWidget("RATIO", value.floatValue, callback.value, 1f..20f)
            }
        }
        compose.runOnIdle { callback.value = { newCalls++; value.floatValue = it } }
        compose.waitForIdle()
        drag(-10f)
        compose.runOnIdle {
            assertEquals(0, oldCalls)
            assertTrue(newCalls > 0)
        }
    }

    @Test fun downwardDragAccumulatesAndClampsToRange() {
        val value = mutableFloatStateOf(2f)
        val changes = mutableListOf<Float>()
        compose.setContent {
            Box(Modifier.testTag("knob")) {
                KnobWidget("RATIO", value.floatValue, {
                    changes.add(it)
                    value.floatValue = it
                }, 1f..20f)
            }
        }
        drag(40f)
        compose.runOnIdle {
            assertTrue(changes.isNotEmpty())
            assertTrue(changes.all { it in 1f..20f })
            assertEquals(1f, value.floatValue)
        }
        drag(-200f)
        compose.runOnIdle { assertEquals(20f, value.floatValue) }
    }
}
