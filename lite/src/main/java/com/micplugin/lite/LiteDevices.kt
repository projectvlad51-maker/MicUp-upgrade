package com.micplugin.lite

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LiteDevice(val key: String, val name: String, val type: Int, val info: AudioDeviceInfo? = null)
data class LiteDevices(val inputs: List<LiteDevice> = emptyList(), val outputs: List<LiteDevice> = emptyList())

class LiteDeviceCatalog(context: Context) {
    val manager: AudioManager = context.getSystemService(AudioManager::class.java)
    private val _state = MutableStateFlow(LiteDevices())
    val state: StateFlow<LiteDevices> = _state
    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refresh()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refresh()
    }
    init { refresh(); manager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper())) }

    fun refresh() {
        val inputs = manager.getDevices(AudioManager.GET_DEVICES_INPUTS).map(::describe).distinctBy { it.key }
        val outputs = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        _state.value = LiteDevices(inputs, outputs.map(::describe).distinctBy { it.key })
    }

    private fun describe(device: AudioDeviceInfo): LiteDevice {
        val name = when (device.type) {
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Микрофон телефона"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Динамик телефона"
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Разговорный динамик"
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Проводные наушники"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth-гарнитура"
            AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> "USB-аудио"
            else -> device.productName.toString()
        }
        // IDs change on reconnect. Keep a stable type/address/name identity instead.
        val key = "${device.type}|${device.address}|${device.productName}"
        return LiteDevice(key, "$name · ${device.productName}", device.type, device)
    }

    companion object {
        fun selected(key: String, devices: List<LiteDevice>): LiteDevice? = devices.firstOrNull { it.key == key }
        fun automaticOutput(devices: List<LiteDevice>): LiteDevice? {
            val order = listOf(AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE)
            return order.firstNotNullOfOrNull { type -> devices.firstOrNull { it.type == type } } ?: devices.firstOrNull()
        }
    }
}
