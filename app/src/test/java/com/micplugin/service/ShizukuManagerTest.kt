package com.micplugin.service

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ShizukuManagerTest {
    private class Bridge : ShizukuBridge {
        var connected = false
        var granted = false
        var deniedPermanently = false
        override fun permissionNeedsSettings() = deniedPermanently
        var registrations = 0
        var requests = 0
        lateinit var received: () -> Unit
        lateinit var died: () -> Unit
        lateinit var permission: () -> Unit
        override fun ping() = connected
        override fun permissionGranted() = granted
        override fun listen(onConnected: () -> Unit, onDisconnected: () -> Unit, onPermissionResult: () -> Unit) {
            registrations++
            received = onConnected; died = onDisconnected; permission = onPermissionResult
        }
        override fun requestPermission() { requests++ }
    }

    @Test fun discoversShizukuStartedAfterMicUp() {
        val bridge = Bridge()
        val manager = ShizukuManager(bridge)
        manager.init()
        assertEquals(ShizukuState.UNAVAILABLE, manager.state.value)
        bridge.connected = true
        bridge.received()
        assertEquals(ShizukuState.NEED_GRANT, manager.state.value)
        manager.requestPermission()
        assertEquals(1, bridge.requests)
        bridge.granted = true
        bridge.permission()
        assertEquals(ShizukuState.READY, manager.state.value)
    }

    @Test fun permanentlyDeniedPermissionRequiresManagerSettingsInsteadOfAnotherPrompt() {
        val bridge = Bridge().apply { connected = true; deniedPermanently = true }
        val manager = ShizukuManager(bridge)
        manager.requestPermission()
        assertEquals(ShizukuState.NEED_GRANT, manager.state.value)
        assertTrue(manager.permissionSettingsRequired.value)
        assertEquals(0, bridge.requests)
        bridge.granted = true
        bridge.permission()
        assertEquals(ShizukuState.READY, manager.state.value)
        assertFalse(manager.permissionSettingsRequired.value)
    }

    @Test fun refreshDoesNotRegisterDuplicateListeners() {
        val bridge = Bridge()
        val manager = ShizukuManager(bridge)
        repeat(3) { manager.init() }
        assertEquals(1, bridge.registrations)
    }

    @Test fun deniedPermissionAndDeadBinderAreNotReportedAsReady() {
        val bridge = Bridge().apply { connected = true }
        val manager = ShizukuManager(bridge)
        manager.init()
        bridge.permission()
        assertEquals(ShizukuState.NEED_GRANT, manager.state.value)
        bridge.granted = true
        bridge.permission()
        assertEquals(ShizukuState.READY, manager.state.value)
        bridge.connected = false
        bridge.died()
        assertEquals(ShizukuState.UNAVAILABLE, manager.state.value)
        bridge.permission()
        assertEquals(ShizukuState.UNAVAILABLE, manager.state.value)
    }

    @Test fun permissionRequestChecksCurrentConnectionFirst() {
        val bridge = Bridge().apply { connected = true; granted = true }
        val manager = ShizukuManager(bridge)
        manager.requestPermission()
        assertEquals(0, bridge.requests)
        assertEquals(ShizukuState.READY, manager.state.value)
        bridge.connected = false
        manager.requestPermission()
        assertEquals(0, bridge.requests)
        assertEquals(ShizukuState.UNAVAILABLE, manager.state.value)
    }
}
