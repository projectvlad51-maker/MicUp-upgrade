package com.micplugin.service

import android.content.Context

/** Only the two new boost values; original application preferences are untouched. */
class BoostSettings(context: Context) {
    private val prefs = context.getSharedPreferences("micup_106_volume", Context.MODE_PRIVATE)
    private fun clean(db: Float, max: Float) = if (db.isFinite()) db.coerceIn(0f, max) else 0f
    val microphoneDb get() = clean(prefs.getFloat("microphone_db", 0f), 36f)
    val outputDb get() = clean(prefs.getFloat("output_db", 0f), 24f)
    fun microphone(db: Float): Float = clean(db, 36f).also { prefs.edit().putFloat("microphone_db", it).apply() }
    fun output(db: Float): Float = clean(db, 24f).also { prefs.edit().putFloat("output_db", it).apply() }
}
