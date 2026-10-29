package com.example.game

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.ui.theme.GroundBase
import com.example.ui.theme.GroundBorder
import com.example.ui.theme.GroundGrass
import com.example.ui.theme.GroundGrassDark
import com.example.ui.theme.PipeGreen
import com.example.ui.theme.PipeGreenBorder
import com.example.ui.theme.PipeGreenHighlight
import com.example.ui.theme.PipeGreenShadow
import com.example.ui.theme.SkyDawnBottom
import com.example.ui.theme.SkyDawnMid
import com.example.ui.theme.SkyDawnTop
import com.example.ui.theme.SkyDayBottom
import com.example.ui.theme.SkyDayTop
import com.example.ui.theme.SkyNightBottom
import com.example.ui.theme.SkyNightMid
import com.example.ui.theme.SkyNightTop
import com.example.ui.theme.SkySunsetBottom
import com.example.ui.theme.SkySunsetMid
import com.example.ui.theme.SkySunsetTop
import kotlin.math.sin

@Composable
fun GameCanvas(
    uiState: GameUiState,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    // Load character bitmaps cached in remember
    val characterBitmaps = remember(context) {
        CharacterSkin.values().associateWith { skin ->
            BitmapFactory.decodeResource(context.resources, skin.drawableRes)
        }
    }

    val activeBitmap = characterBitmaps[uiState.characterSkin] ?: characterBitmaps[CharacterSkin.ROCKY]

    // High performance frame loop for 60fps physics
    LaunchedEffect(uiState.gameState) {
        var lastTimeNanos = 0L
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastTimeNanos != 0L) {
                    val dt = (frameTimeNanos - lastTimeNanos) / 1_000_000_000f
                    viewModel.updateGame(dt)
                }
                lastTimeNanos = frameTimeNanos
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                if (size.width > 0 && size.height > 0) {
                    viewModel.onScreenSizeChanged(size.width.toFloat(), size.height.toFloat(), density)
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            viewModel.onTap()
                        }
                    )
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Apply screen shake translation if active
            translate(left = uiState.shakeOffsetX, top = uiState.shakeOffsetY) {
                // 1. Draw Dynamic Sky Background based on score & time
                drawSkyBackground(
                    progress = uiState.skyCycleProgress,
                    width = canvasWidth,
                    height = canvasHeight,
                    timePlayed = uiState.timePlayedSeconds
                )

                // 2. Draw Distant Parallax Clouds and Hills
                drawDistantScenery(
                    progress = uiState.skyCycleProgress,
                    width = canvasWidth,
                    height = canvasHeight,
                    groundOffset = uiState.groundOffset,
                    groundHeight = uiState.groundHeight
                )

                // 3. Draw Pipes (unbroken ones)
                for (pipe in uiState.pipes) {
                    drawPipePair(pipe, canvasHeight - uiState.groundHeight)
                }

                // 4. Draw Floating Power-Up Stars & Silver Coins
                for (star in uiState.powerUpStars) {
                    if (star.isGold) {
                        drawPowerUpStar(star)
                    } else {
                        drawSilverCoin(star)
                    }
                }

                // 5. Draw Pipe Shatter Debris Blocks
                for (debris in uiState.pipeDebris) {
                    drawPipeDebris(debris)
                }

                // 6. Draw Animated Ground
                drawGround(canvasWidth, canvasHeight, uiState.groundHeight, uiState.groundOffset)

                // 7. Draw Hero Aura (if Power-Up is active)
                if (uiState.powerUpActive) {
                    drawHeroAura(uiState.birdX, uiState.birdY, uiState.birdWidth, uiState.birdHeight)
                }

                // 8. Draw Character Bird Sprite
                drawCharacter(
                    bitmap = activeBitmap,
                    x = uiState.birdX,
                    y = uiState.birdY,
                    width = uiState.birdWidth,
                    height = uiState.birdHeight,
                    rotation = uiState.birdRotation
                )

                // 9. Draw Hero Projectiles
                for (proj in uiState.projectiles) {
                    drawProjectile(proj)
                }

                // 10. Draw Particles
                for (p in uiState.particles) {
                    drawCircle(
                        color = Color(p.color).copy(alpha = p.alpha),
                        radius = p.size,
                        center = Offset(p.x, p.y)
                    )
                }

                // 11. Draw Smash Texts (+5 SMASH!)
                for (st in uiState.smashTexts) {
                    drawSmashText(st, density)
                }
            }
        }
    }
}

