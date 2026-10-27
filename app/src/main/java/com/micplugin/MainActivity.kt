package com.micplugin

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.micplugin.plugin.PluginImporter
import com.micplugin.service.AudioProcessingService
import com.micplugin.service.ShizukuManager
import com.micplugin.service.ShizukuState
import com.micplugin.service.VirtualMicService
import com.micplugin.ui.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import dagger.hilt.android.AndroidEntryPoint
import rikka.shizuku.Shizuku
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var shizukuManager: ShizukuManager
    @Inject lateinit var virtualMicService: VirtualMicService

    private val permissionsToRequest = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.MODIFY_AUDIO_SETTINGS)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT <= 32) add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }.toTypedArray()

    private var startAfterPermission = false

    fun requestAudioStart() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startAudioService()
        } else {
            startAfterPermission = true
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (startAfterPermission) {
            startAfterPermission = false
            if (results[Manifest.permission.RECORD_AUDIO] == true) startAudioService()
            else android.widget.Toast.makeText(this, "Для запуска разрешите доступ к микрофону",
                android.widget.Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        shizukuManager.refreshState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) requestAudioStart()
        // Handle file opened from file manager
        handleIncomingPlugin(intent)
        setContent {
            MicPluginTheme {
                val navController = rememberNavController()
                Scaffold(containerColor = StudioColors.Background) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(StudioColors.Background)
                            .padding(padding),
                    ) {
                        NavHost(navController, startDestination = "main") {
                            composable("main") { MainScreen(navController) }
                            composable("plugin_browser") { PluginBrowserScreen(navController) }
                            composable(
                                "plugin_editor/{slotId}",
                                arguments = listOf(navArgument("slotId") { type = NavType.StringType }),
                            ) { back ->
                                val slotId = back.arguments?.getString("slotId") ?: return@composable
                                PluginEditorScreen(navController, slotId)
                            }
                            composable("settings") { SettingsScreen(navController) }
                            composable("plugin_paths") { PluginPathsScreen(navController) }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIncomingPlugin(intent)
    }

    private fun handleIncomingPlugin(intent: android.content.Intent?) {
        val uri = intent?.data ?: return
        if (intent.action != android.content.Intent.ACTION_VIEW) return
        val activity = this
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
            val result = withContext(Dispatchers.IO) {
                PluginImporter.importFromUri(activity, uri)
            }
            if (result.success) {
                val vm = androidx.lifecycle.ViewModelProvider(activity)[com.micplugin.ui.AudioViewModel::class.java]
                vm.rescan()
                android.widget.Toast.makeText(
                    activity,
                    "Plugin imported: ${result.pluginDescriptor?.name}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                android.widget.Toast.makeText(
                    activity,
                    "Import failed: ${result.error}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
    }

    private fun startAudioService() {
        AudioProcessingService.start(this)
    }

}
