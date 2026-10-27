package com.micplugin.audio

/** Named JNI callback: its method name/signature are retained in Lite's R8 rules. */
fun interface AudioMonitorCallback {
    fun onAudioBuffer(samples: FloatArray, count: Int)
}
