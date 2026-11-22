package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Scoreboard
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BallOutcome
import com.example.data.model.GameMode
import com.example.data.model.GraphicsQuality
import com.example.data.model.MatchState
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.CricketCrimson
import com.example.ui.theme.FloodlightMint
import com.example.ui.theme.PitchSoil
import com.example.ui.theme.StadiumObsidian
import com.example.ui.theme.TurfCardElevated
import com.example.ui.theme.TurfCardHigh
import java.util.Locale

@Composable
fun ScoreboardHud(
    matchState: MatchState,
    onOpenScorecard: () -> Unit,
    onOpenHawkeye: () -> Unit,
    onOpenPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val innings = matchState.currentInnings
    val target = matchState.targetScore
    val rrr = matchState.requiredRunRate

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StadiumObsidian.copy(alpha = 0.88f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(innings.battingTeam.primaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = innings.battingTeam.shortCode,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${innings.runs}/${innings.wickets}",
                                style = MaterialTheme.typography.headlineLarge,
                                color = FloodlightMint,
                                modifier = Modifier.testTag("scoreboard_runs_wickets")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${innings.oversFormatted}/${matchState.oversLimit} ov)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val subLine = if (target != null) {
                            val need = (target - innings.runs).coerceAtLeast(0)
                            val rrrStr = rrr?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
                            "Need $need off ${(matchState.oversLimit * 6 - innings.legalBalls).coerceAtLeast(0)}b • RRR $rrrStr"
                        } else {
                            "CRR ${String.format(Locale.US, "%.2f", innings.runRate)} • ${matchState.weather.label}"
                        }
                        Text(
                            text = subLine,
                            style = MaterialTheme.typography.labelSmall,
                            color = ChampionGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (innings.freeHitPending) {
                        Surface(
                            color = CricketCrimson,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                "FREE HIT",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    IconButton(onClick = onOpenHawkeye, modifier = Modifier.testTag("hawkeye_button")) {
                        Icon(Icons.Default.TrackChanges, contentDescription = "Hawkeye Replay", tint = ChampionGold)
                    }
                    IconButton(onClick = onOpenScorecard, modifier = Modifier.testTag("scorecard_button")) {
                        Icon(Icons.Default.Scoreboard, contentDescription = "Full Scorecard", tint = FloodlightMint)
                    }
                    IconButton(onClick = onOpenPause, modifier = Modifier.testTag("pause_button")) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause Match", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            // Striker, Non-Striker, Bowler & This Over Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val s = innings.striker
                    val ns = innings.nonStriker
                    Text(
                        text = "*${s.player.name} ${s.runs}(${s.balls})  |  ${ns.player.name} ${ns.runs}(${ns.balls})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val b = innings.currentBowler
                    Text(
                        text = "Bowl: ${b.player.name} ${b.wickets}-${b.runsConceded} (${b.oversFormatted})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    innings.currentOverBadges.takeLast(6).forEach { badge ->
                        val bg = when {
                            badge == "W" -> CricketCrimson
                            badge == "6" -> ChampionGold
                            badge == "4" -> FloodlightMint
                            else -> TurfCardHigh
                        }
                        val fg = if (badge == "6" || badge == "4") StadiumObsidian else Color.White
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(bg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(badge, style = MaterialTheme.typography.labelSmall, color = fg)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScorecardDialog(
    matchState: MatchState,
    onDismiss: () -> Unit
) {
    val innings = matchState.currentInnings
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "${innings.battingTeam.name} Scorecard (${innings.runs}/${innings.wickets})",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Text("BATTING", style = MaterialTheme.typography.labelMedium, color = FloodlightMint)
                }
                items(innings.batterStats.filter { it.balls > 0 || it.isOut || it.player.id == innings.striker.player.id || it.player.id == innings.nonStriker.player.id }) { b ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(b.player.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(b.dismissalText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            "${b.runs} (${b.balls}) • ${b.fours}x4 ${b.sixes}x6",
                            style = MaterialTheme.typography.labelMedium,
                            color = ChampionGold
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("BOWLING", style = MaterialTheme.typography.labelMedium, color = FloodlightMint)
                }
                items(innings.bowlerStats.filter { it.legalBalls > 0 || it.runsConceded > 0 }) { bw ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(bw.player.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${bw.oversFormatted} ov • ${bw.wickets}-${bw.runsConceded} (Ec ${String.format(Locale.US, "%.1f", bw.economy)})",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun HawkeyeReplayDialog(
    outcome: BallOutcome?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hawkeye & Pitch Telemetry", style = MaterialTheme.typography.titleLarge) },
        text = {
            if (outcome == null) {
                Text("Bowl at least one delivery to inspect Hawkeye pitch tracking and wagon wheel telemetry.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(colors = CardDefaults.cardColors(containerColor = TurfCardElevated)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Delivery Speed: ${String.format(Locale.US, "%.1f", outcome.speedKmh)} km/h", style = MaterialTheme.typography.labelMedium, color = FloodlightMint)
                            Text("Timing Quality: ${outcome.timingQuality}", style = MaterialTheme.typography.bodyMedium)
                            Text("Shot Distance: ${outcome.distanceMeters.toInt()}m • Max Height: ${String.format(Locale.US, "%.1f", outcome.maxHeightMeters)}m", style = MaterialTheme.typography.bodyMedium)
                            if (outcome.lbwPitchInLine || outcome.lbwImpactInLine || outcome.lbwHittingStumps) {
                                Text(
                                    "DRS Check -> Pitch: ${if (outcome.lbwPitchInLine) "IN LINE" else "OUTSIDE"} | Impact: ${if (outcome.lbwImpactInLine) "IN LINE" else "OUTSIDE"} | Wickets: ${if (outcome.lbwHittingStumps) "HITTING" else "MISSING"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ChampionGold
                                )
                            }
                        }
                    }
                    // Top-down pitch bounce graphic
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PitchSoil.copy(alpha = 0.25f))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    ) {
                        val px = size.width * (0.5f + outcome.pitchX * 0.25f)
                        val py = size.height * (1f - outcome.pitchZ.coerceIn(0.05f, 0.95f))
                        drawLine(Color.White.copy(alpha = 0.5f), Offset(size.width * 0.5f, 0f), Offset(size.width * 0.5f, size.height), strokeWidth = 2f)
                        drawCircle(CricketCrimson, radius = 11f, center = Offset(px, py))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun PauseMenuDialog(
    matchState: MatchState,
    graphicsQuality: GraphicsQuality,
    onResume: () -> Unit,
    onChangeQuality: (GraphicsQuality) -> Unit,
    onDeclareTest: () -> Unit,
    onExitMatch: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onResume,
        title = { Text("Match Paused", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Graphics Preset", style = MaterialTheme.typography.labelMedium, color = FloodlightMint)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GraphicsQuality.entries.forEach { q ->
                        OutlinedButton(
                            onClick = { onChangeQuality(q) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (q == graphicsQuality) FloodlightMint.copy(alpha = 0.2f) else Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(q.name, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                if (matchState.mode == GameMode.TEST && matchState.isUserCurrentlyBatting) {
                    Button(
                        onClick = {
                            onDeclareTest()
                            onResume()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Declare Innings")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onResume) { Text("Resume") }
        },
        dismissButton = {
            TextButton(onClick = onExitMatch) {
                Text("Exit Match", color = CricketCrimson)
            }
        }
    )
}
