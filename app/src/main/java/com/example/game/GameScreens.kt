package com.example.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ArcadeGold
import com.example.ui.theme.ArcadeOrange
import com.example.ui.theme.ArcadeRed
import com.example.ui.theme.PipeGreen
import com.example.ui.theme.PipeGreenBorder
import com.example.ui.theme.TextBorder
import com.example.ui.theme.TextGold
import kotlinx.coroutines.launch

@Composable
fun GameHud(
    uiState: GameUiState,
    viewModel: GameViewModel,
    onOpenCharacterPicker: () -> Unit
) {
    if (uiState.gameState == GameState.GAME_OVER) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Top Navigation / Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Action: Prominent Pause button during PLAYING / PAUSED, or Character Picker Icon during READY
            if (uiState.gameState == GameState.PLAYING || uiState.gameState == GameState.PAUSED) {
                Surface(
                    onClick = { viewModel.togglePause() },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xEE1E293B),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(ArcadeGold, ArcadeOrange))
                    ),
                    modifier = Modifier
                        .testTag("pause_button")
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.gameState == GameState.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Jeda atau Lanjutkan Permainan",
                            tint = ArcadeGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (uiState.gameState == GameState.PAUSED) "LANJUT" else "JEDA",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Character Picker Icon
                IconButton(
                    onClick = onOpenCharacterPicker,
                    modifier = Modifier
                        .testTag("character_picker_button")
                        .size(44.dp)
                        .background(Color(0xCC000000), CircleShape)
                        .border(2.dp, ArcadeGold, CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = uiState.characterSkin.drawableRes),
                        contentDescription = "Pilih Karakter",
                        modifier = Modifier.size(30.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Center: In-Game Score (during PLAYING or PAUSED)
            if (uiState.gameState == GameState.PLAYING || uiState.gameState == GameState.PAUSED) {
                ArcadeScoreText(
                    score = uiState.score,
                    modifier = Modifier.testTag("in_game_score")
                )
            }

            // Right Action Controls: Sound & Theme toggle
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = { viewModel.toggleSound() },
                    modifier = Modifier
                        .testTag("sound_toggle_button")
                        .size(44.dp)
                        .background(Color(0xCC000000), CircleShape)
                        .border(2.dp, Color(0x66FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = if (uiState.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = "Suara",
                        tint = if (uiState.soundEnabled) Color.White else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val nextTheme = when (uiState.skyTheme) {
                            SkyTheme.RANDOM -> SkyTheme.DAY
                            SkyTheme.DAY -> SkyTheme.SUNSET
                            SkyTheme.SUNSET -> SkyTheme.NIGHT
                            SkyTheme.NIGHT -> SkyTheme.RANDOM
                        }
                        viewModel.setSkyTheme(nextTheme)
                    },
                    modifier = Modifier
                        .testTag("theme_toggle_button")
                        .size(44.dp)
                        .background(Color(0xCC000000), CircleShape)
                        .border(
                            2.dp,
                            if (uiState.skyTheme == SkyTheme.RANDOM) ArcadeGold else Color(0x66FFFFFF),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = when (uiState.skyTheme) {
                            SkyTheme.RANDOM -> Icons.Default.Shuffle
                            SkyTheme.DAY -> Icons.Default.LightMode
                            SkyTheme.SUNSET -> Icons.Default.WbTwilight
                            SkyTheme.NIGHT -> Icons.Default.DarkMode
                        },
                        contentDescription = "Tema Langit: ${uiState.currentPhaseName}",
                        tint = when (uiState.skyTheme) {
                            SkyTheme.RANDOM -> ArcadeGold
                            SkyTheme.DAY -> Color(0xFFFFD54F)
                            SkyTheme.SUNSET -> Color(0xFFFF8A65)
                            SkyTheme.NIGHT -> Color(0xFF90CAF9)
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Power-Up Timer & Ammo Bar
        if (uiState.powerUpActive && (uiState.gameState == GameState.PLAYING || uiState.gameState == GameState.PAUSED)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xEE1E1B4B),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(ArcadeGold, ArcadeOrange))
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 58.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚡ JURUS AKTIF",
                        color = ArcadeGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "• ${String.format(java.util.Locale.US, "%.1f", uiState.powerUpTimer)}s",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ArcadeRed,
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        Text(
                            text = "x${uiState.powerUpAmmo}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Floating Shoot Button at bottom right during PLAYING
        if (uiState.powerUpActive && uiState.gameState == GameState.PLAYING) {
            Button(
                onClick = { viewModel.onShoot() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                modifier = Modifier
                    .testTag("shoot_button")
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 48.dp)
                    .background(
                        brush = Brush.horizontalGradient(listOf(ArcadeRed, ArcadeOrange)),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(2.dp, ArcadeGold, RoundedCornerShape(24.dp))
                    .shadow(8.dp, RoundedCornerShape(24.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "💥 TEMBAK",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = "${uiState.powerUpAmmo}",
                            color = ArcadeGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReadyScreenOverlay(
    uiState: GameUiState,
    viewModel: GameViewModel,
    onOpenCharacterPicker: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_ready")
    val tapPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tap_pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 70.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { viewModel.onTap() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Retro Flappy Bird Title
            RetroTitle()

            Spacer(modifier = Modifier.height(14.dp))

            // Prominent Credit Banner
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEE1E293B)),
                modifier = Modifier
                    .border(2.dp, ArcadeGold, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "⭐ " + stringResource(id = R.string.credit_by) + " ⭐",
                        color = ArcadeGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Animated Tap Guide
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(tapPulse)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xCC000000),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.radialGradient(listOf(ArcadeGold, ArcadeOrange))),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Ketuk untuk Terbang",
                            tint = ArcadeGold,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(id = R.string.tap_to_fly),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(Color(0x99000000), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Character, Difficulty & Time Setup Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xDD000000)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 390.dp)
                    .border(1.5.dp, Color(0x66FFFFFF), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Active Character Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x33FFFFFF))
                            .clickable { onOpenCharacterPicker() }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = uiState.characterSkin.drawableRes),
                                contentDescription = uiState.characterSkin.displayName,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Karakter Aktif",
                                    color = Color.LightGray,
                                    fontSize = 8.sp
                                )
                                Text(
                                    text = uiState.characterSkin.displayName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ArcadeGold
                        ) {
                            Text(
                                text = "GANTI >",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Difficulty Selector
                    Text(
                        text = "TINGKAT KESULITAN",
                        color = ArcadeGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Difficulty.values().forEach { diff ->
                            val isSelected = uiState.difficulty == diff
                            val diffHighScore = uiState.highScoresByDifficulty[diff] ?: 0
                            val desc = when(diff) {
                                Difficulty.EASY -> "Pelan"
                                Difficulty.NORMAL -> "Standar"
                                Difficulty.HARD -> "Cepat"
                            }
                            Button(
                                onClick = { viewModel.setDifficulty(diff) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) ArcadeGold else Color(0x33FFFFFF),
                                    contentColor = if (isSelected) Color.Black else Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("difficulty_${diff.name.lowercase()}")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = diff.title, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(text = desc, fontSize = 7.sp, color = if (isSelected) Color(0xFF3E2723) else Color.LightGray)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "🏆 $diffHighScore",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF5D4037) else ArcadeGold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Direct Time / Sky Theme Selector
                    Text(
                        text = "PILIH WAKTU & TEMA LANGIT",
                        color = ArcadeGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val themes = listOf(
                            SkyTheme.DAY to "Siang ☀️",
                            SkyTheme.SUNSET to "Senja 🌅",
                            SkyTheme.NIGHT to "Malam 🌙",
                            SkyTheme.RANDOM to "Acak 🔀"
                        )
                        themes.forEach { (theme, label) ->
                            val isSelected = uiState.skyTheme == theme
                            Button(
                                onClick = { viewModel.setSkyTheme(theme) },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) ArcadeOrange else Color(0x22FFFFFF),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 8.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Best Score display for selected difficulty
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x33FF9100),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ArcadeOrange)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Skor Tertinggi (${uiState.difficulty.title})",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "🏆 ${uiState.highScore}",
                                color = ArcadeGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameOverScreenOverlay(
    uiState: GameUiState,
    viewModel: GameViewModel,
    onOpenCharacterPicker: () -> Unit
) {
    val medal = calculateMedal(uiState.score)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { /* Consume taps on overlay background */ }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
        ) {
            // "GAME OVER" Banner
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ArcadeRed),
                modifier = Modifier
                    .shadow(12.dp, RoundedCornerShape(12.dp))
                    .border(3.dp, Color(0xFF7F0000), RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = stringResource(id = R.string.game_over),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Scoreboard Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF21E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.5.dp, ArcadeGold, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.isNewHighScore) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ArcadeOrange,
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            Text(
                                text = "🏆 REKOR BARU TERCIPTA! 🏆",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Medal badge
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "MEDALI",
                                color = Color.LightGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color(medal.colorHex),
                                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.radialGradient(listOf(Color.White, Color(medal.colorHex)))),
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = when (medal) {
                                            MedalType.PLATINUM -> "💎"
                                            MedalType.GOLD -> "🥇"
                                            MedalType.SILVER -> "🥈"
                                            MedalType.BRONZE -> "🥉"
                                            MedalType.NONE -> "—"
                                        },
                                        fontSize = 22.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = medal.title,
                                color = Color.White,
                                fontSize = 8.sp
                            )
                        }

                        // Scores
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "SKOR",
                                color = Color.LightGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.score}",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "TERTINGGI (${uiState.difficulty.title.uppercase()})",
                                color = ArcadeGold,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.highScore}",
                                color = ArcadeGold,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // All Difficulties High Scores Breakdown
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x33000000), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                            .padding(vertical = 6.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Difficulty.values().forEach { diff ->
                            val diffScore = uiState.highScoresByDifficulty[diff] ?: 0
                            val isCurrent = uiState.difficulty == diff
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = diff.title,
                                    fontSize = 8.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) ArcadeGold else Color.LightGray
                                )
                                Text(
                                    text = "🏆 $diffScore",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) ArcadeOrange else Color.White
                                )
                            }
                        }
                    }

                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Action Buttons
            Button(
                onClick = { viewModel.restartGame() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PipeGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(2.dp, PipeGreenBorder, RoundedCornerShape(12.dp))
                    .testTag("restart_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.restart).uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Change Character Button
            OutlinedButton(
                onClick = onOpenCharacterPicker,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("game_over_change_character_button")
            ) {
                Image(
                    painter = painterResource(id = uiState.characterSkin.drawableRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GANTI KARAKTER",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PauseScreenOverlay(
    uiState: GameUiState,
    viewModel: GameViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { /* Consume taps so clicking outside the dialog does not unpause or flap */ }
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF21E293B)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .border(2.5.dp, ArcadeGold, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = null,
                        tint = ArcadeGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PERMAINAN DIJEDA",
                        color = ArcadeGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats preview while paused
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x33000000),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(ArcadeGold, ArcadeOrange))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "SKOR SAAT INI", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${uiState.score}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "REKOR (${uiState.difficulty.title.uppercase()})", color = ArcadeGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${uiState.highScore}", color = ArcadeGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.togglePause() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PipeGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("resume_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "LANJUTKAN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { viewModel.restartGame() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pause_restart_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "MULAI ULANG", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterPickerSheet(
    uiState: GameUiState,
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E293B)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PILIH KARAKTER",
                color = ArcadeGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Pilih karakter favorit untuk terbang!",
                color = Color.LightGray,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            CharacterSkin.values().forEach { skin ->
                val isSelected = uiState.characterSkin == skin
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0x33FFD700) else Color(0x22FFFFFF)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) ArcadeGold else Color(0x33FFFFFF),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            viewModel.setCharacterSkin(skin)
                            scope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        }
                        .testTag("skin_item_${skin.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x33000000),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = skin.drawableRes),
                                    contentDescription = skin.displayName,
                                    modifier = Modifier.size(50.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = skin.displayName,
                            color = if (isSelected) ArcadeGold else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        if (isSelected) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ArcadeGold
                            ) {
                                Text(
                                    text = "DIPAKAI",
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun RetroTitle() {
    val title = stringResource(id = R.string.game_title)
    Box(contentAlignment = Alignment.Center) {
        // Drop shadow text
        Text(
            text = title,
            color = Color(0xFF1E3A5F),
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )
        // Main retro yellow text
        Text(
            text = title,
            color = TextGold,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun ArcadeScoreText(score: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Black outline shadow
        Text(
            text = "$score",
            color = Color.Black,
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )
        // Bright white/gold score
        Text(
            text = "$score",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Black
        )
    }
}
