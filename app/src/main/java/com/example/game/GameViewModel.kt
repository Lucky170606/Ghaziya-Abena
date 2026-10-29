package com.example.game

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

data class GameUiState(
    val gameState: GameState = GameState.READY,
    val score: Int = 0,
    val highScore: Int = 0,
    val isNewHighScore: Boolean = false,
    val characterSkin: CharacterSkin = CharacterSkin.ROCKY,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val skyTheme: SkyTheme = SkyTheme.RANDOM,
    val skyCycleProgress: Float = 0.05f,
    val currentPhaseName: String = "Siang ☀️",
    val timePlayedSeconds: Float = 0f,
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,

    // Character position & physics
    val birdX: Float = 0f,
    val birdY: Float = 0f,
    val birdVelocity: Float = 0f,
    val birdRotation: Float = 0f,
    val birdWidth: Float = 110f,
    val birdHeight: Float = 140f,

    // Pipes & Ground
    val pipes: List<PipePair> = emptyList(),
    val groundOffset: Float = 0f,
    val groundHeight: Float = 260f,

    // Screen Shake effect on game over
    val shakeOffsetX: Float = 0f,
    val shakeOffsetY: Float = 0f,

    // Visual particles
    val particles: List<Particle> = emptyList()
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    val soundManager = SoundManager(application)
    private val prefs: SharedPreferences =
        application.getSharedPreferences("flappy_bird_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var screenWidth: Float = 1080f
    private var screenHeight: Float = 2160f
    private var density: Float = 2.625f

    private var readyAnimationTime: Float = 0f
    private var pipeSpawnTimer: Float = 0f
    private var pipeIdCounter: Long = 0L
    private var shakeTimer: Float = 0f
    private var timePlayed: Float = 0f

    private fun generateRandomPhase(): Pair<Float, String> {
        val phases = listOf(
            0.05f to "Siang ☀️",
            0.38f to "Senja 🌅",
            0.65f to "Malam 🌙",
            0.88f to "Fajar 🌄"
        )
        return phases[Random.nextInt(phases.size)]
    }

    init {
        val savedDiff = Difficulty.valueOf(prefs.getString("difficulty", Difficulty.NORMAL.name) ?: Difficulty.NORMAL.name)
        val savedSkin = CharacterSkin.valueOf(prefs.getString("skin", CharacterSkin.ROCKY.name) ?: CharacterSkin.ROCKY.name)
        val savedTheme = SkyTheme.valueOf(prefs.getString("theme", SkyTheme.RANDOM.name) ?: SkyTheme.RANDOM.name)
        val savedHighScore = prefs.getInt("high_score_${savedDiff.name}", 0)

        val (initProg, initPhase) = if (savedTheme == SkyTheme.RANDOM) {
            generateRandomPhase()
        } else {
            when (savedTheme) {
                SkyTheme.DAY -> 0.05f to "Siang ☀️"
                SkyTheme.SUNSET -> 0.38f to "Senja 🌅"
                SkyTheme.NIGHT -> 0.65f to "Malam 🌙"
                SkyTheme.RANDOM -> 0.05f to "Siang ☀️"
            }
        }

        _uiState.update {
            it.copy(
                difficulty = savedDiff,
                characterSkin = savedSkin,
                skyTheme = savedTheme,
                skyCycleProgress = initProg,
                currentPhaseName = initPhase,
                highScore = savedHighScore,
                soundEnabled = soundManager.soundEnabled,
                hapticEnabled = soundManager.hapticEnabled
            )
        }
    }

    fun onScreenSizeChanged(width: Float, height: Float, screenDensity: Float) {
        if (screenWidth == width && screenHeight == height && density == screenDensity) return

        screenWidth = width
        screenHeight = height
        density = screenDensity

        val birdW = 44f * density
        val birdH = 56f * density
        val gHeight = 100f * density
        val initialBirdX = screenWidth * 0.28f
        val initialBirdY = (screenHeight - gHeight) * 0.45f

        _uiState.update {
            it.copy(
                birdX = initialBirdX,
                birdY = if (it.gameState == GameState.READY) initialBirdY else it.birdY,
                birdWidth = birdW,
                birdHeight = birdH,
                groundHeight = gHeight
            )
        }
    }

    fun onTap() {
        val currentState = _uiState.value.gameState
        when (currentState) {
            GameState.READY -> {
                _uiState.update { it.copy(gameState = GameState.PLAYING) }
                performFlap()
            }
            GameState.PLAYING -> {
                performFlap()
            }
            GameState.PAUSED -> {
                _uiState.update { it.copy(gameState = GameState.PLAYING) }
            }
            GameState.GAME_OVER -> {
                // Ignore taps on background during game over
            }
        }
    }

    private fun performFlap() {
        val flapSpeed = -460f * density
        _uiState.update {
            it.copy(
                birdVelocity = flapSpeed,
                birdRotation = -22f
            )
        }
        soundManager.playFlap()
        soundManager.triggerTapHaptic()
        spawnJumpParticles()
    }

    fun updateGame(deltaTimeSeconds: Float) {
        val dt = deltaTimeSeconds.coerceIn(0.001f, 0.05f)
        val state = _uiState.value

        if (state.gameState == GameState.PLAYING) {
            timePlayed += dt
        }

        val cycleProgress = when (state.skyTheme) {
            SkyTheme.RANDOM -> state.skyCycleProgress
            SkyTheme.DAY -> 0.05f
            SkyTheme.SUNSET -> 0.38f
            SkyTheme.NIGHT -> 0.65f
        }

        val phaseName = when (state.skyTheme) {
            SkyTheme.RANDOM -> state.currentPhaseName
            SkyTheme.DAY -> "Siang ☀️"
            SkyTheme.SUNSET -> "Senja 🌅"
            SkyTheme.NIGHT -> "Malam 🌙"
        }

        // Screen shake decay
        var sX = 0f
        var sY = 0f
        if (shakeTimer > 0f) {
            shakeTimer -= dt
            val intensity = (shakeTimer / 0.25f) * 16f * density
            sX = (Random.nextFloat() * 2f - 1f) * intensity
            sY = (Random.nextFloat() * 2f - 1f) * intensity
        }

        // Particle updates
        val updatedParticles = state.particles.mapNotNull { p ->
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += 800f * density * dt
            p.alpha -= p.decay
            if (p.alpha > 0f) p else null
        }

        when (state.gameState) {
            GameState.READY -> {
                readyAnimationTime += dt
                val bobbingOffset = sin(readyAnimationTime * 5f) * 12f * density
                val groundSpeed = 140f * density
                val nextGround = (state.groundOffset + groundSpeed * dt) % (30f * density)
                val baseBirdY = (screenHeight - state.groundHeight) * 0.45f

                _uiState.update {
                    it.copy(
                        birdY = baseBirdY + bobbingOffset,
                        birdVelocity = 0f,
                        birdRotation = 0f,
                        groundOffset = nextGround,
                        shakeOffsetX = 0f,
                        shakeOffsetY = 0f,
                        skyCycleProgress = cycleProgress,
                        currentPhaseName = phaseName,
                        timePlayedSeconds = timePlayed,
                        particles = updatedParticles
                    )
                }
            }

            GameState.PLAYING -> {
                // Ground scroll
                val baseSpeed = 150f * density * state.difficulty.speedScale
                val nextGround = (state.groundOffset + baseSpeed * dt) % (30f * density)

                // Gravity & bird position
                val gravity = 1350f * density
                var newVelocity = (state.birdVelocity + gravity * dt).coerceAtMost(850f * density)
                var newBirdY = state.birdY + newVelocity * dt

                // Ceiling clamp
                val minCeilingY = state.birdHeight * 0.45f
                if (newBirdY < minCeilingY) {
                    newBirdY = minCeilingY
                    newVelocity = 0f
                }

                // Bird pitch rotation
                val targetRotation = when {
                    newVelocity < 0 -> -22f
                    else -> ((newVelocity / (850f * density)) * 85f).coerceIn(-22f, 75f)
                }
                val newRotation = state.birdRotation + (targetRotation - state.birdRotation) * (dt * 12f).coerceAtMost(1f)

                // Pipe movement & collision detection
                val pipeSpeed = baseSpeed
                val newPipes = ArrayList<PipePair>()
                var scoreIncrement = 0

                val birdLeft = state.birdX - state.birdWidth * 0.36f
                val birdRight = state.birdX + state.birdWidth * 0.36f
                val birdTop = newBirdY - state.birdHeight * 0.36f
                val birdBottom = newBirdY + state.birdHeight * 0.36f

                var collided = false

                // Ground collision
                if (birdBottom >= screenHeight - state.groundHeight) {
                    collided = true
                }

                for (pipe in state.pipes) {
                    val nx = pipe.x - pipeSpeed * dt
                    val pipeRight = nx + pipe.width
                    val gapTop = pipe.gapCenterY - pipe.gapHeight / 2f
                    val gapBottom = pipe.gapCenterY + pipe.gapHeight / 2f

                    // Score check
                    if (!pipe.hasScored && pipeRight < state.birdX) {
                        pipe.hasScored = true
                        scoreIncrement += state.difficulty.scoreMultiplier
                    }

                    // Pipe collision
                    if (!collided) {
                        val overlapX = birdRight > nx && birdLeft < pipeRight
                        if (overlapX) {
                            val inGap = birdTop > gapTop && birdBottom < gapBottom
                            if (!inGap) {
                                collided = true
                            }
                        }
                    }

                    // Keep pipe if still on screen
                    if (pipeRight > -50f) {
                        pipe.x = nx
                        newPipes.add(pipe)
                    }
                }

                // Spawn new pipe
                pipeSpawnTimer += dt
                val spawnInterval = 1.75f / state.difficulty.speedScale
                if (pipeSpawnTimer >= spawnInterval) {
                    pipeSpawnTimer = 0f
                    val pipeW = 62f * density
                    val gapH = 175f * density * state.difficulty.gapScale
                    val minCenterY = gapH / 2f + 50f * density
                    val maxCenterY = screenHeight - state.groundHeight - gapH / 2f - 40f * density
                    val safeMaxCenter = maxCenterY.coerceAtLeast(minCenterY + 10f)
                    val gapCenterY = Random.nextFloat() * (safeMaxCenter - minCenterY) + minCenterY

                    newPipes.add(
                        PipePair(
                            id = ++pipeIdCounter,
                            x = screenWidth + 20f,
                            gapCenterY = gapCenterY,
                            gapHeight = gapH,
                            width = pipeW
                        )
                    )
                }

                if (scoreIncrement > 0) {
                    soundManager.playScore()
                    spawnScoreParticles(state.birdX, newBirdY)
                }

                if (collided) {
                    val dropVel = if (newVelocity < 0) 120f * density else newVelocity
                    triggerGameOver(state.score + scoreIncrement, newBirdY, dropVel, newRotation, newPipes, cycleProgress, phaseName)
                } else {
                    val currentScore = state.score + scoreIncrement
                    val newHigh = currentScore > state.highScore
                    val activeHigh = if (newHigh) currentScore else state.highScore

                    _uiState.update {
                        it.copy(
                            birdY = newBirdY,
                            birdVelocity = newVelocity,
                            birdRotation = newRotation,
                            pipes = newPipes,
                            score = currentScore,
                            highScore = activeHigh,
                            isNewHighScore = newHigh,
                            groundOffset = nextGround,
                            shakeOffsetX = sX,
                            shakeOffsetY = sY,
                            skyCycleProgress = cycleProgress,
                            currentPhaseName = phaseName,
                            timePlayedSeconds = timePlayed,
                            particles = updatedParticles
                        )
                    }
                }
            }

            GameState.PAUSED -> {
                // Do not update physics when paused
            }

            GameState.GAME_OVER -> {
                // Bird falls to ground if in air
                val groundLimit = screenHeight - state.groundHeight - state.birdHeight * 0.35f
                var fallenY = state.birdY
                var fallenVel = state.birdVelocity
                var fallenRot = state.birdRotation

                val isMoving = fallenY < groundLimit || shakeTimer > 0f || state.particles.isNotEmpty()
                if (!isMoving) {
                    return
                }

                if (state.birdY < groundLimit) {
                    val gravity = 1400f * density
                    fallenVel = (state.birdVelocity + gravity * dt).coerceAtMost(900f * density)
                    fallenY = (state.birdY + fallenVel * dt).coerceAtMost(groundLimit)
                    fallenRot = (state.birdRotation + dt * 220f).coerceAtMost(90f)
                }

                _uiState.update {
                    it.copy(
                        birdY = fallenY,
                        birdVelocity = fallenVel,
                        birdRotation = fallenRot,
                        shakeOffsetX = sX,
                        shakeOffsetY = sY,
                        skyCycleProgress = cycleProgress,
                        currentPhaseName = phaseName,
                        particles = updatedParticles
                    )
                }
            }
        }
    }

    private fun triggerGameOver(
        finalScore: Int,
        lastBirdY: Float,
        lastVel: Float,
        lastRot: Float,
        lastPipes: List<PipePair>,
        lastCycleProgress: Float,
        lastPhaseName: String
    ) {
        soundManager.playHit()
        soundManager.triggerCrashHaptic()
        shakeTimer = 0.25f

        val currentHigh = _uiState.value.highScore
        val isNewRecord = finalScore > currentHigh
        val bestScore = if (isNewRecord) finalScore else currentHigh

        if (isNewRecord) {
            prefs.edit().putInt("high_score_${_uiState.value.difficulty.name}", bestScore).apply()
        }

        viewModelScope.launch {
            kotlinx.coroutines.delay(120)
            soundManager.playDie()
        }

        _uiState.update {
            it.copy(
                gameState = GameState.GAME_OVER,
                score = finalScore,
                highScore = bestScore,
                isNewHighScore = isNewRecord,
                birdY = lastBirdY,
                birdVelocity = lastVel,
                birdRotation = lastRot,
                pipes = lastPipes,
                skyCycleProgress = lastCycleProgress,
                currentPhaseName = lastPhaseName
            )
        }
    }

    fun restartGame() {
        val initialBirdX = screenWidth * 0.28f
        val initialBirdY = (screenHeight - _uiState.value.groundHeight) * 0.45f
        readyAnimationTime = 0f
        pipeSpawnTimer = 0f
        timePlayed = 0f

        val (newProg, newPhase) = if (_uiState.value.skyTheme == SkyTheme.RANDOM) {
            generateRandomPhase()
        } else {
            when (_uiState.value.skyTheme) {
                SkyTheme.DAY -> 0.05f to "Siang ☀️"
                SkyTheme.SUNSET -> 0.38f to "Senja 🌅"
                SkyTheme.NIGHT -> 0.65f to "Malam 🌙"
                SkyTheme.RANDOM -> 0.05f to "Siang ☀️"
            }
        }

        _uiState.update {
            it.copy(
                gameState = GameState.READY,
                score = 0,
                isNewHighScore = false,
                birdX = initialBirdX,
                birdY = initialBirdY,
                birdVelocity = 0f,
                birdRotation = 0f,
                pipes = emptyList(),
                shakeOffsetX = 0f,
                shakeOffsetY = 0f,
                skyCycleProgress = newProg,
                currentPhaseName = newPhase,
                timePlayedSeconds = 0f
            )
        }
    }

    fun togglePause() {
        _uiState.update {
            when (it.gameState) {
                GameState.PLAYING -> it.copy(gameState = GameState.PAUSED)
                GameState.PAUSED -> it.copy(gameState = GameState.PLAYING)
                else -> it
            }
        }
    }

    fun setCharacterSkin(skin: CharacterSkin) {
        prefs.edit().putString("skin", skin.name).apply()
        _uiState.update { it.copy(characterSkin = skin) }
    }

    fun setDifficulty(diff: Difficulty) {
        prefs.edit().putString("difficulty", diff.name).apply()
        val savedHigh = prefs.getInt("high_score_${diff.name}", 0)
        _uiState.update {
            it.copy(
                difficulty = diff,
                highScore = savedHigh
            )
        }
        if (_uiState.value.gameState == GameState.GAME_OVER) {
            restartGame()
        }
    }

    fun setSkyTheme(theme: SkyTheme) {
        prefs.edit().putString("theme", theme.name).apply()
        val (prog, phase) = if (theme == SkyTheme.RANDOM) {
            generateRandomPhase()
        } else {
            when (theme) {
                SkyTheme.DAY -> 0.05f to "Siang ☀️"
                SkyTheme.SUNSET -> 0.38f to "Senja 🌅"
                SkyTheme.NIGHT -> 0.65f to "Malam 🌙"
                SkyTheme.RANDOM -> 0.05f to "Siang ☀️"
            }
        }
        _uiState.update {
            it.copy(
                skyTheme = theme,
                skyCycleProgress = prog,
                currentPhaseName = phase
            )
        }
    }

    fun toggleSound() {
        val next = !soundManager.soundEnabled
        soundManager.soundEnabled = next
        _uiState.update { it.copy(soundEnabled = next) }
    }

    fun toggleHaptic() {
        val next = !soundManager.hapticEnabled
        soundManager.hapticEnabled = next
        _uiState.update { it.copy(hapticEnabled = next) }
    }

    private fun spawnScoreParticles(cx: Float, cy: Float) {
        val colors = listOf(0xFFFFD700, 0xFFFFE57F, 0xFFFFFFFF, 0xFFFF8F00)
        val newParticles = (0 until 12).map {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = (Random.nextFloat() * 180f + 80f) * density
            Particle(
                x = cx,
                y = cy,
                vx = kotlin.math.cos(angle) * speed,
                vy = kotlin.math.sin(angle) * speed,
                color = colors[Random.nextInt(colors.size)],
                size = (Random.nextFloat() * 4f + 3f) * density,
                decay = Random.nextFloat() * 0.02f + 0.025f
            )
        }
        _uiState.update { it.copy(particles = it.particles + newParticles) }
    }

    private fun spawnJumpParticles() {
        val bX = _uiState.value.birdX
        val bY = _uiState.value.birdY + _uiState.value.birdHeight * 0.3f
        val newParticles = (0 until 5).map {
            Particle(
                x = bX + (Random.nextFloat() * 16f - 8f) * density,
                y = bY,
                vx = (Random.nextFloat() * 80f - 40f) * density,
                vy = (Random.nextFloat() * 40f + 20f) * density,
                color = 0xAAFFFFFF,
                size = (Random.nextFloat() * 3f + 2f) * density,
                decay = 0.06f
            )
        }
        _uiState.update { it.copy(particles = it.particles + newParticles) }
    }
}