private fun calculateSkyColors(progress: Float): Triple<Color, Color, Color> {
    return when {
        progress < 0.25f -> {
            // Day Phase
            val f = progress / 0.25f
            Triple(
                SkyDayTop,
                lerp(SkyDayTop, SkyDayBottom, 0.45f + f * 0.1f),
                SkyDayBottom
            )
        }
        progress < 0.50f -> {
            // Transition Day -> Sunset
            val f = (progress - 0.25f) / 0.25f
            Triple(
                lerp(SkyDayTop, SkySunsetTop, f),
                lerp(SkyDayBottom, SkySunsetMid, f),
                lerp(SkyDayBottom, SkySunsetBottom, f)
            )
        }
        progress < 0.75f -> {
            // Transition Sunset -> Night
            val f = (progress - 0.50f) / 0.25f
            Triple(
                lerp(SkySunsetTop, SkyNightTop, f),
                lerp(SkySunsetMid, SkyNightMid, f),
                lerp(SkySunsetBottom, SkyNightBottom, f)
            )
        }
        else -> {
            // Transition Night -> Dawn -> Day
            val f = (progress - 0.75f) / 0.25f
            Triple(
                lerp(SkyNightTop, SkyDawnTop, f),
                lerp(SkyNightMid, SkyDawnMid, f),
                lerp(SkyNightBottom, SkyDawnBottom, f)
            )
        }
    }
}

private fun DrawScope.drawSkyBackground(
    progress: Float,
    width: Float,
    height: Float,
    timePlayed: Float
) {
    val (topColor, midColor, bottomColor) = calculateSkyColors(progress)

    val brush = Brush.verticalGradient(
        colors = listOf(topColor, midColor, bottomColor),
        startY = 0f,
        endY = height
    )
    drawRect(brush = brush, size = Size(width, height))

    // 1. STARS: fade in during dusk & night (0.42 .. 0.86)
    val starAlpha = when {
        progress < 0.42f -> 0f
        progress in 0.42f..0.52f -> (progress - 0.42f) / 0.10f
        progress in 0.52f..0.76f -> 1f
        progress in 0.76f..0.86f -> 1f - (progress - 0.76f) / 0.10f
        else -> 0f
    }
    if (starAlpha > 0f) {
        val stars = listOf(
            Offset(width * 0.12f, height * 0.08f),
            Offset(width * 0.28f, height * 0.14f),
            Offset(width * 0.48f, height * 0.06f),
            Offset(width * 0.65f, height * 0.11f),
            Offset(width * 0.85f, height * 0.09f),
            Offset(width * 0.20f, height * 0.22f),
            Offset(width * 0.42f, height * 0.26f),
            Offset(width * 0.75f, height * 0.20f),
            Offset(width * 0.90f, height * 0.25f),
            Offset(width * 0.58f, height * 0.16f),
            Offset(width * 0.08f, height * 0.18f),
            Offset(width * 0.35f, height * 0.05f)
        )
        for ((idx, star) in stars.withIndex()) {
            val twinkle = (sin(timePlayed * 3.5f + idx * 1.7f) * 0.35f + 0.65f).coerceIn(0.2f, 1f)
            drawCircle(
                color = Color.White.copy(alpha = (starAlpha * twinkle).coerceIn(0f, 1f)),
                radius = if (idx % 3 == 0) 3.5f else 2.2f,
                center = star
            )
        }
    }

    // 2. SUN: Arc across the sky during Day & Sunset (progress 0.84 .. 1.0 & 0.0 .. 0.54)
    val sunAlpha = when {
        progress in 0.84f..0.96f -> (progress - 0.84f) / 0.12f
        progress < 0.44f -> 1f
        progress in 0.44f..0.54f -> 1f - (progress - 0.44f) / 0.10f
        else -> 0f
    }
    if (sunAlpha > 0f) {
        val sunProgress = when {
            progress >= 0.84f -> (progress - 0.84f) / 0.70f
            else -> (progress + 0.16f) / 0.70f
        }.coerceIn(0f, 1f)

        val sunX = width * 0.82f - (sunProgress * width * 0.20f)
        val sunY = height * 0.12f + (sunProgress * height * 0.18f)

        val isSunset = progress in 0.25f..0.55f
        val sunGlow = if (isSunset) Color(0x55FFA726) else Color(0x33FFFFFF)
        val sunCore = if (isSunset) Color(0xFFFF7043) else Color(0xFFFFFAED)

        drawCircle(
            color = sunGlow.copy(alpha = sunGlow.alpha * sunAlpha),
            radius = 52f,
            center = Offset(sunX, sunY)
        )
        drawCircle(
            color = sunCore.copy(alpha = sunAlpha),
            radius = 36f,
            center = Offset(sunX, sunY)
        )
    }

    // 3. MOON: Crescent Moon during Night (progress 0.48 .. 0.86)
    val moonAlpha = when {
        progress < 0.48f -> 0f
        progress in 0.48f..0.56f -> (progress - 0.48f) / 0.08f
        progress in 0.56f..0.78f -> 1f
        progress in 0.78f..0.86f -> 1f - (progress - 0.78f) / 0.08f
        else -> 0f
    }
    if (moonAlpha > 0f) {
        val moonX = width * 0.80f
        val moonY = height * 0.16f

        // Outer glow
        drawCircle(
            color = Color(0x33FFFDE7).copy(alpha = 0.22f * moonAlpha),
            radius = 42f,
            center = Offset(moonX, moonY)
        )
        // Main silver moon body
        drawCircle(
            color = Color(0xFFFFFDE7).copy(alpha = moonAlpha),
            radius = 28f,
            center = Offset(moonX, moonY)
        )
        // Crescent cutout (masked with sky background top color)
        drawCircle(
            color = topColor.copy(alpha = moonAlpha),
            radius = 24f,
            center = Offset(moonX - 9f, moonY - 3f)
        )
    }
}

