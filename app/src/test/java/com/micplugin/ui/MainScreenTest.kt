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


    @Test fun processingAndPlaybackSwitchesControlTheirOwnFunctions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        lateinit var vm: AudioViewModel
        compose.runOnIdle { vm = createViewModel(context, ShizukuManager()) }
        compose.setContent { MicPluginTheme { MainScreen(rememberNavController(), vm) } }
        compose.runOnIdle { vm.setMonitoring(false) }
        compose.onNodeWithText("Обработка голоса").performScrollTo().assertIsOn().performClick().assertIsOff()
        compose.runOnIdle { assertEquals(true, vm.masterBypass.value); assertEquals(false, vm.monitoringEnabled.value) }
        compose.onNodeWithText("Проверка микрофона").performScrollTo().performClick().assertIsOn()
        compose.runOnIdle { assertEquals(true, vm.monitoringEnabled.value); assertEquals(true, vm.masterBypass.value) }
        compose.onNodeWithText("Обработка голоса").performScrollTo().performClick().assertIsOn()
        compose.runOnIdle { assertEquals(false, vm.masterBypass.value); assertEquals(true, vm.monitoringEnabled.value) }
    }

    @Test fun mediaAndVoipButtonsChooseMatchingPlaybackRoute() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        lateinit var vm: AudioViewModel
        compose.runOnIdle { vm = createViewModel(context, ShizukuManager()) }
        compose.setContent { MicPluginTheme { MainScreen(rememberNavController(), vm) } }
        compose.onNodeWithText("Связь / VoIP").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(VirtualMicTier.VOIP_STREAM, vm.virtualMicTier.value) }
        compose.onNodeWithText("Медиа / динамики").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(VirtualMicTier.MEDIA_PROJECTION, vm.virtualMicTier.value) }
    }

    @Test fun resetSpeakerBoostDoesNotResetMicrophoneGain() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        lateinit var vm: AudioViewModel
        compose.runOnIdle {
            vm = createViewModel(context, ShizukuManager())
            vm.setMicrophoneGain(12f)
            vm.setSpeakerBoost(18f)
        }
        compose.setContent { MicPluginTheme { MainScreen(rememberNavController(), vm) } }
        compose.onNodeWithText("Сбросить усиление вывода").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(0f, vm.speakerBoostState.value.gainDb)
            assertEquals(12f, vm.microphoneGainDb.value)
            assertEquals(0f, context.getSharedPreferences("micup_prefs", Context.MODE_PRIVATE)
                .getFloat("speaker_boost_db", -1f))
        }
    }


    @Test fun pluginStorageButtonOpensAndroidSettingsInsteadOfShizuku() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        lateinit var vm: AudioViewModel
        compose.runOnIdle { vm = createViewModel(context, ShizukuManager()) }
        val app = org.robolectric.Shadows.shadowOf(context as android.app.Application)
        while (app.nextStartedActivity != null) { }
        compose.setContent { MicPluginTheme { SettingsScreen(rememberNavController(), vm) } }
        compose.onNodeWithText("Доступ к папкам плагинов").performScrollTo().performClick()
        compose.runOnIdle {
            val intent = app.nextStartedActivity
            assertEquals(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
            assertEquals("package:" + context.packageName, intent.data.toString())
        }
    }

    @Test fun monitoringChoicePersistsAndPresetsCannotEnableIt() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        compose.runOnIdle {
            val first = createViewModel(context, ShizukuManager())
            first.setMonitoring(true)
            assertEquals(true, createViewModel(context, ShizukuManager()).monitoringEnabled.value)
            first.setMonitoring(false)
            first.applyPreset(PresetManager.BUILT_IN.first())
            assertEquals(false, first.monitoringEnabled.value)
            assertEquals(false, createViewModel(context, ShizukuManager()).monitoringEnabled.value)
        }
    }

    @Test fun noiseSuppressionSettingIsSavedIndependentlyOfPresetAndMonitoring() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        compose.runOnIdle {
            val vm = createViewModel(context, ShizukuManager())
            vm.setNoiseReduction(false)
            vm.applyPreset(PresetManager.BUILT_IN.first())
            assertEquals(false, vm.noiseReductionEnabled.value)
            assertEquals(false, createViewModel(context, ShizukuManager()).noiseReductionEnabled.value)
        }
    }

}
