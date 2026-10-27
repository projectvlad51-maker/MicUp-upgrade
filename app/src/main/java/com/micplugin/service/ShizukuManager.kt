package com.micplugin.service

import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

enum class ShizukuState { UNAVAILABLE, NEED_GRANT, READY }

internal interface ShizukuBridge {
    fun ping(): Boolean
    fun permissionGranted(): Boolean
    fun listen(onConnected: () -> Unit, onDisconnected: () -> Unit, onPermissionResult: () -> Unit)
    fun requestPermission()
}

private object AndroidShizukuBridge : ShizukuBridge {
    override fun ping() = Shizuku.pingBinder()
    override fun permissionGranted() = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    override fun listen(onConnected: () -> Unit, onDisconnected: () -> Unit, onPermissionResult: () -> Unit) {
        Shizuku.addBinderReceivedListener { onConnected() }
        Shizuku.addBinderDeadListener { onDisconnected() }
        Shizuku.addRequestPermissionResultListener { _, _ -> onPermissionResult() }
    }
    override fun requestPermission() = Shizuku.requestPermission(42)
}

@Singleton
class ShizukuManager internal constructor(private val bridge: ShizukuBridge) {
    @Inject constructor() : this(AndroidShizukuBridge)

    private val _state = MutableStateFlow(ShizukuState.UNAVAILABLE)
    val state: StateFlow<ShizukuState> = _state
    private var initialized = false

    fun init() {
        if (!initialized) {
            // Register even when Shizuku has not started yet.
            bridge.listen(::refreshState, { _state.value = ShizukuState.UNAVAILABLE }, ::refreshState)
            initialized = true
        }
        refreshState()
    }

    fun refreshState() {
        _state.value = try {
            when {
                !bridge.ping() -> ShizukuState.UNAVAILABLE
                bridge.permissionGranted() -> ShizukuState.READY
                else -> ShizukuState.NEED_GRANT
            }
        } catch (e: Exception) {
            Log.w("ShizukuManager", "Connection check failed", e)
            ShizukuState.UNAVAILABLE
        }
    }

    fun requestPermission() {
        init()
        if (_state.value != ShizukuState.NEED_GRANT) return
        try {
            bridge.requestPermission()
        } catch (e: Exception) {
            Log.w("ShizukuManager", "Permission request failed", e)
            refreshState()
        }
    }
}
