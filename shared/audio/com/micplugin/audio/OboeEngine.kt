package com.micplugin.audio


/**
 * JNI bridge to the C++ AudioEngine / PluginHost.
 * All native methods map 1-to-1 with the extern "C" functions in AudioEngine.cpp.
 */
class OboeEngine : NativeAudioDriver {

    private var engineHandle: Long = 0L

    private var libraryLoaded = false

    init {
        try {
            System.loadLibrary("micplugin_native")
            libraryLoaded = true
            engineHandle = nativeCreate()
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e("OboeEngine", "Failed to load native library: $e")
            engineHandle = 0L
        } catch (e: Exception) {
            android.util.Log.e("OboeEngine", "Native init error: $e")
            engineHandle = 0L
        }
    }

    fun isNativeLoaded() = libraryLoaded && engineHandle != 0L

    override fun start(): Boolean = if (engineHandle != 0L) nativeStart(engineHandle) else false
    override fun stop() { if (engineHandle != 0L) nativeStop(engineHandle) }
    fun isValid() = engineHandle != 0L
    override fun isRunning(): Boolean = engineHandle != 0L && nativeIsRunning(engineHandle)

    override fun setParam(effectId: Int, paramId: Int, value: Float) {
        if (engineHandle != 0L) nativeSetParam(engineHandle, effectId, paramId, value)
    }

    override fun getLevels(): FloatArray =
        if (engineHandle != 0L) nativeGetLevels(engineHandle) else floatArrayOf(-100f, -100f, 0f)

    fun loadPlugin(soPath: String, formatId: Int): Long =
        if (engineHandle != 0L) nativeLoadPlugin(engineHandle, soPath, formatId) else 0L

    fun unloadPlugin(pluginHandle: Long) {
        if (engineHandle != 0L) nativeUnloadPlugin(engineHandle, pluginHandle)
    }

    fun getPluginParams(pluginHandle: Long): String =
        if (engineHandle != 0L) nativeGetPluginParams(engineHandle, pluginHandle) else "[]"

    override fun setMonitorCallback(callback: ((FloatArray, Int) -> Unit)?) {
        if (engineHandle != 0L) nativeSetMonitorCallback(engineHandle, callback?.let { function -> AudioMonitorCallback { samples, count -> function(samples, count) } })
    }

    fun setPluginParam(pluginHandle: Long, paramId: Int, value: Float) {
        if (engineHandle != 0L) nativeSetPluginParam(engineHandle, pluginHandle, paramId, value)
    }

    override fun getInputSessionId(): Int = if (engineHandle != 0L) nativeGetInputSessionId(engineHandle) else -1

    override fun getSampleRate(): Int = if (engineHandle != 0L) nativeGetSampleRate(engineHandle) else 48000
    override fun getFramesPerBurst(): Int = if (engineHandle != 0L) nativeGetFramesPerBurst(engineHandle) else 128

    fun destroy() {
        if (engineHandle != 0L) {
            nativeDestroy(engineHandle)
            engineHandle = 0L
        }
    }

    override fun setInputDevice(id: Int) { if (engineHandle != 0L) nativeSetInputDevice(engineHandle, id) }

    // ── JNI declarations ──────────────────────────────────────────────────────
    private external fun nativeSetInputDevice(handle: Long, deviceId: Int)
    private external fun nativeCreate(): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeStart(handle: Long): Boolean
    private external fun nativeStop(handle: Long)
    private external fun nativeSetParam(handle: Long, effectId: Int, paramId: Int, value: Float)
    private external fun nativeGetLevels(handle: Long): FloatArray
    private external fun nativeLoadPlugin(handle: Long, soPath: String, formatId: Int): Long
    private external fun nativeUnloadPlugin(handle: Long, pluginHandle: Long)
    private external fun nativeSetPluginParam(handle: Long, pluginHandle: Long, paramId: Int, value: Float)
    private external fun nativeGetPluginParams(handle: Long, pluginHandle: Long): String
    private external fun nativeSetMonitorCallback(handle: Long, callback: AudioMonitorCallback?)
    private external fun nativeIsRunning(handle: Long): Boolean
    private external fun nativeGetInputSessionId(handle: Long): Int
    private external fun nativeGetSampleRate(handle: Long): Int
    private external fun nativeGetFramesPerBurst(handle: Long): Int
}
