package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object AlarmSoundManager {
    private const val TAG = "AlarmSoundManager"
    private var activeRingtone: Ringtone? = null
    private var activeToneGenerator: ToneGenerator? = null
    private var previewJob: Job? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)

    /**
     * Previews a selected ringtone for 4 seconds then stops automatically.
     */
    fun previewRingtone(context: Context, ringtoneType: String, customUri: String? = null, onFinished: (() -> Unit)? = null) {
        stopAll()

        previewJob = mainScope.launch {
            try {
                if (ringtoneType == "custom" && customUri != null) {
                    playCustomRingtone(context, customUri)
                } else {
                    when (ringtoneType) {
                        "chimes" -> playMelodicSequence(listOf(ToneGenerator.TONE_DTMF_1, ToneGenerator.TONE_DTMF_5, ToneGenerator.TONE_DTMF_9, ToneGenerator.TONE_DTMF_D), 180L, 2)
                        "energy" -> playMelodicSequence(listOf(ToneGenerator.TONE_CDMA_HIGH_L, ToneGenerator.TONE_CDMA_MED_PBX_L, ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE), 150L, 3)
                        "bell" -> playMelodicSequence(listOf(ToneGenerator.TONE_PROP_BEEP2, ToneGenerator.TONE_PROP_PROMPT), 400L, 2)
                        "zen" -> playMelodicSequence(listOf(ToneGenerator.TONE_PROP_ACK, ToneGenerator.TONE_PROP_BEEP), 600L, 2)
                        "digital" -> playMelodicSequence(listOf(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD), 200L, 3)
                        else -> playSystemRingtone(context)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Preview error: ${e.message}")
            } finally {
                delay(3500L)
                stopAll()
                onFinished?.invoke()
            }
        }
    }

    /**
     * Plays the alarm sound continuously when the alarm triggers.
     */
    fun playAlarmSound(context: Context, ringtoneType: String, customUri: String? = null) {
        stopAll()
        previewJob = mainScope.launch {
            try {
                if (ringtoneType == "custom" && customUri != null) {
                    playCustomRingtone(context, customUri)
                } else {
                    when (ringtoneType) {
                        "chimes" -> {
                            while (isActive) {
                                playMelodicSequence(listOf(ToneGenerator.TONE_DTMF_1, ToneGenerator.TONE_DTMF_5, ToneGenerator.TONE_DTMF_9, ToneGenerator.TONE_DTMF_D), 200L, 1)
                                delay(1000L)
                            }
                        }
                        "energy" -> {
                            while (isActive) {
                                playMelodicSequence(listOf(ToneGenerator.TONE_CDMA_HIGH_L, ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, ToneGenerator.TONE_CDMA_HIGH_L), 150L, 1)
                                delay(800L)
                            }
                        }
                        "bell" -> {
                            while (isActive) {
                                playMelodicSequence(listOf(ToneGenerator.TONE_PROP_BEEP2, ToneGenerator.TONE_PROP_PROMPT), 450L, 1)
                                delay(1200L)
                            }
                        }
                        "zen" -> {
                            while (isActive) {
                                playMelodicSequence(listOf(ToneGenerator.TONE_PROP_ACK, ToneGenerator.TONE_PROP_BEEP), 700L, 1)
                                delay(1500L)
                            }
                        }
                        "digital" -> {
                            while (isActive) {
                                playMelodicSequence(listOf(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD), 250L, 1)
                                delay(900L)
                            }
                        }
                        else -> {
                            playSystemRingtone(context)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Alarm sound error: ${e.message}")
            }
        }
    }

    private suspend fun playMelodicSequence(tones: List<Int>, durationMs: Long, repeatCount: Int) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 95)
            activeToneGenerator = toneGen
            for (r in 0 until repeatCount) {
                for (tone in tones) {
                    toneGen.startTone(tone, durationMs.toInt())
                    delay(durationMs + 40L)
                }
                delay(200L)
            }
        } catch (e: Exception) {
            Log.e(TAG, "ToneGenerator error: ${e.message}")
        }
    }

    private fun playCustomRingtone(context: Context, uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ringtone.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                }
                activeRingtone = ringtone
                ringtone.play()
            } else {
                playSystemRingtone(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Custom ringtone error: ${e.message}")
            playSystemRingtone(context)
        }
    }

    private fun playSystemRingtone(context: Context) {
        try {
            val alertUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, alertUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            activeRingtone = ringtone
            ringtone.play()
        } catch (e: Exception) {
            Log.e(TAG, "System ringtone error: ${e.message}")
        }
    }

    /**
     * Stops all running ringtones, tone generators, and preview jobs immediately.
     */
    fun stopAll() {
        previewJob?.cancel()
        previewJob = null

        try {
            activeToneGenerator?.stopTone()
            activeToneGenerator?.release()
        } catch (e: Exception) {
            // ignore
        } finally {
            activeToneGenerator = null
        }

        try {
            if (activeRingtone?.isPlaying == true) {
                activeRingtone?.stop()
            }
        } catch (e: Exception) {
            // ignore
        } finally {
            activeRingtone = null
        }
    }
}
