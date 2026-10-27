package com.micplugin.ui

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.micplugin.audio.AudioEngine
import com.micplugin.audio.OboeEngine
import com.micplugin.plugin.*
import com.micplugin.preset.PresetManager
import com.micplugin.service.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w320dp-h640dp")
class MainScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun narrowScreenKeepsTabsReachableAndShowsSeparateControlGroups() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val paths = PluginPathPrefs().apply { init(context) }
        val shizuku = ShizukuManager()
        lateinit var vm: AudioViewModel
        compose.runOnIdle {
            vm = AudioViewModel(context, AudioEngine(OboeEngine(), PluginChain()),
                PluginManager(context, paths), PresetManager(context),
                VirtualMicService(context, shizuku), shizuku, paths, DeviceVolumeController(context))
        }
        compose.setContent { MicPluginTheme { MainScreen(rememberNavController(), vm) } }
        compose.onNodeWithText("Mic Boost").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Музыка / видео на телефоне").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Эффекты").assertIsDisplayed().performClick()
        compose.onNodeWithText("УБРАТЬ ГУЛ И НИЗКИЕ ЧАСТОТЫ").assertExists()
        compose.onNodeWithText("THRESH").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Плагины").assertIsDisplayed().performClick()
        compose.onNodeWithText("Add Plugin").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Громкость").assertIsDisplayed().performClick()
        compose.onNodeWithText("Mic Boost").performScrollTo().assertIsDisplayed()
    }
}
