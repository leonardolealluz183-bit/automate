package com.riftking.mirrorcounter

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator

/** Output only: no microphone, sensors, recording, network or ongoing playback. */
internal class TouchFeedback(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val vibrator = context.getSystemService(Vibrator::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private var tone: ToneGenerator? = null

    fun start() {
        if (tone == null) {
            // Audio initialization must never prevent scoring if the device is muted/unavailable.
            tone = runCatching { ToneGenerator(AudioManager.STREAM_SYSTEM, 65) }.getOrNull()
        }
    }

    @Suppress("DEPRECATION")
    fun play(reset: Boolean) {
        if (vibrator?.hasVibrator() == true) {
            val amplitude = if (vibrator.hasAmplitudeControl()) 255 else VibrationEffect.DEFAULT_AMPLITUDE
            runCatching {
                vibrator.vibrate(VibrationEffect.createOneShot(if (reset) 120L else 70L, amplitude), attributes)
            }
        }
        // Honor the watch's silent/vibrate profile and system volume. Never raise it globally.
        if (audioManager?.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            runCatching { tone?.startTone(ToneGenerator.TONE_PROP_BEEP, if (reset) 70 else 45) }
        }
    }

    fun stop() {
        vibrator?.cancel()
        tone?.release()
        tone = null
    }
}
