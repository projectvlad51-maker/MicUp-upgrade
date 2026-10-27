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
import org.junit.Assert.assertEquals
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
        lateinit var vm: AudioViewModel
        compose.runOnIdle { vm = createViewModel(context, ShizukuManager()) }
        compose.setContent { MicPluginTheme { MainScreen(rememberNavController(), vm) } }
        compose.onNodeWithText("Shizuku").assertIsDisplayed()
        compose.onNodeWithText("Mic Boost").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Музыка / видео на телефоне").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Shizuku").assertIsDisplayed()
        compose.onNodeWithText("Эффекты").assertIsDisplayed().performClick()
        compose.onNodeWithText("Shizuku").assertIsDisplayed()
        compose.onNodeWithText("УБРАТЬ ГУЛ И НИЗКИЕ ЧАСТОТЫ").assertExists()
        compose.onNodeWithText("THRESH").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Плагины").assertIsDisplayed().performClick()
        compose.onNodeWithText("Shizuku").assertIsDisplayed()
        compose.onNodeWithText("Add Plugin").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Громкость").assertIsDisplayed().performClick()
        compose.onNodeWithText("Shizuku").assertIsDisplayed()
        compose.onNodeWithText("Mic Boost").performScrollTo().assertIsDisplayed()
    }

    private fun createViewModel(context: Context, shizuku: ShizukuManager): AudioViewModel {
        val paths = PluginPathPrefs().apply { init(context) }
        return AudioViewModel(context, AudioEngine(OboeEngine(), PluginChain()),
            PluginManager(context, paths), PresetManager(context), VirtualMicService(context, shizuku),
            shizuku, paths, DeviceVolumeController(context))
    }

    @Test fun mainScreenPermissionButtonRequestsAccessAndReflectsGrantResult() {
        val bridge = object : ShizukuBridge {
            var granted = false
            var requests = 0
            lateinit var permissionResult: () -> Unit
            override fun ping() = true
            override fun permissionGranted() = granted
            override fun listen(onConnected: () -> Unit, onDisconnected: () -> Unit, onPermissionResult: () -> Unit) {
                permissionResult = onPermissionResult
            }
            override fun requestPermission() { requests++ }
        }
        val manager = ShizukuManager(bridge).apply { init() }
        val context = ApplicationProvider.getApplicationContext<Context>()
        lateinit var vm: AudioViewModel
        compose.runOnIdle { vm = createViewModel(context, manager) }
        compose.setContent { MicPluginTheme { MainScreen(rememberNavController(), vm) } }
        compose.onNodeWithText("Разрешить").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1, bridge.requests)
            bridge.granted = true
            bridge.permissionResult()
        }
        compose.onNodeWithText("Подключён · доступ разрешён").assertIsDisplayed()
    }


    @Test fun unavailableServiceButtonOpensInstalledShizukuManager() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerPackage = "moe.shizuku.privileged.api"
        val managerActivity = "moe.shizuku.manager.MainActivity"
        val resolve = android.content.pm.ResolveInfo().apply {
            activityInfo = android.content.pm.ActivityInfo().apply {
                packageName = managerPackage
                name = managerActivity
            }
        }
        val launchQuery = android.content.Intent(android.content.Intent.ACTION_MAIN)
            .addCategory(android.content.Intent.CATEGORY_INFO).setPackage(managerPackage)
        org.robolectric.Shadows.shadowOf(context.packageManager).addResolveInfoForIntent(launchQuery, resolve)
        val app = org.robolectric.Shadows.shadowOf(context as android.app.Application)
        while (app.nextStartedActivity != null) { /* discard setup intents */ }
        val bridge = object : ShizukuBridge {
            override fun ping() = false
            override fun permissionGranted() = false
            override fun listen(onConnected: () -> Unit, onDisconnected: () -> Unit, onPermissionResult: () -> Unit) {}
            override fun requestPermission() { error("Cannot request permission without a running service") }
        }
        lateinit var vm: AudioViewModel
        compose.runOnIdle { vm = createViewModel(context, ShizukuManager(bridge)) }
        compose.setContent { MicPluginTheme { MainScreen(rememberNavController(), vm) } }
        compose.onNodeWithText("Открыть Shizuku").performClick()
        compose.runOnIdle {
            assertEquals(android.content.ComponentName(managerPackage, managerActivity), app.nextStartedActivity.component)
        }
    }

}
