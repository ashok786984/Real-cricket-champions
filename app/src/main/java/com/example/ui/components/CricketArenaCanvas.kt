package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.model.GraphicsQuality
import com.example.data.model.MatchPhase
import com.example.data.model.MatchState
import com.example.data.model.TimeOfDay
import com.example.data.model.WeatherCondition
import com.example.engine.CricketPhysics
import com.example.engine.Vec3
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.CricketCrimson
import com.example.ui.theme.FloodlightMint
import com.example.ui.theme.LeatherBallRed
import com.example.ui.theme.LeatherBallWhite
import com.example.ui.theme.OutfieldDark
import com.example.ui.theme.OutfieldLight
import com.example.ui.theme.PitchCrease
import com.example.ui.theme.PitchSoil
import com.example.ui.theme.SkyDayBottom
import com.example.ui.theme.SkyDayTop
import com.example.ui.theme.SkyNightBottom
import com.example.ui.theme.SkyNightTop
import com.example.ui.theme.SkyTwilightBottom
import com.example.ui.theme.SkyTwilightTop
import com.example.ui.theme.StumpWood
import com.example.viewmodel.AnimationFrameState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CricketArenaCanvas(
    matchState: MatchState,
    animState: AnimationFrameState,
    pitchTargetX: Float,
    pitchTargetZ: Float,
    graphicsQuality: GraphicsQuality,
    onPitchAimTap: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "crowd_wave")
    val crowdPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Restart
        ),
        label = "crowd_pulse"
    )

    val isBowlingAimActive = !matchState.isUserCurrentlyBatting && matchState.phase == MatchPhase.PRE_BALL
    val isWhiteBall = !matchState.mode.isMultiInnings

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isBowlingAimActive) {
                if (isBowlingAimActive) {
                    detectTapGestures { offset ->
                        val normX = ((offset.x / size.width) - 0.5f) * 4.5f
                        val normZ = (1f - ((offset.y / size.height - 0.40f) / 0.44f)).coerceIn(0.08f, 0.96f)
                        onPitchAimTap(normX.coerceIn(-1.2f, 1.2f), normZ)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val horizonY = h * 0.34f

        // 1. Sky Gradient
        val (skyTop, skyBottom) = when (matchState.timeOfDay) {
            TimeOfDay.DAY -> SkyDayTop to SkyDayBottom
            TimeOfDay.TWILIGHT -> SkyTwilightTop to SkyTwilightBottom
            TimeOfDay.NIGHT -> SkyNightTop to SkyNightBottom
        }
        drawRect(
            brush = Brush.verticalGradient(listOf(skyTop, skyBottom), startY = 0f, endY = horizonY),
            size = Size(w, horizonY + 4f)
        )

        // 2. Floodlight Towers & Stadium Bowl
        drawStadiumBackdrop(w, h, horizonY, matchState.timeOfDay, crowdPulse, graphicsQuality, matchState)

        // 3. Lush Outfield Turf & Boundary Rope
        drawRect(
            brush = Brush.verticalGradient(
                listOf(OutfieldDark, OutfieldLight, OutfieldDark),
                startY = horizonY,
                endY = h
            ),
            topLeft = Offset(0f, horizonY),
            size = Size(w, h - horizonY)
        )

        // Mowed turf stripes
        if (graphicsQuality != GraphicsQuality.LOW) {
            for (i in 0 until 7) {
                val yTop = horizonY + (h - horizonY) * (i / 7f)
                val bandH = (h - horizonY) / 14f
                drawRect(
                    color = Color.White.copy(alpha = 0.035f),
                    topLeft = Offset(0f, yTop),
                    size = Size(w, bandH)
                )
            }
        }

        // 30-yard inner circle projection
        drawOval(
            color = Color.White.copy(alpha = 0.24f),
            topLeft = Offset(w * 0.14f, horizonY + (h - horizonY) * 0.14f),
            size = Size(w * 0.72f, (h - horizonY) * 0.68f),
            style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
        )

        // 4. 3D-Projected Pitch Strip (z = 0m batter crease -> z = 20.12m bowler crease)
        drawPerspectivePitch(w, h)

        // 5. Bowling Pitch Target Reticle
        if (!matchState.isUserCurrentlyBatting) {
            val targetWorld = Vec3(pitchTargetX * 1.1f, 0.02f, pitchTargetZ * 18f + 1.2f)
            val (tx, ty) = CricketPhysics.projectWorldToScreen(targetWorld)
            val cx = tx * w
            val cy = ty * h
            drawCircle(
                color = FloodlightMint.copy(alpha = 0.25f),
                radius = w * 0.042f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = FloodlightMint,
                radius = w * 0.024f,
                center = Offset(cx, cy),
                style = Stroke(width = 3f)
            )
            drawCircle(color = ChampionGold, radius = 5f, center = Offset(cx, cy))
        }

        // 6. Fielders in Team Colors
        val innings = matchState.currentInnings
        val fielders = CricketPhysics.getFielderPositions(
            matchState.fieldingPreset,
            innings.bowlingTeam.squad
        )
        fielders.forEach { fielder ->
            val (fx, fy) = CricketPhysics.projectWorldToScreen(
                Vec3(fielder.xMeters * 0.45f, 0f, fielder.zMeters * 0.65f + 22f)
            )
            if (fx in 0.04f..0.96f && fy in 0.35f..0.95f) {
                drawPlayerFigure(
                    centerX = fx * w,
                    footY = fy * h,
                    scale = (1f - (fy - 0.34f)).coerceIn(0.42f, 0.85f) * 0.72f,
                    jerseyColor = innings.bowlingTeam.primaryColor,
                    accentColor = innings.bowlingTeam.accentColor,
                    isBatter = false,
                    swingAngle = 0f
                )
            }
        }

        // 7. Bowler End Stumps & Animated Bowler
        drawStumps(w, h, zMeters = 20.12f, broken = false, scaleFactor = 0.58f)
        val bowlerZ = 27f - (animState.bowlerRunup * 7.2f)
        val (bx, by) = CricketPhysics.projectWorldToScreen(Vec3(-0.7f, 0f, bowlerZ))
        drawPlayerFigure(
            centerX = bx * w,
            footY = by * h,
            scale = 0.68f,
            jerseyColor = innings.bowlingTeam.primaryColor,
            accentColor = innings.bowlingTeam.accentColor,
            isBatter = false,
            swingAngle = animState.bowlerRunup * 1.4f
        )

        // 8. Striker End Stumps, Wicketkeeper & Batsman
        val (kx, ky) = CricketPhysics.projectWorldToScreen(Vec3(0.05f, 0f, -3.2f))
        drawPlayerFigure(
            centerX = kx * w,
            footY = ky * h,
            scale = 1.05f,
            jerseyColor = innings.bowlingTeam.primaryColor,
            accentColor = innings.bowlingTeam.accentColor,
            isBatter = false,
            swingAngle = 0f
        )

        drawStumps(w, h, zMeters = 0f, broken = animState.stumpsBroken, scaleFactor = 1.0f)

        val (batX, batY) = CricketPhysics.projectWorldToScreen(Vec3(-0.55f, 0f, 1.1f))
        drawPlayerFigure(
            centerX = batX * w,
            footY = batY * h,
            scale = 1.08f,
            jerseyColor = innings.battingTeam.primaryColor,
            accentColor = innings.battingTeam.accentColor,
            isBatter = true,
            swingAngle = animState.batterSwing
        )

        // 9. Hawkeye Trail & 3D Ball + Ground Shadow
        if (animState.trailPoints.size >= 2) {
            val path = Path()
            animState.trailPoints.forEachIndexed { idx, pt ->
                val (sx, sy) = CricketPhysics.projectWorldToScreen(pt)
                val p = Offset(sx * w, sy * h)
                if (idx == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(
                path = path,
                color = ChampionGold.copy(alpha = 0.55f),
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
        }

        if (animState.ballVisible) {
            val ballPos = animState.ballPos
            val (shadowX, shadowY) = CricketPhysics.projectWorldToScreen(Vec3(ballPos.x, 0f, ballPos.z))
            val (ballSX, ballSY) = CricketPhysics.projectWorldToScreen(ballPos)
            val depthScale = (22f / (ballPos.z + 14f).coerceAtLeast(4f)).coerceIn(0.4f, 1.8f)

            drawOval(
                color = Color.Black.copy(alpha = 0.35f),
                topLeft = Offset(shadowX * w - 7f * depthScale, shadowY * h - 3f * depthScale),
                size = Size(14f * depthScale, 6f * depthScale)
            )
            val ballColor = if (isWhiteBall) LeatherBallWhite else LeatherBallRed
            drawCircle(
                color = ballColor,
                radius = 6.5f * depthScale,
                center = Offset(ballSX * w, ballSY * h)
            )
            drawCircle(
                color = if (isWhiteBall) CricketCrimson else Color.White.copy(alpha = 0.8f),
                radius = 6.5f * depthScale,
                center = Offset(ballSX * w, ballSY * h),
                style = Stroke(width = 1.4f)
            )
        }

        // 10. Weather Particles
        if (matchState.weather == WeatherCondition.HUMID_DEW || matchState.weather == WeatherCondition.OVERCAST) {
            val alpha = if (matchState.weather == WeatherCondition.OVERCAST) 0.16f else 0.08f
            for (i in 0 until 18) {
                val rx = ((i * 73 + (crowdPulse * 30).toInt()) % 100) / 100f * w
                val ry = ((i * 41 + (crowdPulse * 60).toInt()) % 100) / 100f * h
                drawLine(
                    color = Color.White.copy(alpha = alpha),
                    start = Offset(rx, ry),
                    end = Offset(rx - 4f, ry + 14f),
                    strokeWidth = 1.5f
                )
            }
        }
    }
}

private fun DrawScope.drawStadiumBackdrop(
    w: Float,
    h: Float,
    horizonY: Float,
    timeOfDay: TimeOfDay,
    crowdPulse: Float,
    quality: GraphicsQuality,
    matchState: MatchState
) {
    // Upper tier stadium wall
    drawRect(
        color = Color(0xFF0A1B16),
        topLeft = Offset(0f, horizonY - h * 0.085f),
        size = Size(w, h * 0.085f)
    )

    // Animated crowd dots
    val crowdCount = if (quality == GraphicsQuality.LOW) 24 else 48
    val excited = matchState.lastOutcome?.let { it.isBoundaryFour || it.isBoundarySix || it.isWicket } == true
    val amp = if (excited) 5f else 1.5f
    for (i in 0 until crowdCount) {
        val cx = (i + 0.5f) / crowdCount * w
        val cy = horizonY - h * 0.045f + sin(crowdPulse + i * 0.6f) * amp
        val dotColor = when (i % 4) {
            0 -> matchState.userTeam.primaryColor
            1 -> ChampionGold
            2 -> FloodlightMint
            else -> Color.White.copy(alpha = 0.7f)
        }
        drawCircle(color = dotColor.copy(alpha = 0.85f), radius = 3.2f, center = Offset(cx, cy))
    }

    // LED Boundary Ad Perimeter
    drawRect(
        color = Color(0xFF062C22),
        topLeft = Offset(0f, horizonY - 8f),
        size = Size(w, 10f)
    )
    drawLine(
        color = FloodlightMint.copy(alpha = 0.65f),
        start = Offset(0f, horizonY - 8f),
        end = Offset(w, horizonY - 8f),
        strokeWidth = 2f
    )

    // Floodlight towers
    if (timeOfDay != TimeOfDay.DAY) {
        val towers = listOf(w * 0.16f, w * 0.84f)
        towers.forEach { tx ->
            drawLine(
                color = Color(0xFF334155),
                start = Offset(tx, horizonY - h * 0.08f),
                end = Offset(tx, horizonY - h * 0.25f),
                strokeWidth = 5f
            )
            drawRect(
                color = Color(0xFFE2E8F0),
                topLeft = Offset(tx - 16f, horizonY - h * 0.27f),
                size = Size(32f, 12f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(tx, horizonY - h * 0.26f),
                    radius = w * 0.14f
                ),
                radius = w * 0.14f,
                center = Offset(tx, horizonY - h * 0.26f)
            )
        }
    }
}

private fun DrawScope.drawPerspectivePitch(w: Float, h: Float) {
    val (topL_x, topL_y) = CricketPhysics.projectWorldToScreen(Vec3(-1.52f, 0f, 21.5f))
    val (topR_x, topR_y) = CricketPhysics.projectWorldToScreen(Vec3(1.52f, 0f, 21.5f))
    val (botR_x, botR_y) = CricketPhysics.projectWorldToScreen(Vec3(1.52f, 0f, -1.5f))
    val (botL_x, botL_y) = CricketPhysics.projectWorldToScreen(Vec3(-1.52f, 0f, -1.5f))

    val pitchPath = Path().apply {
        moveTo(topL_x * w, topL_y * h)
        lineTo(topR_x * w, topR_y * h)
        lineTo(botR_x * w, botR_y * h)
        lineTo(botL_x * w, botL_y * h)
        close()
    }
    drawPath(pitchPath, color = PitchSoil)

    // Popping creases
    listOf(1.22f, 18.9f).forEach { z ->
        val (clx, cly) = CricketPhysics.projectWorldToScreen(Vec3(-1.65f, 0f, z))
        val (crx, cry) = CricketPhysics.projectWorldToScreen(Vec3(1.65f, 0f, z))
        drawLine(
            color = PitchCrease,
            start = Offset(clx * w, cly * h),
            end = Offset(crx * w, cry * h),
            strokeWidth = 2.5f
        )
    }
}

private fun DrawScope.drawStumps(
    w: Float,
    h: Float,
    zMeters: Float,
    broken: Boolean,
    scaleFactor: Float
) {
    val offsets = listOf(-0.11f, 0f, 0.11f)
    offsets.forEachIndexed { idx, xOff ->
        val tilt = if (broken) (idx - 1) * 0.18f else 0f
        val (bx, by) = CricketPhysics.projectWorldToScreen(Vec3(xOff, 0f, zMeters))
        val (tx, ty) = CricketPhysics.projectWorldToScreen(Vec3(xOff + tilt, 0.71f * scaleFactor, zMeters))
        drawLine(
            color = StumpWood,
            start = Offset(bx * w, by * h),
            end = Offset(tx * w, ty * h),
            strokeWidth = 3.2f * scaleFactor,
            cap = StrokeCap.Round
        )
    }
    if (!broken) {
        val (lx, ly) = CricketPhysics.projectWorldToScreen(Vec3(-0.11f, 0.72f * scaleFactor, zMeters))
        val (rx, ry) = CricketPhysics.projectWorldToScreen(Vec3(0.11f, 0.72f * scaleFactor, zMeters))
        drawLine(
            color = ChampionGold,
            start = Offset(lx * w, ly * h),
            end = Offset(rx * w, ry * h),
            strokeWidth = 2.6f * scaleFactor
        )
    }
}

private fun DrawScope.drawPlayerFigure(
    centerX: Float,
    footY: Float,
    scale: Float,
    jerseyColor: Color,
    accentColor: Color,
    isBatter: Boolean,
    swingAngle: Float
) {
    val bodyH = 44f * scale
    val headR = 7f * scale

    // Shadow
    drawOval(
        color = Color.Black.copy(alpha = 0.25f),
        topLeft = Offset(centerX - 11f * scale, footY - 3f * scale),
        size = Size(22f * scale, 6f * scale)
    )
    // Legs / Pads
    val legColor = if (isBatter) Color(0xFFF1F5F9) else jerseyColor.copy(alpha = 0.85f)
    drawLine(legColor, Offset(centerX - 4f * scale, footY - bodyH * 0.45f), Offset(centerX - 5f * scale, footY), strokeWidth = 5f * scale, cap = StrokeCap.Round)
    drawLine(legColor, Offset(centerX + 4f * scale, footY - bodyH * 0.45f), Offset(centerX + 5f * scale, footY), strokeWidth = 5f * scale, cap = StrokeCap.Round)

    // Torso Jersey
    drawLine(
        color = jerseyColor,
        start = Offset(centerX, footY - bodyH * 0.85f),
        end = Offset(centerX, footY - bodyH * 0.42f),
        strokeWidth = 13f * scale,
        cap = StrokeCap.Round
    )
    // Helmet / Head
    drawCircle(
        color = if (isBatter) accentColor else Color(0xFF1E293B),
        radius = headR,
        center = Offset(centerX, footY - bodyH)
    )

    // Cricket Bat or Bowling Arm
    if (isBatter) {
        val batAngle = -0.3f + swingAngle * 1.6f
        val handX = centerX + 6f * scale
        val handY = footY - bodyH * 0.62f
        val tipX = handX + sin(batAngle) * 26f * scale
        val tipY = handY + cos(batAngle) * 26f * scale
        drawLine(
            color = Color(0xFFE7C582),
            start = Offset(handX, handY),
            end = Offset(tipX, tipY),
            strokeWidth = 4.5f * scale,
            cap = StrokeCap.Round
        )
    } else if (swingAngle > 0f) {
        drawLine(
            color = accentColor,
            start = Offset(centerX + 5f * scale, footY - bodyH * 0.78f),
            end = Offset(centerX + 14f * scale, footY - bodyH * 1.05f),
            strokeWidth = 3.5f * scale,
            cap = StrokeCap.Round
        )
    }
}
