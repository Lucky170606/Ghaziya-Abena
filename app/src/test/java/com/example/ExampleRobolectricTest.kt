package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.Difficulty
import com.example.game.MedalType
import com.example.game.calculateMedal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Flappy Heroes", appName)
    val credit = context.getString(R.string.credit_by)
    assertEquals("Credit by Ghaziya Abena", credit)
  }

  @Test
  fun `test medal calculation`() {
    assertEquals(MedalType.NONE, calculateMedal(0))
    assertEquals(MedalType.BRONZE, calculateMedal(5))
    assertEquals(MedalType.SILVER, calculateMedal(10))
    assertEquals(MedalType.GOLD, calculateMedal(20))
    assertEquals(MedalType.PLATINUM, calculateMedal(35))
  }

  @Test
  fun `test character skins`() {
    val names = com.example.game.CharacterSkin.values().map { it.displayName }
    assertEquals(listOf("Rocky Gibraltar", "Fred Flintstone", "Kamisato Ayaka", "Tanjiro Kamado"), names)
  }

  @Test
  fun `test sky themes`() {
    val themes = com.example.game.SkyTheme.values().map { it.name }
    assertEquals(listOf("RANDOM", "DAY", "SUNSET", "NIGHT"), themes)
  }

  @Test
  fun `test per-difficulty high scores persistence in ViewModel`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val prefs = app.getSharedPreferences("flappy_bird_prefs", Context.MODE_PRIVATE)
    prefs.edit()
      .putInt("high_score_EASY", 12)
      .putInt("high_score_NORMAL", 7)
      .putInt("high_score_HARD", 3)
      .putString("difficulty", Difficulty.NORMAL.name)
      .commit()

    val viewModel = com.example.game.GameViewModel(app)
    val state = viewModel.uiState.value

    assertEquals(Difficulty.NORMAL, state.difficulty)
    assertEquals(7, state.highScore)
    assertEquals(12, state.highScoresByDifficulty[Difficulty.EASY])
    assertEquals(7, state.highScoresByDifficulty[Difficulty.NORMAL])
    assertEquals(3, state.highScoresByDifficulty[Difficulty.HARD])

    // Switch to EASY
    viewModel.setDifficulty(Difficulty.EASY)
    assertEquals(Difficulty.EASY, viewModel.uiState.value.difficulty)
    assertEquals(12, viewModel.uiState.value.highScore)

    // Switch to HARD
    viewModel.setDifficulty(Difficulty.HARD)
    assertEquals(Difficulty.HARD, viewModel.uiState.value.difficulty)
    assertEquals(3, viewModel.uiState.value.highScore)

    // Restart game preserves difficulty high score
    viewModel.restartGame()
    assertEquals(Difficulty.HARD, viewModel.uiState.value.difficulty)
    assertEquals(3, viewModel.uiState.value.highScore)
  }

  @Test
  fun `test pause and resume behavior`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.game.GameViewModel(app)

    assertEquals(com.example.game.GameState.READY, viewModel.uiState.value.gameState)

    // Tap starts the game
    viewModel.onTap()
    assertEquals(com.example.game.GameState.PLAYING, viewModel.uiState.value.gameState)

    // Toggle pause transitions to PAUSED
    viewModel.togglePause()
    assertEquals(com.example.game.GameState.PAUSED, viewModel.uiState.value.gameState)

    // Generic canvas tap while paused must NOT resume or flap
    viewModel.onTap()
    assertEquals(com.example.game.GameState.PAUSED, viewModel.uiState.value.gameState)

    // Explicit togglePause resumes the game
    viewModel.togglePause()
    assertEquals(com.example.game.GameState.PLAYING, viewModel.uiState.value.gameState)
  }

  @Test
  fun `test power-up star collection and hero shooting`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.game.GameViewModel(app)

    viewModel.onScreenSizeChanged(1080f, 2160f, 2.5f)
    viewModel.onTap() // Start playing
    assertEquals(com.example.game.GameState.PLAYING, viewModel.uiState.value.gameState)

    // Initially power up is inactive
    assertFalse(viewModel.uiState.value.powerUpActive)
    assertEquals(0, viewModel.uiState.value.powerUpAmmo)

    // Simulate star close to bird position
    val birdX = viewModel.uiState.value.birdX
    val birdY = viewModel.uiState.value.birdY
    val testStar = com.example.game.PowerUpStar(
      id = 101L,
      x = birdX,
      y = birdY,
      radius = 30f,
      baseCenterY = birdY
    )

    // Manually add star to state and trigger update loop
    val reflectionField = com.example.game.GameViewModel::class.java.getDeclaredField("_uiState").apply {
      isAccessible = true
    }
    @Suppress("UNCHECKED_CAST")
    val stateFlow = reflectionField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<com.example.game.GameUiState>
    stateFlow.value = stateFlow.value.copy(powerUpStars = listOf(testStar))

    viewModel.updateGame(0.016f)

    // Star should be collected and power up activated!
    val updatedState = viewModel.uiState.value
    assertTrue("PowerUp should be active after collecting star", updatedState.powerUpActive)
    assertTrue("Should have ammo", updatedState.powerUpAmmo > 0)

    // Player fires shot
    val initialAmmo = updatedState.powerUpAmmo
    viewModel.onShoot()

    val stateAfterShoot = viewModel.uiState.value
    assertEquals(initialAmmo - 1, stateAfterShoot.powerUpAmmo)
    assertEquals(1, stateAfterShoot.projectiles.size)
  }

  @Test
  fun `test projectile destroys pipe and bird survives through destroyed pipe`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.game.GameViewModel(app)

    viewModel.onScreenSizeChanged(1080f, 2160f, 2.5f)
    viewModel.onTap()

    val reflectionField = com.example.game.GameViewModel::class.java.getDeclaredField("_uiState").apply {
      isAccessible = true
    }
    @Suppress("UNCHECKED_CAST")
    val stateFlow = reflectionField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<com.example.game.GameUiState>

    // Place a pipe directly ahead
    val pipe = com.example.game.PipePair(
      id = 1L,
      x = 400f,
      gapCenterY = 500f,
      gapHeight = 200f,
      width = 120f
    )

    // Place a projectile right about to hit the top pipe obstacle
    val projectile = com.example.game.Projectile(
      id = 99L,
      x = 390f,
      y = 200f, // Above gapTop (which is 400f)
      vx = 1200f,
      radius = 20f,
      colorHex = 0xFFFF4500,
      skin = com.example.game.CharacterSkin.TANJIRO
    )

    stateFlow.value = stateFlow.value.copy(
      pipes = listOf(pipe),
      projectiles = listOf(projectile),
      score = 0
    )

    // Update game physics
    viewModel.updateGame(0.02f)

    val postHitState = viewModel.uiState.value
    val updatedPipe = postHitState.pipes.firstOrNull()

    assertTrue("Pipe should be marked destroyed", updatedPipe?.isDestroyed == true)
    assertTrue("Score should increase from pipe smash bonus", postHitState.score >= 5)
    assertTrue("Pipe debris should be spawned", postHitState.pipeDebris.isNotEmpty())
    assertTrue("Smash texts should be displayed", postHitState.smashTexts.isNotEmpty())

    // Now position bird directly inside the destroyed pipe obstacle area
    stateFlow.value = stateFlow.value.copy(
      birdX = updatedPipe!!.x + 20f,
      birdY = 200f // Inside the destroyed pipe area
    )

    viewModel.updateGame(0.016f)

    // Bird should NOT crash because the pipe was destroyed!
    assertEquals("Game should still be PLAYING because pipe is destroyed", com.example.game.GameState.PLAYING, viewModel.uiState.value.gameState)
  }
}


