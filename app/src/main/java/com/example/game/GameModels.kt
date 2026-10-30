package com.example.game

import androidx.annotation.DrawableRes
import com.example.R

enum class GameState {
    READY,
    PLAYING,
    PAUSED,
    GAME_OVER
}

enum class Difficulty(
    val title: String,
    val gapScale: Float,
    val speedScale: Float,
    val scoreMultiplier: Int
) {
    EASY("Santai", 1.25f, 0.85f, 1),
    NORMAL("Normal", 1.0f, 1.0f, 1),
    HARD("Menantang", 0.82f, 1.2f, 2)
}

enum class CharacterSkin(
    val displayName: String,
    @DrawableRes val drawableRes: Int
) {
    ROCKY("Rocky Gibraltar", R.drawable.character_rocky),
    FRED("Fred Flintstone", R.drawable.character_fred),
    AYAKA("Kamisato Ayaka", R.drawable.character_ayaka),
    TANJIRO("Tanjiro Kamado", R.drawable.character_tanjiro),
    EMUL("Emul", R.drawable.character_emul),
    PINK_PANTHER("Pink Panther", R.drawable.character_pink_panther),
    PINK_KNIGHT("Rune Knight", R.drawable.character_pink_knight)
}

enum class SkyTheme(val title: String) {
    RANDOM("Acak (Random)"),
    DAY("Siang"),
    SUNSET("Senja"),
    NIGHT("Malam")
}

data class PipePair(
    val id: Long,
    var x: Float,
    val gapCenterY: Float,
    val gapHeight: Float,
    val width: Float,
    var hasScored: Boolean = false,
    var isDestroyed: Boolean = false
)

data class PowerUpStar(
    val id: Long,
    var x: Float,
    var y: Float,
    val radius: Float,
    val baseCenterY: Float,
    var floatAngle: Float = 0f,
    var collected: Boolean = false,
    val isGold: Boolean = true
)

data class Projectile(
    val id: Long,
    var x: Float,
    var y: Float,
    var vx: Float,
    val radius: Float,
    val colorHex: Long,
    val skin: CharacterSkin
)

data class PipeDebris(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotation: Float,
    var rotSpeed: Float,
    val width: Float,
    val height: Float,
    val colorHex: Long,
    var alpha: Float = 1.0f
)

data class SmashText(
    val id: Long,
    var x: Float,
    var y: Float,
    val text: String,
    var alpha: Float = 1.0f,
    var vy: Float = -60f
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float = 1.0f,
    val color: Long,
    val size: Float,
    val decay: Float = 0.03f
)

enum class MedalType(val title: String, val minScore: Int, val colorHex: Long) {
    NONE("Belum Ada", 0, 0xFF888888),
    BRONZE("Perunggu", 5, 0xFFCD7F32),
    SILVER("Perak", 10, 0xFFC0C0C0),
    GOLD("Emas", 20, 0xFFFFD700),
    PLATINUM("Platinum", 35, 0xFFE5E4E2)
}

fun calculateMedal(score: Int): MedalType {
    return when {
        score >= MedalType.PLATINUM.minScore -> MedalType.PLATINUM
        score >= MedalType.GOLD.minScore -> MedalType.GOLD
        score >= MedalType.SILVER.minScore -> MedalType.SILVER
        score >= MedalType.BRONZE.minScore -> MedalType.BRONZE
        else -> MedalType.NONE
    }
}
