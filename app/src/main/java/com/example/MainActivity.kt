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

    // Android Back button handling
    BackHandler(enabled = true) {
        when (uiState.gameState) {
            GameState.PLAYING -> viewModel.togglePause()
            GameState.PAUSED -> viewModel.togglePause()
            GameState.GAME_OVER -> viewModel.restartGame()
            GameState.READY -> {
                // Allow default back to close app
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

        // 2. HUD: Controls, Sound, Theme, Scores
        GameHud(
            uiState = uiState,
            viewModel = viewModel,
            onOpenCharacterPicker = { showCharacterPicker = true }
        )

        // 3. Game Overlays
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
