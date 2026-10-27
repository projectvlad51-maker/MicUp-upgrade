package com.micplugin.lite

import android.app.Application

class LiteApplication : Application() {
    val settings by lazy { LiteSettingsStore(this) }
    val devices by lazy { LiteDeviceCatalog(this) }
    val engine by lazy { LiteEngine(this, settings, devices) }
}