private fun DrawScope.drawDistantScenery(
    progress: Float,
    width: Float,
    height: Float,
    groundOffset: Float,
    groundHeight: Float
) {
    val horizonY = height - groundHeight

    // Dynamic cloud color tinting
    val cloudColor = when {
        progress < 0.25f -> Color(0x77FFFFFF)
        progress < 0.50f -> {
            val f = (progress - 0.25f) / 0.25f
            lerp(Color(0x77FFFFFF), Color(0x66FFAB91), f)
        }
        progress < 0.75f -> {
            val f = (progress - 0.50f) / 0.25f
            lerp(Color(0x66FFAB91), Color(0x3378909C), f)
        }
        else -> {
            val f = (progress - 0.75f) / 0.25f
            lerp(Color(0x3378909C), Color(0x77FFFFFF), f)
        }
    }

    // Parallax clouds moving slowly
    val cloudOffset = (groundOffset * 0.3f) % (width + 200f)
    val c1x = (width * 0.2f - cloudOffset + width + 200f) % (width + 200f) - 100f
    val c2x = (width * 0.7f - cloudOffset + width + 200f) % (width + 200f) - 100f

    drawCloud(c1x, horizonY * 0.35f, 60f, cloudColor)
    drawCloud(c2x, horizonY * 0.48f, 80f, cloudColor)

    // Dynamic distant mountain skyline
    val mountainColor = when {
        progress < 0.25f -> Color(0x334CAF50)
        progress < 0.50f -> {
            val f = (progress - 0.25f) / 0.25f
            lerp(Color(0x334CAF50), Color(0x444A148C), f)
        }
        progress < 0.75f -> {
            val f = (progress - 0.50f) / 0.25f
            lerp(Color(0x444A148C), Color(0x441A237E), f)
        }
        else -> {
            val f = (progress - 0.75f) / 0.25f
            lerp(Color(0x441A237E), Color(0x334CAF50), f)
        }
    }

    val mountainPath = Path().apply {
        moveTo(0f, horizonY)
        lineTo(0f, horizonY - 60f)
        lineTo(width * 0.25f, horizonY - 110f)
        lineTo(width * 0.5f, horizonY - 70f)
        lineTo(width * 0.75f, horizonY - 130f)
        lineTo(width, horizonY - 50f)
        lineTo(width, horizonY)
        close()
    }
    drawPath(path = mountainPath, color = mountainColor)
}

