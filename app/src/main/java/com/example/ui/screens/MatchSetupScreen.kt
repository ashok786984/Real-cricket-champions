package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CricketTeam
import com.example.data.model.GameMode
import com.example.data.model.TimeOfDay
import com.example.data.model.WeatherCondition
import com.example.engine.CricketCatalog
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.FloodlightMint
import com.example.ui.theme.StadiumObsidian
import com.example.ui.theme.TurfCardElevated
import com.example.ui.theme.TurfCardHigh
import com.example.viewmodel.MatchSetupConfig

@Composable
fun MatchSetupScreen(
    config: MatchSetupConfig,
    onUpdateConfig: ((MatchSetupConfig) -> MatchSetupConfig) -> Unit,
    onFlipCoin: (Boolean) -> Boolean,
    onChooseTossDecision: (Boolean) -> Unit,
    onLaunchMatch: () -> Unit,
    onBack: () -> Unit
) {
    var showSquadPreview by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("setup_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(config.mode.title.uppercase(), style = MaterialTheme.typography.headlineLarge, color = FloodlightMint)
                    Text("Configure squads, stadium, pitch conditions & coin toss", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Team Selection
        item {
            Text("YOUR SQUAD", style = MaterialTheme.typography.labelMedium, color = ChampionGold)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(CricketCatalog.teams) { team ->
                    TeamSelectableCard(
                        team = team,
                        selected = team.id == config.userTeam.id,
                        onClick = {
                            onUpdateConfig { c ->
                                val nextOpp = if (c.opponentTeam.id == team.id) {
                                    CricketCatalog.teams.first { it.id != team.id }
                                } else c.opponentTeam
                                c.copy(userTeam = team, opponentTeam = nextOpp)
                            }
                        }
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("OPPONENT SQUAD", style = MaterialTheme.typography.labelMedium, color = ChampionGold)
                Text(
                    text = if (showSquadPreview) "Hide Playing XI" else "View Playing XI",
                    style = MaterialTheme.typography.labelMedium,
                    color = FloodlightMint,
                    modifier = Modifier.clickable { showSquadPreview = !showSquadPreview }
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(CricketCatalog.teams.filter { it.id != config.userTeam.id }) { team ->
                    TeamSelectableCard(
                        team = team,
                        selected = team.id == config.opponentTeam.id,
                        onClick = { onUpdateConfig { it.copy(opponentTeam = team) } }
                    )
                }
            }
        }

        if (showSquadPreview) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = TurfCardElevated)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${config.userTeam.name} Playing XI", style = MaterialTheme.typography.titleMedium, color = FloodlightMint)
                        config.userTeam.squad.forEachIndexed { idx, p ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${idx + 1}. ${p.name} (${p.role.label})", style = MaterialTheme.typography.bodyMedium)
                                Text("BAT ${p.battingSkill} • BOWL ${p.bowlingSkill}", style = MaterialTheme.typography.labelSmall, color = ChampionGold)
                            }
                        }
                    }
                }
            }
        }

        // Overs & Format
        if (config.mode == GameMode.QUICK || config.mode == GameMode.TOURNAMENT || config.mode == GameMode.CAREER) {
            item {
                Text("OVERS PER INNINGS", style = MaterialTheme.typography.labelMedium, color = ChampionGold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 5, 10, 20).forEach { ov ->
                        FilterChip(
                            selected = config.oversLimit == ov,
                            onClick = { onUpdateConfig { it.copy(oversLimit = ov) } },
                            label = { Text("$ov Overs") }
                        )
                    }
                }
            }
        }

        // Stadium Selection
        item {
            Text("STADIUM & PITCH", style = MaterialTheme.typography.labelMedium, color = ChampionGold)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(CricketCatalog.stadiums) { st ->
                    val selected = st.id == config.stadium.id
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (selected) TurfCardHigh else TurfCardElevated),
                        modifier = Modifier
                            .width(200.dp)
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = if (selected) FloodlightMint else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onUpdateConfig {
                                    it.copy(stadium = st, timeOfDay = st.defaultTime, weather = st.defaultWeather)
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(st.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(st.city, style = MaterialTheme.typography.labelSmall, color = FloodlightMint)
                            Text(
                                "Pace ${(st.pitchPace * 100).toInt()} • Turn ${(st.pitchTurn * 100).toInt()} • ${st.boundaryMeters}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Time of Day & Weather
        item {
            Text("TIME & WEATHER", style = MaterialTheme.typography.labelMedium, color = ChampionGold)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TimeOfDay.entries) { tod ->
                    FilterChip(
                        selected = config.timeOfDay == tod,
                        onClick = { onUpdateConfig { it.copy(timeOfDay = tod) } },
                        label = { Text(tod.label) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(WeatherCondition.entries) { w ->
                    FilterChip(
                        selected = config.weather == w,
                        onClick = { onUpdateConfig { it.copy(weather = w) } },
                        label = { Text(w.label) }
                    )
                }
            }
        }

        // Coin Toss Card
        if (config.mode != GameMode.PRACTICE) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TurfCardElevated),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Casino, contentDescription = null, tint = ChampionGold)
                            Spacer(Modifier.width(8.dp))
                            Text("OFFICIAL COIN TOSS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        if (!config.tossCompleted) {
                            Text("Call Heads or Tails to decide who opens on this pitch.", style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { onFlipCoin(true) },
                                    modifier = Modifier.weight(1f).testTag("toss_heads_button")
                                ) { Text("Call HEADS") }
                                OutlinedButton(
                                    onClick = { onFlipCoin(false) },
                                    modifier = Modifier.weight(1f).testTag("toss_tails_button")
                                ) { Text("Call TAILS") }
                            }
                        } else if (config.userWonToss) {
                            Text("You won the toss! Choose to Bat or Bowl first:", style = MaterialTheme.typography.bodyMedium, color = FloodlightMint)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterChip(
                                    selected = config.userBatsFirst,
                                    onClick = { onChooseTossDecision(true) },
                                    label = { Text("Bat First") },
                                    modifier = Modifier.testTag("choose_bat_first")
                                )
                                FilterChip(
                                    selected = !config.userBatsFirst,
                                    onClick = { onChooseTossDecision(false) },
                                    label = { Text("Bowl First") },
                                    modifier = Modifier.testTag("choose_bowl_first")
                                )
                            }
                        } else {
                            val oppChoice = if (config.userBatsFirst) "bowl first" else "bat first"
                            Text(
                                "${config.opponentTeam.name} won the toss and elected to $oppChoice.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ChampionGold
                            )
                        }
                    }
                }
            }
        }

        // Launch Match CTA
        item {
            Button(
                onClick = onLaunchMatch,
                enabled = config.tossCompleted || config.mode == GameMode.PRACTICE,
                colors = ButtonDefaults.buttonColors(containerColor = FloodlightMint, contentColor = StadiumObsidian),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("start_match_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (config.mode == GameMode.PRACTICE) "ENTER PRACTICE NETS" else "WALK OUT TO THE PITCH",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TeamSelectableCard(
    team: CricketTeam,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (selected) TurfCardHigh else TurfCardElevated),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(148.dp)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) FloodlightMint else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(team.primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(team.shortCode, style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
                Surface(color = StadiumObsidian.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp)) {
                    Text("OVR ${team.teamRating}", style = MaterialTheme.typography.labelSmall, color = ChampionGold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text(team.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
