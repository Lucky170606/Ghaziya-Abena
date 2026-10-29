package com.example.game

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

class SoundManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("flappy_bird_prefs", Context.MODE_PRIVATE)

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) {
            prefs.edit().putBoolean("sound_enabled", value).apply()
        }

    var hapticEnabled: Boolean
        get() = prefs.getBoolean("haptic_enabled", true)
        set(value) {
            prefs.edit().putBoolean("haptic_enabled", value).apply()
        }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var flapSoundId: Int = 0
    private var scoreSoundId: Int = 0
    private var hitSoundId: Int = 0
    private var dieSoundId: Int = 0
    private var powerUpSoundId: Int = 0
    private var shootSoundId: Int = 0
    private var explodeSoundId: Int = 0

    init {
        try {
            val cache = context.cacheDir
            val flapFile = File(cache, "flap.wav").apply {
                if (!exists() || length() == 0L) {
                    writeBytes(createWav(generateChirp(startFreq = 480.0, endFreq = 850.0, durationMs = 70)))
                }
            }
            val scoreFile = File(cache, "score.wav").apply {
                if (!exists() || length() == 0L) {
                    writeBytes(createWav(generateTwoTone(freq1 = 988.0, freq2 = 1318.0, durationMs1 = 50, durationMs2 = 90)))
                }
            }
            val hitFile = File(cache, "hit.wav").apply {
                if (!exists() || length() == 0L) {
                    writeBytes(createWav(generateThud(freq = 160.0, durationMs = 90)))
                }
            }
            val dieFile = File(cache, "die.wav").apply {
                if (!exists() || length() == 0L) {
                    writeBytes(createWav(generateChirp(startFreq = 620.0, endFreq = 180.0, durationMs = 180)))
                }
            }
            val powerUpFile = File(cache, "powerup.wav").apply {
                if (!exists() || length() == 0L) {
                    writeBytes(createWav(generatePowerUpFanfare()))
                }
            }
            val shootFile = File(cache, "shoot.wav").apply {
                if (!exists() || length() == 0L) {
                    writeBytes(createWav(generateChirp(startFreq = 1600.0, endFreq = 400.0, durationMs = 110)))
                }
            }
            val explodeFile = File(cache, "explode.wav").apply {
                if (!exists() || length() == 0L) {
                    writeBytes(createWav(generateExplosion()))
                }
            }

            flapSoundId = soundPool.load(flapFile.absolutePath, 1)
            scoreSoundId = soundPool.load(scoreFile.absolutePath, 1)
            hitSoundId = soundPool.load(hitFile.absolutePath, 1)
            dieSoundId = soundPool.load(dieFile.absolutePath, 1)
            powerUpSoundId = soundPool.load(powerUpFile.absolutePath, 1)
            shootSoundId = soundPool.load(shootFile.absolutePath, 1)
            explodeSoundId = soundPool.load(explodeFile.absolutePath, 1)
        } catch (_: Exception) {
            // Fallback gracefully if filesystem fails
        }
    }

    fun playFlap() {
        if (!soundEnabled || flapSoundId == 0) return
        soundPool.play(flapSoundId, 0.9f, 0.9f, 1, 0, 1.0f)
    }

    fun playScore() {
        if (!soundEnabled || scoreSoundId == 0) return
        soundPool.play(scoreSoundId, 1.0f, 1.0f, 2, 0, 1.0f)
    }

    fun playHit() {
        if (!soundEnabled || hitSoundId == 0) return
        soundPool.play(hitSoundId, 1.0f, 1.0f, 3, 0, 1.0f)
    }

    fun playDie() {
        if (!soundEnabled || dieSoundId == 0) return
        soundPool.play(dieSoundId, 0.9f, 0.9f, 2, 0, 1.0f)
    }

    fun playPowerUp() {
        if (!soundEnabled || powerUpSoundId == 0) return
        soundPool.play(powerUpSoundId, 1.0f, 1.0f, 3, 0, 1.0f)
    }

    fun playShoot() {
        if (!soundEnabled || shootSoundId == 0) return
        soundPool.play(shootSoundId, 1.0f, 1.0f, 2, 0, 1.0f)
    }

    fun playExplosion() {
        if (!soundEnabled || explodeSoundId == 0) return
        soundPool.play(explodeSoundId, 1.0f, 1.0f, 4, 0, 1.0f)
    }

    fun triggerTapHaptic() {
        if (!hapticEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    }

    fun triggerCrashHaptic() {
        if (!hapticEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    private fun createWav(pcmData: ShortArray): ByteArray {
        val sampleRate = 44100
        val channels = 1
        val byteRate = sampleRate * channels * 2
        val dataSize = pcmData.size * 2
        val totalSize = 36 + dataSize

        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        // RIFF header
        buffer.put("RIFF".toByteArray())
        buffer.putInt(totalSize)
        buffer.put("WAVE".toByteArray())

        // fmt chunk
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16) // Subchunk1Size
        buffer.putShort(1.toShort()) // AudioFormat (1 = PCM)
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort((channels * 2).toShort()) // BlockAlign
        buffer.putShort(16.toShort()) // BitsPerSample

        // data chunk
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)

        for (sample in pcmData) {
            buffer.putShort(sample)
        }

        return buffer.array()
    }

    private fun generateChirp(startFreq: Double, endFreq: Double, durationMs: Int): ShortArray {
        val sampleRate = 44100
        val totalSamples = (sampleRate * durationMs / 1000.0).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val currentFreq = startFreq + (endFreq - startFreq) * progress
            phase += 2.0 * PI * currentFreq / sampleRate

            val envelope = (1.0 - progress).coerceIn(0.0, 1.0)
            val sample = (sin(phase) * envelope * 28000).toInt().toShort()
            buffer[i] = sample
        }
        return buffer
    }

    private fun generateTwoTone(freq1: Double, freq2: Double, durationMs1: Int, durationMs2: Int): ShortArray {
        val sampleRate = 44100
        val s1 = (sampleRate * durationMs1 / 1000.0).toInt()
        val s2 = (sampleRate * durationMs2 / 1000.0).toInt()
        val buffer = ShortArray(s1 + s2)
        var phase = 0.0

        for (i in 0 until s1) {
            phase += 2.0 * PI * freq1 / sampleRate
            val env = 1.0 - (i.toDouble() / s1 * 0.3)
            buffer[i] = (sin(phase) * env * 26000).toInt().toShort()
        }

        phase = 0.0
        for (i in 0 until s2) {
            phase += 2.0 * PI * freq2 / sampleRate
            val env = 1.0 - (i.toDouble() / s2)
            buffer[s1 + i] = (sin(phase) * env * 28000).toInt().toShort()
        }
        return buffer
    }

    private fun generateThud(freq: Double, durationMs: Int): ShortArray {
        val sampleRate = 44100
        val totalSamples = (sampleRate * durationMs / 1000.0).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val currentFreq = freq * (1.0 - progress * 0.6)
            phase += 2.0 * PI * currentFreq / sampleRate
            val envelope = (1.0 - progress) * (1.0 - progress)
            buffer[i] = (sin(phase) * envelope * 30000).toInt().toShort()
        }
        return buffer
    }

    private fun generatePowerUpFanfare(): ShortArray {
        val sampleRate = 44100
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6 arpeggio
        val noteSamples = (sampleRate * 0.06).toInt()
        val totalSamples = noteSamples * notes.size
        val buffer = ShortArray(totalSamples)

        for (n in notes.indices) {
            val freq = notes[n]
            var phase = 0.0
            val offset = n * noteSamples
            for (i in 0 until noteSamples) {
                phase += 2.0 * PI * freq / sampleRate
                val progress = i.toDouble() / noteSamples
                val env = (1.0 - progress * 0.5)
                buffer[offset + i] = (sin(phase) * env * 27000).toInt().toShort()
            }
        }
        return buffer
    }

    private fun generateExplosion(): ShortArray {
        val sampleRate = 44100
        val durationMs = 180
        val totalSamples = (sampleRate * durationMs / 1000.0).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0
        val random = java.util.Random(42)

        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            // Low booming pitch dropping from 220Hz to 40Hz
            val freq = 220.0 * (1.0 - progress * 0.8)
            phase += 2.0 * PI * freq / sampleRate

            val noise = (random.nextDouble() * 2.0 - 1.0)
            val envelope = (1.0 - progress) * (1.0 - progress)
            // Combine noise and low boom
            val wave = sin(phase) * 0.6 + noise * 0.4
            buffer[i] = (wave * envelope * 30000).toInt().toShort()
        }
        return buffer
    }
}