private fun DrawScope.drawCloud(cx: Float, cy: Float, radius: Float, color: Color) {
    drawCircle(color = color, radius = radius * 0.7f, center = Offset(cx - radius * 0.6f, cy))
    drawCircle(color = color, radius = radius, center = Offset(cx, cy - radius * 0.2f))
    drawCircle(color = color, radius = radius * 0.8f, center = Offset(cx + radius * 0.7f, cy))
}

private fun DrawScope.drawPipePair(pipe: PipePair, groundY: Float) {
    if (pipe.isDestroyed) return

    val px = pipe.x
    val pw = pipe.width
    val gapTop = pipe.gapCenterY - pipe.gapHeight / 2f
    val gapBottom = pipe.gapCenterY + pipe.gapHeight / 2f
    val lipHeight = 26f
    val lipOverlap = 4f

    // 1. TOP PIPE (hanging from top of screen to gapTop)
    if (gapTop > 0) {
        // Main pipe shaft
        val topShaftRect = Rect(px, 0f, px + pw, gapTop - lipHeight)
        drawPipeSection(topShaftRect)

        // Top pipe rim / lip
        val topLipRect = Rect(px - lipOverlap, gapTop - lipHeight, px + pw + lipOverlap, gapTop)
        drawPipeLip(topLipRect)
    }

    // 2. BOTTOM PIPE (from gapBottom down to groundY)
    if (gapBottom < groundY) {
        // Bottom pipe rim / lip
        val bottomLipRect = Rect(px - lipOverlap, gapBottom, px + pw + lipOverlap, gapBottom + lipHeight)
        drawPipeLip(bottomLipRect)

        // Bottom pipe shaft
        val bottomShaftRect = Rect(px, gapBottom + lipHeight, px + pw, groundY)
        drawPipeSection(bottomShaftRect)
    }
}

private fun DrawScope.drawPipeSection(rect: Rect) {
    if (rect.height <= 0 || rect.width <= 0) return

    // Base green
    drawRect(
        color = PipeGreen,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height)
    )

    // Left highlight stripe
    drawRect(
        color = PipeGreenHighlight,
        topLeft = Offset(rect.left + rect.width * 0.12f, rect.top),
        size = Size(rect.width * 0.18f, rect.height)
    )

    // Right shadow
    drawRect(
        color = PipeGreenShadow,
        topLeft = Offset(rect.left + rect.width * 0.65f, rect.top),
        size = Size(rect.width * 0.35f, rect.height)
    )

    // Dark outlines
    drawRect(
        color = PipeGreenBorder,
        topLeft = Offset(rect.left, rect.top),
        size = Size(2.5f, rect.height)
    )
    drawRect(
        color = PipeGreenBorder,
        topLeft = Offset(rect.right - 2.5f, rect.top),
        size = Size(2.5f, rect.height)
    )
}

private fun DrawScope.drawPipeLip(rect: Rect) {
    // Pipe Cap / Rim
    drawRect(
        color = PipeGreen,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height)
    )

    // Highlight
    drawRect(
        color = PipeGreenHighlight,
        topLeft = Offset(rect.left + rect.width * 0.12f, rect.top),
        size = Size(rect.width * 0.18f, rect.height)
    )

    // Shadow
    drawRect(
        color = PipeGreenShadow,
        topLeft = Offset(rect.left + rect.width * 0.65f, rect.top),
        size = Size(rect.width * 0.35f, rect.height)
    )

    // Lip border box
    drawRect(
        color = PipeGreenBorder,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, 3f)
    )
    drawRect(
        color = PipeGreenBorder,
        topLeft = Offset(rect.left, rect.bottom - 3f),
        size = Size(rect.width, 3f)
    )
    drawRect(
        color = PipeGreenBorder,
        topLeft = Offset(rect.left, rect.top),
        size = Size(3f, rect.height)
    )
    drawRect(
        color = PipeGreenBorder,
        topLeft = Offset(rect.right - 3f, rect.top),
        size = Size(3f, rect.height)
    )
}

