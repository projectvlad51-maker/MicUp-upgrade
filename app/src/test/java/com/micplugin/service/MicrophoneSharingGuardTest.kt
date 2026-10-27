package com.micplugin.service

import org.junit.Assert.*
import org.junit.Test

class MicrophoneSharingGuardTest {
    private class Monitor : RecordingMonitor {
        var callback: ((List<CaptureStatus>) -> Unit)? = null
        var removals = 0
        override fun register(callback: (List<CaptureStatus>) -> Unit) { this.callback = callback }
        override fun unregister() { callback = null; removals++ }
    }
    @Test fun silencingEitherSideOfRecordingConflictTriggersSingleRelease() {
        val monitor = Monitor()
        var stopped = 0
        val guard = MicrophoneSharingGuard(monitor, { 12 }, { stopped++ })
        guard.start()
        monitor.callback!!(listOf(CaptureStatus(13, false), CaptureStatus(12, false)))
        assertEquals(0, stopped)
        monitor.callback!!(listOf(CaptureStatus(13, true), CaptureStatus(12, false)))
        monitor.callback!!(listOf(CaptureStatus(12, true)))
        assertEquals(1, stopped)
        guard.stop()
        assertNull(monitor.callback)
    }
    @Test fun invalidSessionAndStoppedGuardCannotPauseNewCapture() {
        val monitor = Monitor()
        var session = -1
        var stopped = 0
        val guard = MicrophoneSharingGuard(monitor, { session }, { stopped++ })
        guard.start()
        val oldCallback = monitor.callback!!
        oldCallback(listOf(CaptureStatus(-1, true)))
        guard.stop(); session = 12
        oldCallback(listOf(CaptureStatus(12, true)))
        assertEquals(0, stopped)
    }
    @Test fun existingCallModesAreBusyButNormalModeIsNot() {
        assertFalse(MicrophoneSharingGuard.communicationBusy(android.media.AudioManager.MODE_NORMAL))
        assertTrue(MicrophoneSharingGuard.communicationBusy(android.media.AudioManager.MODE_IN_COMMUNICATION))
        assertTrue(MicrophoneSharingGuard.communicationBusy(android.media.AudioManager.MODE_IN_CALL))
    }
}
