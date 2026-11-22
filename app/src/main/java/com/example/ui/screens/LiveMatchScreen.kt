package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.DeliveryType
import com.example.data.model.FieldingPreset
import com.example.data.model.GameMode
import com.example.data.model.GraphicsQuality
import com.example.data.model.MatchPhase
import com.example.data.model.MatchState
import com.example.data.model.ShotType
import com.example.ui.components.CricketArenaCanvas
import com.example.ui.components.HawkeyeReplayDialog
import com.example.ui.components.PauseMenuDialog
import com.example.ui.components.ScoreboardHud
import com.example.ui.components.ScorecardDialog
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.CricketCrimson
import com.example.ui.theme.FloodlightMint
import com.example.ui.theme.StadiumObsidian
import com.example.ui.theme.TurfCardElevated
import com.example.ui.theme.TurfCardHigh
import com.example.viewmodel.AnimationFrameState
import com.example.viewmodel.MatchViewModel

@Composable
fun LiveMatchScreen(
    matchViewModel: MatchViewModel,
    graphicsQuality: GraphicsQuality,
    hapticsEnabled: Boolean,
    onChangeQuality: (GraphicsQuality) -> Unit,
    onExitMatch: () -> Unit
) {
    val matchState by matchViewModel.matchState.collectAsStateWithLifecycle()
    val animState by matchViewModel.animState.collectAsStateWithLifecycle()
    val shotType by matchViewModel.selectedShotType.collectAsStateWithLifecycle()
    val shotAngle by matchViewModel.selectedShotAngle.collectAsStateWithLifecycle()
    val deliveryType by matchViewModel.selectedDelivery.collectAsStateWithLifecycle()
    val pitchTargetX by matchViewModel.pitchTargetX.collectAsStateWithLifecycle()
    val pitchTargetZ by matchViewModel.pitchTargetZ.collectAsStateWithLifecycle()
    val isPaused by matchViewModel.isPaused.collectAsStateWithLifecycle()
    val showHawkeye by matchViewModel.showHawkeyeReplay.collectAsStateWithLifecycle()

    var showScorecard by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(matchState.lastOutcome) {
        val outcome = matchState.lastOutcome
        if (hapticsEnabled && outcome != null && (outcome.isBoundarySix || outcome.isBoundaryFour || outcome.isWicket)) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    BackHandler {
        if (matchState.phase == MatchPhase.MATCH_FINISHED) {
            onExitMatch()
        } else {
            matchViewModel.togglePause(true)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(StadiumObsidian)
            .systemBarsPadding()
    ) {
        val isWideLandscape = maxWidth > maxHeight && maxWidth > 620.dp

        if (matchState.phase == MatchPhase.MATCH_FINISHED) {
            MatchResultsView(
                matchState = matchState,
                onViewScorecard = { showScorecard = true },
                onExitMatch = onExitMatch
            )
        } else if (isWideLandscape) {
            Row(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(0.58f).fillMaxHeight().clip(RoundedCornerShape(18.dp))) {
                    CricketArenaCanvas(
                        matchState = matchState,
                        animState = animState,
                        pitchTargetX = pitchTargetX,
                        pitchTargetZ = pitchTargetZ,
                        graphicsQuality = graphicsQuality,
                        onPitchAimTap = { x, z -> matchViewModel.setPitchTarget(x, z) }
                    )
                    CommentaryBanner(
                        text = matchState.commentary,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(0.42f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScoreboardHud(
                        matchState = matchState,
                        onOpenScorecard = { showScorecard = true },
                        onOpenHawkeye = { matchViewModel.toggleHawkeyeReplay(true) },
                        onOpenPause = { matchViewModel.togglePause(true) }
                    )
                    MatchControlsPanel(
                        matchState = matchState,
                        animState = animState,
                        shotType = shotType,
                        shotAngle = shotAngle,
                        deliveryType = deliveryType,
                        matchViewModel = matchViewModel
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScoreboardHud(
                    matchState = matchState,
                    onOpenScorecard = { showScorecard = true },
                    onOpenHawkeye = { matchViewModel.toggleHawkeyeReplay(true) },
                    onOpenPause = { matchViewModel.togglePause(true) }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    CricketArenaCanvas(
                        matchState = matchState,
                        animState = animState,
                        pitchTargetX = pitchTargetX,
                        pitchTargetZ = pitchTargetZ,
                        graphicsQuality = graphicsQuality,
                        onPitchAimTap = { x, z -> matchViewModel.setPitchTarget(x, z) }
                    )
                    CommentaryBanner(
                        text = matchState.commentary,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                    )
                }

                MatchControlsPanel(
                    matchState = matchState,
                    animState = animState,
                    shotType = shotType,
                    shotAngle = shotAngle,
                    deliveryType = deliveryType,
                    matchViewModel = matchViewModel
                )
            }
        }
    }

    if (showScorecard) {
        ScorecardDialog(matchState = matchState, onDismiss = { showScorecard = false })
    }
    if (showHawkeye) {
        HawkeyeReplayDialog(outcome = matchState.lastOutcome, onDismiss = { matchViewModel.toggleHawkeyeReplay(false) })
    }
    if (isPaused) {
        PauseMenuDialog(
            matchState = matchState,
            graphicsQuality = graphicsQuality,
            onResume = { matchViewModel.togglePause(false) },
            onChangeQuality = onChangeQuality,
            onDeclareTest = { matchViewModel.declareTestInnings() },
            onExitMatch = onExitMatch
        )
    }
}

@Composable
private fun CommentaryBanner(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = StadiumObsidian.copy(alpha = 0.85f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun MatchControlsPanel(
    matchState: MatchState,
    animState: AnimationFrameState,
    shotType: ShotType,
    shotAngle: Float,
    deliveryType: DeliveryType,
    matchViewModel: MatchViewModel
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TurfCardElevated),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (matchState.phase) {
                MatchPhase.INNINGS_BREAK -> {
                    Text("INNINGS BREAK", style = MaterialTheme.typography.titleLarge, color = ChampionGold)
                    Text(
                        "Target set: ${(matchState.currentInnings.runs + 1)} runs",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { matchViewModel.startSecondInnings() },
                        colors = ButtonDefaults.buttonColors(containerColor = FloodlightMint, contentColor = StadiumObsidian),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("start_next_innings_button")
                    ) {
                        Text("Start Next Innings", fontWeight = FontWeight.Bold)
                    }
                }
                MatchPhase.SUPER_OVER_PROMPT -> {
                    Text("MATCH TIED — SUPER OVER!", style = MaterialTheme.typography.titleLarge, color = ChampionGold)
                    Button(
                        onClick = { matchViewModel.startSuperOver() },
                        colors = ButtonDefaults.buttonColors(containerColor = ChampionGold, contentColor = StadiumObsidian),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Play Super Over", fontWeight = FontWeight.Bold)
                    }
                }
                MatchPhase.RUNNING_DECISION -> {
                    Text(
                        "Completed ${animState.pendingRunningRuns} run(s) — Push for another?",
                        style = MaterialTheme.typography.titleMedium,
                        color = ChampionGold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { matchViewModel.pushExtraRun() },
                            colors = ButtonDefaults.buttonColors(containerColor = ChampionGold, contentColor = StadiumObsidian),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("push_run_button")
                        ) { Text("PUSH +1 RUN", fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { matchViewModel.holdCrease() },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("hold_crease_button")
                        ) { Text("HOLD CREASE") }
                    }
                }
                else -> {
                    if (matchState.mode == GameMode.PRACTICE && matchState.phase == MatchPhase.PRE_BALL) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Practice Nets Mode", style = MaterialTheme.typography.labelMedium, color = FloodlightMint)
                            OutlinedButton(onClick = { matchViewModel.togglePracticeRole() }) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (matchState.isUserCurrentlyBatting) "Switch to Bowl" else "Switch to Bat")
                            }
                        }
                    }

                    if (matchState.isUserCurrentlyBatting) {
                        BattingControls(
                            matchState = matchState,
                            animState = animState,
                            shotType = shotType,
                            shotAngle = shotAngle,
                            onSelectShot = { matchViewModel.selectShotType(it) },
                            onSelectAngle = { matchViewModel.selectShotAngle(it) },
                            onBowlNext = { matchViewModel.startDelivery() },
                            onSwing = { matchViewModel.tapSwing() }
                        )
                    } else {
                        BowlingControls(
                            matchState = matchState,
                            animState = animState,
                            deliveryType = deliveryType,
                            onSelectDelivery = { matchViewModel.selectDelivery(it) },
                            onSelectPreset = { matchViewModel.setFieldingPreset(it) },
                            onStartBowl = { matchViewModel.startDelivery() },
                            onReleaseBall = { matchViewModel.tapBowlingRelease() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BattingControls(
    matchState: MatchState,
    animState: AnimationFrameState,
    shotType: ShotType,
    shotAngle: Float,
    onSelectShot: (ShotType) -> Unit,
    onSelectAngle: (Float) -> Unit,
    onBowlNext: () -> Unit,
    onSwing: () -> Unit
) {
    // Timing Meter
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("BATTING TIMING BAR", style = MaterialTheme.typography.labelSmall, color = FloodlightMint)
            Text("Direction: ${directionLabel(shotAngle)}", style = MaterialTheme.typography.labelSmall, color = ChampionGold)
        }
        TimingMeterBar(cursor = animState.timingCursor, isCenteredZero = true)
    }

    // Shot Types
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(ShotType.entries) { shot ->
            FilterChip(
                selected = shot == shotType,
                onClick = { onSelectShot(shot) },
                label = { Text(shot.label, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }

    // Wagon Wheel Direction Selector + Primary Action Button
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val directions = listOf(
            -90f to "SQ LEG",
            -40f to "MID WKT",
            0f to "STRAIGHT",
            40f to "COVER",
            90f to "POINT",
            145f to "3RD MAN"
        )
        LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(directions) { (deg, label) ->
                val active = shotAngle == deg
                Surface(
                    color = if (active) ChampionGold else TurfCardHigh,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable { onSelectAngle(deg) }
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) StadiumObsidian else Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            }
        }

        if (matchState.phase == MatchPhase.PRE_BALL) {
            Button(
                onClick = onBowlNext,
                colors = ButtonDefaults.buttonColors(containerColor = FloodlightMint, contentColor = StadiumObsidian),
                modifier = Modifier.height(50.dp).testTag("bowl_next_ball_button")
            ) {
                Icon(Icons.Default.SportsCricket, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("PLAY BALL", fontWeight = FontWeight.Bold)
            }
        } else if (matchState.phase == MatchPhase.BALL_IN_FLIGHT_TO_BAT) {
            Button(
                onClick = onSwing,
                colors = ButtonDefaults.buttonColors(containerColor = ChampionGold, contentColor = StadiumObsidian),
                modifier = Modifier.height(50.dp).testTag("swing_bat_button")
            ) {
                Text("SWING!", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(onClick = {}, enabled = false, modifier = Modifier.height(50.dp)) {
                Text("IN PLAY...")
            }
        }
    }
}

@Composable
private fun BowlingControls(
    matchState: MatchState,
    animState: AnimationFrameState,
    deliveryType: DeliveryType,
    onSelectDelivery: (DeliveryType) -> Unit,
    onSelectPreset: (FieldingPreset) -> Unit,
    onStartBowl: () -> Unit,
    onReleaseBall: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("TAP PITCH ABOVE TO AIM • PACE & ACCURACY METER", style = MaterialTheme.typography.labelSmall, color = FloodlightMint)
        TimingMeterBar(cursor = (animState.paceCursor * 2f - 1f), isCenteredZero = true)
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(DeliveryType.entries) { del ->
            FilterChip(
                selected = del == deliveryType,
                onClick = { onSelectDelivery(del) },
                label = { Text(del.label, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(FieldingPreset.entries) { preset ->
                FilterChip(
                    selected = matchState.fieldingPreset == preset,
                    onClick = { onSelectPreset(preset) },
                    label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        if (matchState.phase == MatchPhase.PRE_BALL) {
            Button(
                onClick = onStartBowl,
                colors = ButtonDefaults.buttonColors(containerColor = FloodlightMint, contentColor = StadiumObsidian),
                modifier = Modifier.height(50.dp).testTag("start_bowl_button")
            ) {
                Icon(Icons.Default.SportsBaseball, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("BOWL", fontWeight = FontWeight.Bold)
            }
        } else if (matchState.phase == MatchPhase.BOWLING_RUNUP) {
            Button(
                onClick = onReleaseBall,
                colors = ButtonDefaults.buttonColors(containerColor = ChampionGold, contentColor = StadiumObsidian),
                modifier = Modifier.height(50.dp).testTag("release_ball_button")
            ) {
                Text("RELEASE!", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(onClick = {}, enabled = false, modifier = Modifier.height(50.dp)) {
                Text("LIVE...")
            }
        }
    }
}

@Composable
private fun TimingMeterBar(cursor: Float, isCenteredZero: Boolean) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(StadiumObsidian)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
    ) {
        val w = size.width
        val h = size.height
        // Amber & Green Sweet Spot
        drawRect(color = ChampionGold.copy(alpha = 0.35f), topLeft = Offset(w * 0.34f, 0f), size = Size(w * 0.32f, h))
        drawRect(color = FloodlightMint.copy(alpha = 0.75f), topLeft = Offset(w * 0.43f, 0f), size = Size(w * 0.14f, h))
        // Moving cursor
        val norm = if (isCenteredZero) ((cursor + 1f) / 2f).coerceIn(0f, 1f) else cursor.coerceIn(0f, 1f)
        val cx = norm * w
        drawLine(Color.White, Offset(cx, 0f), Offset(cx, h), strokeWidth = 5f)
    }
}

@Composable
private fun MatchResultsView(
    matchState: MatchState,
    onViewScorecard: () -> Unit,
    onExitMatch: () -> Unit
) {
    val userWon = matchState.winnerTeamId == matchState.userTeam.id
    val topBatter = matchState.inningsList.flatMap { it.batterStats }.maxByOrNull { it.runs }
    val topBowler = matchState.inningsList.flatMap { it.bowlerStats }.maxByOrNull { it.wickets * 100 - it.runsConceded }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_champions_trophy),
            contentDescription = "Championship Trophy",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(22.dp))
        )

        Surface(
            color = if (userWon) FloodlightMint.copy(alpha = 0.18f) else CricketCrimson.copy(alpha = 0.2f),
            shape = RoundedCornerShape(50)
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = ChampionGold)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (userWon) "VICTORY!" else "MATCH COMPLETE",
                    style = MaterialTheme.typography.labelLarge,
                    color = ChampionGold
                )
            }
        }

        Text(
            text = matchState.resultSummary.ifBlank { "Match Finished" },
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = TurfCardElevated),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                matchState.inningsList.forEach { inn ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(inn.battingTeam.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${inn.runs}/${inn.wickets} (${inn.oversFormatted} ov)", style = MaterialTheme.typography.titleMedium, color = FloodlightMint)
                    }
                }
                if (topBatter != null) {
                    Text("Top Batter: ${topBatter.player.name} — ${topBatter.runs} (${topBatter.balls})", style = MaterialTheme.typography.bodyMedium, color = ChampionGold)
                }
                if (topBowler != null && topBowler.legalBalls > 0) {
                    Text("Best Bowler: ${topBowler.player.name} — ${topBowler.wickets}/${topBowler.runsConceded}", style = MaterialTheme.typography.bodyMedium, color = FloodlightMint)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onViewScorecard, modifier = Modifier.weight(1f).height(52.dp)) {
                Text("Scorecard")
            }
            Button(
                onClick = onExitMatch,
                colors = ButtonDefaults.buttonColors(containerColor = FloodlightMint, contentColor = StadiumObsidian),
                modifier = Modifier.weight(1f).height(52.dp).testTag("exit_results_button")
            ) {
                Text("Continue", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun directionLabel(angle: Float): String = when {
    angle < -60f -> "Square Leg"
    angle < -15f -> "Mid-Wicket"
    angle < 20f -> "Straight"
    angle < 65f -> "Cover"
    angle < 115f -> "Point"
    else -> "Third Man"
}