private fun DrawScope.drawGround(width: Float, height: Float, groundHeight: Float, groundOffset: Float) {
    val groundTop = height - groundHeight

    // 1. Sand/dirt base
    drawRect(
        color = GroundBase,
        topLeft = Offset(0f, groundTop),
        size = Size(width, groundHeight)
    )

    // 2. Animated ground stripes
    val stripeWidth = 24f
    val totalStripes = ((width + stripeWidth * 4) / stripeWidth).toInt()
    val startX = -(groundOffset % (stripeWidth * 2))

    for (i in -1..totalStripes) {
        val sx = startX + i * stripeWidth * 2
        val path = Path().apply {
            moveTo(sx, groundTop + 18f)
            lineTo(sx + 14f, groundTop + 18f)
            lineTo(sx + 6f, height)
            lineTo(sx - 8f, height)
            close()
        }
        drawPath(path = path, color = Color(0x228D6E63))
    }

    // 3. Top Grass strip
    drawRect(
        color = GroundGrass,
        topLeft = Offset(0f, groundTop),
        size = Size(width, 18f)
    )

    // Grass blade triangles
    val bladeWidth = 16f
    val bladeCount = (width / bladeWidth).toInt() + 2
    for (i in 0..bladeCount) {
        val bx = i * bladeWidth - (groundOffset % bladeWidth)
        val grassBlade = Path().apply {
            moveTo(bx, groundTop + 18f)
            lineTo(bx + bladeWidth / 2f, groundTop + 24f)
            lineTo(bx + bladeWidth, groundTop + 18f)
            close()
        }
        drawPath(path = grassBlade, color = GroundGrassDark)
    }

    // 4. Ground top outline
    drawRect(
        color = GroundBorder,
        topLeft = Offset(0f, groundTop),
        size = Size(width, 3.5f)
    )
}

private fun DrawScope.drawCharacter(
    bitmap: Bitmap?,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    rotation: Float
) {
    if (bitmap == null) return

    val imageBitmap = bitmap.asImageBitmap()

    rotate(degrees = rotation, pivot = Offset(x, y)) {
        // Character drop shadow for depth
        drawOval(
            color = Color(0x33000000),
            topLeft = Offset(x - width * 0.45f, y + height * 0.45f),
            size = Size(width * 0.9f, height * 0.18f)
        )

        // Draw character sprite centered at (x, y)
        drawImage(
            image = imageBitmap,
            dstOffset = IntOffset((x - width / 2f).toInt(), (y - height / 2f).toInt()),
            dstSize = IntSize(width.toInt(), height.toInt())
        )
    }
}

private fun DrawScope.drawPowerUpStar(star: PowerUpStar) {
    val cx = star.x
    val cy = star.y
    val r = star.radius

    // Outer glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x99FFD700), Color(0x33FFA000), Color.Transparent),
            center = Offset(cx, cy),
            radius = r * 1.8f
        ),
        radius = r * 1.8f,
        center = Offset(cx, cy)
    )

    // Inner bright base
    drawCircle(
        color = Color(0xFFFFD700),
        radius = r * 0.95f,
        center = Offset(cx, cy)
    )

    // 5-pointed star polygon
    val starPath = Path()
    val points = 5
    val outerR = r * 0.9f
    val innerR = r * 0.45f
    val startAngle = -Math.PI / 2.0
    for (i in 0 until points * 2) {
        val currR = if (i % 2 == 0) outerR else innerR
        val angle = startAngle + i * Math.PI / points
        val px = (cx + currR * kotlin.math.cos(angle)).toFloat()
        val py = (cy + currR * kotlin.math.sin(angle)).toFloat()
        if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
    }
    starPath.close()

    drawPath(path = starPath, color = Color(0xFFFFF9C4))

    // White core glint
    drawCircle(
        color = Color.White,
        radius = r * 0.28f,
        center = Offset(cx - r * 0.2f, cy - r * 0.2f)
    )
}

private fun DrawScope.drawHeroAura(bx: Float, by: Float, bw: Float, bh: Float) {
    val auraRadius = (bw.coerceAtLeast(bh)) * 0.75f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x88FFD700), Color(0x55FF6D00), Color.Transparent),
            center = Offset(bx, by),
            radius = auraRadius
        ),
        radius = auraRadius,
        center = Offset(bx, by)
    )
    drawCircle(
        color = Color(0xAAFFF176),
        radius = auraRadius * 0.65f,
        center = Offset(bx, by),
        style = Stroke(width = 3.5f)
    )
}

