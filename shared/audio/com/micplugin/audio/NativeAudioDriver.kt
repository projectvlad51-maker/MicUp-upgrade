package com.micplugin.audio

/** The original Oboe capture/processing bridge, shared by full and Lite. */
interface NativeAudioDriver {
    fun start(): Boolean
    fun stop()
    fun isRunning(): Boolean
    fun setParam(effectId: Int, paramId: Int, value: Float)
    fun getLevels(): FloatArray
    fun setMonitorCallback(callback: ((FloatArray, Int) -> Unit)?)
    fun getInputSessionId(): Int
    fun getSampleRate(): Int
    fun getFramesPerBurst(): Int
    fun setInputDevice(id: Int)
}
