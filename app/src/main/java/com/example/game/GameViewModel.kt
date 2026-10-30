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
    val highScoresByDifficulty: Map<Difficulty, Int> = emptyMap(),
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

    // Screen Shake effect on game over or pipe smash
    val shakeOffsetX: Float = 0f,
    val shakeOffsetY: Float = 0f,

    // Power-Up & Hero Projectiles
    val powerUpActive: Boolean = false,
    val powerUpTimer: Float = 0f,
    val powerUpDuration: Float = 8f,
    val powerUpAmmo: Int = 0,
    val powerUpStars: List<PowerUpStar> = emptyList(),
    val projectiles: List<Projectile> = emptyList(),
    val pipeDebris: List<PipeDebris> = emptyList(),
    val smashTexts: List<SmashText> = emptyList(),

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

    fun getHighScoreForDifficulty(diff: Difficulty): Int {
        return prefs.getInt("high_score_${diff.name}", 0)
    }

    private fun loadAllHighScores(): Map<Difficulty, Int> {
        return Difficulty.values().associateWith { diff ->
            prefs.getInt("high_score_${diff.name}", 0)
        }
    }

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
        val allHighScores = loadAllHighScores()
        val savedHighScore = allHighScores[savedDiff] ?: 0

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
                highScoresByDifficulty = allHighScores,
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
                if (soundManager.soundEnabled) {
                    soundManager.startBgm(getApplication())
                }
                performFlap()
            }
            GameState.PLAYING -> {
                performFlap()
            }
            GameState.PAUSED -> {
                // Do not unpause or flap on canvas tap while paused; player must tap "Lanjutkan" or Pause button
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

                // Power-Up timer update
                var powerUpActive = state.powerUpActive
                var powerUpTimer = state.powerUpTimer
                var powerUpAmmo = state.powerUpAmmo
                if (powerUpActive) {
                    powerUpTimer -= dt
                    if (powerUpTimer <= 0f || powerUpAmmo <= 0) {
                        powerUpActive = false
                        powerUpTimer = 0f
                    }
                }

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

                    // Score check (only for surviving pipes)
                    if (!pipe.hasScored && pipeRight < state.birdX) {
                        pipe.hasScored = true
                        scoreIncrement += state.difficulty.scoreMultiplier
                    }

                    // Pipe collision (skipped if pipe is destroyed by hero projectile!)
                    if (!collided && !pipe.isDestroyed) {
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

                // Power-Up Star collectables update & collision
                val newStars = ArrayList<PowerUpStar>()
                val birdCenterX = state.birdX
                val birdCenterY = newBirdY
                val birdCollectRadius = state.birdWidth * 0.45f
                for (star in state.powerUpStars) {
                    star.x -= pipeSpeed * dt
                    star.floatAngle += dt * 4.5f
                    star.y = star.baseCenterY + sin(star.floatAngle) * (14f * density)

                    if (!star.collected) {
                        val dx = star.x - birdCenterX
                        val dy = star.y - birdCenterY
                        val distSq = dx * dx + dy * dy
                        val hitDist = star.radius + birdCollectRadius
                        if (distSq <= hitDist * hitDist) {
                            star.collected = true
                            if (star.isGold) {
                                powerUpActive = true
                                powerUpTimer = 8.0f
                                powerUpAmmo = (powerUpAmmo + 5).coerceAtMost(10)
                                soundManager.playPowerUp()
                                soundManager.triggerTapHaptic()
                                spawnStarCollectParticles(star.x, star.y)
                            } else {
                                scoreIncrement += 1
                                soundManager.playScore()
                                soundManager.triggerTapHaptic()
                                spawnSilverCollectParticles(star.x, star.y)
                            }
                        }
                    }

                    if (!star.collected && star.x + star.radius > -40f) {
                        newStars.add(star)
                    }
                }

                // Spawn new pipe and optional floating star
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

                    // 50% chance to spawn an item in the gap (75% silver coin, 25% gold power-up star)
                    if (Random.nextFloat() < 0.50f) {
                        val isGold = Random.nextFloat() < 0.25f
                        newStars.add(
                            PowerUpStar(
                                id = System.nanoTime() + Random.nextLong(1000),
                                x = screenWidth + 20f + pipeW / 2f,
                                y = gapCenterY,
                                radius = 18f * density,
                                baseCenterY = gapCenterY,
                                isGold = isGold
                            )
                        )
                    }
                }

                // Projectiles update & pipe destruction collision
                val activeProjectiles = ArrayList<Projectile>()
                val newDebris = ArrayList<PipeDebris>(state.pipeDebris)
                val newSmashTexts = ArrayList<SmashText>(state.smashTexts)

                for (proj in state.projectiles) {
                    proj.x += proj.vx * dt
                    var hit = false

                    for (pipe in newPipes) {
                        if (pipe.isDestroyed) continue
                        val px = pipe.x
                        val pw = pipe.width
                        val gapTop = pipe.gapCenterY - pipe.gapHeight / 2f
                        val gapBottom = pipe.gapCenterY + pipe.gapHeight / 2f

                        // Check projectile collision against pipe
                        if (proj.x + proj.radius >= px && proj.x - proj.radius <= px + pw) {
                            if (proj.y - proj.radius <= gapTop || proj.y + proj.radius >= gapBottom) {
                                hit = true
                                pipe.isDestroyed = true
                                scoreIncrement += 5

                                soundManager.playExplosion()
                                soundManager.triggerCrashHaptic()
                                shakeTimer = 0.22f

                                spawnPipeDebris(pipe, newDebris)
                                spawnExplosionParticles(pipe.x + pipe.width / 2f, proj.y)

                                newSmashTexts.add(
                                    SmashText(
                                        id = System.nanoTime(),
                                        x = pipe.x + pipe.width / 2f,
                                        y = pipe.gapCenterY,
                                        text = "+5 SMASH!"
                                    )
                                )
                                break
                            }
                        }
                    }

                    if (!hit && proj.x - proj.radius < screenWidth + 100f) {
                        activeProjectiles.add(proj)
                    }
                }

                // Pipe Debris physics update
                val updatedDebris = newDebris.mapNotNull { d ->
                    d.x += d.vx * dt
                    d.vy += 1250f * density * dt
                    d.y += d.vy * dt
                    d.rotation += d.rotSpeed * dt
                    d.alpha = (d.alpha - dt * 0.85f).coerceAtLeast(0f)
                    if (d.alpha > 0f && d.y < screenHeight) d else null
                }

                // Smash Text physics update
                val updatedSmashTexts = newSmashTexts.mapNotNull { st ->
                    st.y += st.vy * dt
                    st.alpha = (st.alpha - dt * 1.1f).coerceAtLeast(0f)
                    if (st.alpha > 0f) st else null
                }

                if (scoreIncrement > 0) {
                    soundManager.playScore()
                    spawnScoreParticles(state.birdX, newBirdY)
                }

                val currentScore = state.score + scoreIncrement
                val currentDiff = state.difficulty
                val savedBaselineHigh = getHighScoreForDifficulty(currentDiff)
                val isRecordBroken = currentScore > savedBaselineHigh
                val activeHigh = if (isRecordBroken) currentScore else savedBaselineHigh

                var allScoresMap = state.highScoresByDifficulty
                if (isRecordBroken) {
                    prefs.edit().putInt("high_score_${currentDiff.name}", currentScore).apply()
                    allScoresMap = allScoresMap + (currentDiff to currentScore)
                }

                if (collided) {
                    val dropVel = if (newVelocity < 0) 120f * density else newVelocity
                    triggerGameOver(currentScore, newBirdY, dropVel, newRotation, newPipes, cycleProgress, phaseName)
                } else {
                    _uiState.update {
                        it.copy(
                            birdY = newBirdY,
                            birdVelocity = newVelocity,
                            birdRotation = newRotation,
                            pipes = newPipes,
                            score = currentScore,
                            highScore = activeHigh,
                            highScoresByDifficulty = allScoresMap,
                            isNewHighScore = isRecordBroken,
                            groundOffset = nextGround,
                            shakeOffsetX = sX,
                            shakeOffsetY = sY,
                            skyCycleProgress = cycleProgress,
                            currentPhaseName = phaseName,
                            timePlayedSeconds = timePlayed,
                            particles = updatedParticles,
                            powerUpActive = powerUpActive,
                            powerUpTimer = powerUpTimer,
                            powerUpAmmo = powerUpAmmo,
                            powerUpStars = newStars,
                            projectiles = activeProjectiles,
                            pipeDebris = updatedDebris,
                            smashTexts = updatedSmashTexts
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
        soundManager.playGameOverBgm(getApplication())
        soundManager.playHit()
        soundManager.triggerCrashHaptic()
        shakeTimer = 0.25f

        val currentDiff = _uiState.value.difficulty
        val previousHigh = getHighScoreForDifficulty(currentDiff)
        val isNewRecord = finalScore > previousHigh
        val bestScore = if (isNewRecord) finalScore else previousHigh

        if (isNewRecord) {
            prefs.edit().putInt("high_score_${currentDiff.name}", bestScore).commit()
        }

        val allScores = loadAllHighScores()

        viewModelScope.launch {
            kotlinx.coroutines.delay(120)
            soundManager.playDie()
        }

        _uiState.update {
            it.copy(
                gameState = GameState.GAME_OVER,
                score = finalScore,
                highScore = bestScore,
                highScoresByDifficulty = allScores,
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
        soundManager.stopBgm()
        val initialBirdX = screenWidth * 0.28f
        val initialBirdY = (screenHeight - _uiState.value.groundHeight) * 0.45f
        readyAnimationTime = 0f
        pipeSpawnTimer = 0f
        timePlayed = 0f

        val currentDiff = _uiState.value.difficulty
        val currentSavedHigh = getHighScoreForDifficulty(currentDiff)
        val allHighScores = loadAllHighScores()

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
                highScore = currentSavedHigh,
                highScoresByDifficulty = allHighScores,
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
                timePlayedSeconds = 0f,
                powerUpActive = false,
                powerUpTimer = 0f,
                powerUpAmmo = 0,
                powerUpStars = emptyList(),
                projectiles = emptyList(),
                pipeDebris = emptyList(),
                smashTexts = emptyList()
            )
        }
    }

    fun togglePause() {
        _uiState.update {
            when (it.gameState) {
                GameState.PLAYING -> {
                    soundManager.pauseBgm()
                    it.copy(gameState = GameState.PAUSED)
                }
                GameState.PAUSED -> {
                    soundManager.resumeBgm()
                    it.copy(gameState = GameState.PLAYING)
                }
                else -> it
            }
        }
    }

    fun setCharacterSkin(skin: CharacterSkin) {
        prefs.edit().putString("skin", skin.name).apply()
        _uiState.update { it.copy(characterSkin = skin) }
    }

    fun setDifficulty(diff: Difficulty) {
        prefs.edit().putString("difficulty", diff.name).commit()
        val savedHigh = getHighScoreForDifficulty(diff)
        val allHighScores = loadAllHighScores()
        _uiState.update {
            it.copy(
                difficulty = diff,
                highScore = savedHigh,
                highScoresByDifficulty = allHighScores,
                isNewHighScore = false
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
        if (!next) {
            soundManager.stopBgm()
        } else if (_uiState.value.gameState == GameState.PLAYING) {
            soundManager.startBgm(getApplication())
        }
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

    fun onShoot() {
        val state = _uiState.value
        if (state.gameState != GameState.PLAYING || !state.powerUpActive || state.powerUpAmmo <= 0) return

        soundManager.playShoot()
        soundManager.triggerTapHaptic()

        val projectileColor = when (state.characterSkin) {
            CharacterSkin.TANJIRO -> 0xFFFF4500 // Hinokami Fire (Red/Orange)
            CharacterSkin.FRED -> 0xFFFFD700 // Stone Gold
            CharacterSkin.AYAKA -> 0xFF00E5FF // Frost Cyan
            CharacterSkin.ROCKY -> 0xFFFF9100 // Molten Lava
            CharacterSkin.EMUL -> 0xFF00BFFF // Magic Blue
            CharacterSkin.PINK_PANTHER -> 0xFFFF1493 // Deep Pink
            CharacterSkin.PINK_KNIGHT -> 0xFFFF69B4 // Hot Pink
        }

        val newProj = Projectile(
            id = System.nanoTime(),
            x = state.birdX + state.birdWidth * 0.45f,
            y = state.birdY,
            vx = 1200f * density,
            radius = 14f * density,
            colorHex = projectileColor,
            skin = state.characterSkin
        )

        val remainingAmmo = state.powerUpAmmo - 1
        _uiState.update {
            it.copy(
                projectiles = it.projectiles + newProj,
                powerUpAmmo = remainingAmmo,
                powerUpActive = remainingAmmo > 0 && it.powerUpTimer > 0f
            )
        }
    }

    private fun spawnPipeDebris(pipe: PipePair, targetList: MutableList<PipeDebris>) {
        val colors = listOf(0xFF22C55E, 0xFF16A34A, 0xFF15803D, 0xFF4ADE80, 0xFF166534)
        val gapTop = pipe.gapCenterY - pipe.gapHeight / 2f
        val gapBottom = pipe.gapCenterY + pipe.gapHeight / 2f
        val pw = pipe.width

        // Top pipe chunks (4 chunks)
        val topChunkH = (gapTop / 4f).coerceAtLeast(10f * density)
        for (i in 0 until 4) {
            val yPos = gapTop - (i + 0.5f) * topChunkH
            targetList.add(
                PipeDebris(
                    x = pipe.x + (if (i % 2 == 0) 0f else pw / 2f),
                    y = yPos,
                    vx = (Random.nextFloat() * 420f - 140f) * density,
                    vy = (Random.nextFloat() * -380f - 100f) * density,
                    rotation = Random.nextFloat() * 360f,
                    rotSpeed = (Random.nextFloat() * 480f - 240f),
                    width = pw / 2f,
                    height = topChunkH.coerceAtMost(40f * density),
                    colorHex = colors[Random.nextInt(colors.size)]
                )
            )
        }

        // Bottom pipe chunks (4 chunks)
        val bottomChunkH = 35f * density
        for (i in 0 until 4) {
            val yPos = gapBottom + (i + 0.5f) * bottomChunkH
            targetList.add(
                PipeDebris(
                    x = pipe.x + (if (i % 2 == 0) 0f else pw / 2f),
                    y = yPos,
                    vx = (Random.nextFloat() * 420f - 140f) * density,
                    vy = (Random.nextFloat() * -450f - 120f) * density,
                    rotation = Random.nextFloat() * 360f,
                    rotSpeed = (Random.nextFloat() * 480f - 240f),
                    width = pw / 2f,
                    height = bottomChunkH,
                    colorHex = colors[Random.nextInt(colors.size)]
                )
            )
        }
    }

    private fun spawnExplosionParticles(cx: Float, cy: Float) {
        val colors = listOf(0xFFFF4500, 0xFFFFD700, 0xFFFFFFFF, 0xFFFF8F00, 0xFF4ADE80)
        val newParticles = (0 until 20).map {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = (Random.nextFloat() * 280f + 100f) * density
            Particle(
                x = cx,
                y = cy,
                vx = kotlin.math.cos(angle) * speed,
                vy = kotlin.math.sin(angle) * speed,
                color = colors[Random.nextInt(colors.size)],
                size = (Random.nextFloat() * 6f + 3f) * density,
                decay = Random.nextFloat() * 0.025f + 0.03f
            )
        }
        _uiState.update { it.copy(particles = it.particles + newParticles) }
    }

    private fun spawnStarCollectParticles(cx: Float, cy: Float) {
        val colors = listOf(0xFFFFD700, 0xFFFFF176, 0xFFFFFFFF, 0xFFFFAB00)
        val newParticles = (0 until 14).map {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = (Random.nextFloat() * 200f + 80f) * density
            Particle(
                x = cx,
                y = cy,
                vx = kotlin.math.cos(angle) * speed,
                vy = kotlin.math.sin(angle) * speed,
                color = colors[Random.nextInt(colors.size)],
                size = (Random.nextFloat() * 5f + 3f) * density,
                decay = 0.04f
            )
        }
        _uiState.update { it.copy(particles = it.particles + newParticles) }
    }

    private fun spawnSilverCollectParticles(cx: Float, cy: Float) {
        val colors = listOf(0xFFC0C0C0, 0xFFE0E0E0, 0xFFFFFFFF, 0xFF90CAF9)
        val newParticles = (0 until 10).map {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = (Random.nextFloat() * 160f + 60f) * density
            Particle(
                x = cx,
                y = cy,
                vx = kotlin.math.cos(angle) * speed,
                vy = kotlin.math.sin(angle) * speed,
                color = colors[Random.nextInt(colors.size)],
                size = (Random.nextFloat() * 4f + 2f) * density,
                decay = 0.045f
            )
        }
        _uiState.update { it.copy(particles = it.particles + newParticles) }
    }
}