private fun DrawScope.drawProjectile(proj: Projectile) {
    val cx = proj.x
    val cy = proj.y
    val r = proj.radius
    val projColor = Color(proj.colorHex)

    // Flame / Laser Trail behind projectile
    val tailLength = r * 3.2f
    val trailPath = Path().apply {
        moveTo(cx, cy - r * 0.8f)
        lineTo(cx - tailLength, cy)
        lineTo(cx, cy + r * 0.8f)
        close()
    }
    drawPath(
        path = trailPath,
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, projColor.copy(alpha = 0.85f)),
            startX = cx - tailLength,
            endX = cx
        )
    )

    // Outer glow
    drawCircle(
        color = projColor.copy(alpha = 0.5f),
        radius = r * 1.5f,
        center = Offset(cx, cy)
    )

    // Core body
    drawCircle(
        color = projColor,
        radius = r,
        center = Offset(cx, cy)
    )

    // Hot center highlight
    drawCircle(
        color = Color.White,
        radius = r * 0.45f,
        center = Offset(cx + r * 0.2f, cy)
    )
}

private fun DrawScope.drawPipeDebris(debris: PipeDebris) {
    rotate(degrees = debris.rotation, pivot = Offset(debris.x, debris.y)) {
        val baseColor = Color(debris.colorHex).copy(alpha = debris.alpha)
        val borderColor = Color(0xFF0F381E).copy(alpha = debris.alpha)
        val w = debris.width
        val h = debris.height
        val left = debris.x - w / 2f
        val top = debris.y - h / 2f

        drawRect(
            color = baseColor,
            topLeft = Offset(left, top),
            size = Size(w, h)
        )
        // Shading bevel
        drawRect(
            color = Color.White.copy(alpha = 0.35f * debris.alpha),
            topLeft = Offset(left, top),
            size = Size(w * 0.3f, h)
        )
        // Outline border
        drawRect(
            color = borderColor,
            topLeft = Offset(left, top),
            size = Size(w, h),
            style = Stroke(width = 2.5f)
        )
    }
}

private fun DrawScope.drawSmashText(st: SmashText, density: Float) {
    drawContext.canvas.nativeCanvas.apply {
        val textPaint = Paint().apply {
            textSize = 22f * density
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            alpha = (st.alpha * 255).toInt().coerceIn(0, 255)
        }

        // Draw dark outline
        textPaint.style = Paint.Style.STROKE
        textPaint.strokeWidth = 4f * density
        textPaint.color = android.graphics.Color.BLACK
        textPaint.alpha = (st.alpha * 255).toInt().coerceIn(0, 255)
        drawText(st.text, st.x, st.y, textPaint)

        // Draw golden fill
        textPaint.style = Paint.Style.FILL
        textPaint.color = android.graphics.Color.rgb(255, 215, 0)
        textPaint.alpha = (st.alpha * 255).toInt().coerceIn(0, 255)
        drawText(st.text, st.x, st.y, textPaint)
    }
}


private fun DrawScope.drawSilverCoin(star: PowerUpStar) {
    val cx = star.x
    val cy = star.y
    val r = star.radius

    // Outer glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x66C0C0C0), Color(0x22E0E0E0), Color.Transparent),
            center = Offset(cx, cy),
            radius = r * 1.6f
        ),
        radius = r * 1.6f,
        center = Offset(cx, cy)
    )

    // Silver coin base circle
    drawCircle(
        color = Color(0xFFB0BEC5),
        radius = r * 1.0f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = Color(0xFFECEFF1),
        radius = r * 0.9f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = Color(0xFFCFD8DC),
        radius = r * 0.75f,
        center = Offset(cx, cy),
        style = Stroke(width = 2f)
    )

    drawContext.canvas.nativeCanvas.apply {
        val textPaint = Paint().apply {
            textSize = r * 1.1f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            color = android.graphics.Color.rgb(90, 105, 120)
        }
        drawText("¢", cx, cy + r * 0.38f, textPaint)
    }

    // White glint highlight
    drawCircle(
        color = Color.White,
        radius = r * 0.25f,
        center = Offset(cx - r * 0.25f, cy - r * 0.25f)
    )
}
