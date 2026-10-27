# JNI resolves the bridge class and this callback method by name.
-keep class com.micplugin.audio.OboeEngine { *; }
-keep interface com.micplugin.audio.AudioMonitorCallback { *; }
-keepclassmembers class * implements com.micplugin.audio.AudioMonitorCallback {
    public void onAudioBuffer(float[], int);
}
