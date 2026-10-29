package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.CharacterPickerSheet
import com.example.game.GameCanvas
import com.example.game.GameHud
import com.example.game.GameOverScreenOverlay
import com.example.game.GameState
import com.example.game.GameViewModel
import com.example.game.PauseScreenOverlay
import com.example.game.ReadyScreenOverlay
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FlappyBirdApp()
                }
            }
        }
    }
}

@Composable
fun FlappyBirdApp(
    viewModel: GameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCharacterPicker by remember { mutableStateOf(false) }
    val composeContext = androidx.compose.ui.platform.LocalContext.current
    var backPressedTime by remember { mutableStateOf(0L) }

    // Android Back button handling
    BackHandler(enabled = true) {
        when {
            showCharacterPicker -> {
                showCharacterPicker = false
            }
            uiState.gameState == GameState.PLAYING -> {
                viewModel.togglePause()
            }
            uiState.gameState == GameState.PAUSED -> {
                viewModel.togglePause()
            }
            uiState.gameState == GameState.GAME_OVER -> {
                viewModel.restartGame()
            }
            uiState.gameState == GameState.READY -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - backPressedTime < 2000L) {
                    (composeContext as? android.app.Activity)?.finish()
                } else {
                    backPressedTime = currentTime
                    android.widget.Toast.makeText(composeContext, "Tekan sekali lagi untuk keluar aplikasi", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Game Canvas: physics, pipes, ground, sky, and character
        GameCanvas(
            uiState = uiState,
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Game Overlays
        when (uiState.gameState) {
            GameState.READY -> {
                ReadyScreenOverlay(
                    uiState = uiState,
                    viewModel = viewModel,
                    onOpenCharacterPicker = { showCharacterPicker = true }
                )
            }
            GameState.PAUSED -> {
                PauseScreenOverlay(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
            GameState.GAME_OVER -> {
                GameOverScreenOverlay(
                    uiState = uiState,
                    viewModel = viewModel,
                    onOpenCharacterPicker = { showCharacterPicker = true }
                )
            }
            GameState.PLAYING -> {
                // In-game: clean canvas with HUD
            }
        }

        // 3. HUD: Controls, Sound, Theme, Scores (Top Layer - Always accessible)
        GameHud(
            uiState = uiState,
            viewModel = viewModel,
            onOpenCharacterPicker = { showCharacterPicker = true }
        )

        // 4. Character Selection Bottom Sheet
        if (showCharacterPicker) {
            CharacterPickerSheet(
                uiState = uiState,
                viewModel = viewModel,
                onDismiss = { showCharacterPicker = false }
            )
        }
    }
}
