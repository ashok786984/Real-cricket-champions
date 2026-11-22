package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CareerProfileLocal
import com.example.data.model.GameMode
import com.example.data.model.TournamentFixture
import com.example.data.model.TournamentState
import com.example.engine.CricketCatalog
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.FloodlightMint
import com.example.ui.theme.StadiumObsidian
import com.example.ui.theme.TurfCardElevated
import com.example.ui.theme.TurfCardHigh
import java.util.Locale

@Composable
fun TournamentsAndCareerScreen(
    worldCupState: TournamentState,
    franchiseState: TournamentState,
    offlineCupState: TournamentState,
    careerProfile: CareerProfileLocal,
    onPlayFixture: (GameMode, TournamentFixture) -> Unit,
    onSimulateAi: (GameMode) -> Unit,
    onResetTournament: (GameMode, String) -> Unit,
    onUpgradeCareerSkill: (String) -> Unit,
    onPlayCareerMatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("World Cup", "Franchise", "Offline Cup", "Career Pro")

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { idx, label ->
                FilterChip(
                    selected = activeTab == idx,
                    onClick = { activeTab = idx },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        when (activeTab) {
            0 -> TournamentBracketView(
                tournament = worldCupState,
                onPlayFixture = { onPlayFixture(GameMode.WORLD_CUP, it) },
                onSimulateAi = { onSimulateAi(GameMode.WORLD_CUP) },
                onReset = { onResetTournament(GameMode.WORLD_CUP, worldCupState.userTeamId) }
            )
            1 -> TournamentBracketView(
                tournament = franchiseState,
                onPlayFixture = { onPlayFixture(GameMode.FRANCHISE, it) },
                onSimulateAi = { onSimulateAi(GameMode.FRANCHISE) },
                onReset = { onResetTournament(GameMode.FRANCHISE, franchiseState.userTeamId) }
            )
            2 -> TournamentBracketView(
                tournament = offlineCupState,
                onPlayFixture = { onPlayFixture(GameMode.TOURNAMENT, it) },
                onSimulateAi = { onSimulateAi(GameMode.TOURNAMENT) },
                onReset = { onResetTournament(GameMode.TOURNAMENT, offlineCupState.userTeamId) }
            )
            else -> CareerModeView(
                career = careerProfile,
                onUpgradeSkill = onUpgradeCareerSkill,
                onPlayCareerMatch = onPlayCareerMatch
            )
        }
    }
}

@Composable
private fun TournamentBracketView(
    tournament: TournamentState,
    onPlayFixture: (TournamentFixture) -> Unit,
    onSimulateAi: () -> Unit,
    onReset: () -> Unit
) {
    val userTeam = CricketCatalog.teamById(tournament.userTeamId)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = TurfCardElevated), shape = RoundedCornerShape(20.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_champions_trophy),
                        contentDescription = tournament.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(modifier = Modifier.fillMaxSize().background(StadiumObsidian.copy(alpha = 0.65f)))
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(tournament.title.uppercase(), style = MaterialTheme.typography.headlineLarge, color = ChampionGold)
                        Text("Representing: ${userTeam.name}", style = MaterialTheme.typography.bodyMedium, color = FloodlightMint)
                        if (tournament.championTeamId != null) {
                            val champ = CricketCatalog.teamById(tournament.championTeamId)
                            Text("CHAMPION: ${champ.name}!", style = MaterialTheme.typography.titleMedium, color = ChampionGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onSimulateAi,
                    colors = ButtonDefaults.buttonColors(containerColor = FloodlightMint, contentColor = StadiumObsidian),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sim AI Matches", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New Bracket")
                }
            }
        }

        if (tournament.standings.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = TurfCardElevated)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("FRANCHISE STANDINGS", style = MaterialTheme.typography.labelMedium, color = ChampionGold)
                        tournament.standings.forEachIndexed { idx, row ->
                            val t = CricketCatalog.teamById(row.teamId)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${idx + 1}. ${t.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "P ${row.played} • W ${row.won} • PTS ${row.points} • NRR ${String.format(Locale.US, "%.2f", row.netRunRate)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FloodlightMint
                                )
                            }
                        }
                    }
                }
            }
        }

        items(tournament.fixtures) { fx ->
            val home = CricketCatalog.teamById(fx.homeTeamId)
            val away = CricketCatalog.teamById(fx.awayTeamId)
            val involvesUser = fx.homeTeamId == tournament.userTeamId || fx.awayTeamId == tournament.userTeamId
            Card(
                colors = CardDefaults.cardColors(containerColor = if (involvesUser) TurfCardHigh else TurfCardElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(fx.roundLabel, style = MaterialTheme.typography.labelSmall, color = ChampionGold)
                        Text("${home.name} vs ${away.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(fx.scoreSummary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (fx.winnerTeamId == null && involvesUser) {
                        Button(
                            onClick = { onPlayFixture(fx) },
                            colors = ButtonDefaults.buttonColors(containerColor = FloodlightMint, contentColor = StadiumObsidian)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Text("Play")
                        }
                    } else if (fx.winnerTeamId != null) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Completed", tint = ChampionGold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CareerModeView(
    career: CareerProfileLocal,
    onUpgradeSkill: (String) -> Unit,
    onPlayCareerMatch: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = TurfCardElevated), shape = RoundedCornerShape(20.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.img_captain_avatar),
                        contentDescription = "Career Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(career.playerName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Pro Career Level ${career.level} • ${career.skillPoints} Skill Points", style = MaterialTheme.typography.labelMedium, color = ChampionGold)
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (career.xp / 250f).coerceIn(0f, 1f) },
                            color = FloodlightMint,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = TurfCardElevated)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("SKILL TRAINING LAB", style = MaterialTheme.typography.labelMedium, color = FloodlightMint)
                    SkillUpgradeRow("Batting Power & Timing", career.battingRating, career.skillPoints > 0) { onUpgradeSkill("BAT") }
                    SkillUpgradeRow("Bowling Seam & Spin", career.bowlingRating, career.skillPoints > 0) { onUpgradeSkill("BOWL") }
                    SkillUpgradeRow("Agile Fielding", career.fieldingRating, career.skillPoints > 0) { onUpgradeSkill("FIELD") }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = TurfCardElevated)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatMini("Matches", career.matchesPlayed.toString())
                    StatMini("Runs", career.totalRuns.toString())
                    StatMini("Wickets", career.totalWickets.toString())
                    StatMini("Best", career.highScore.toString())
                }
            }
        }

        item {
            Button(
                onClick = onPlayCareerMatch,
                colors = ButtonDefaults.buttonColors(containerColor = FloodlightMint, contentColor = StadiumObsidian),
                modifier = Modifier.fillMaxWidth().height(54.dp).testTag("play_career_match_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("PLAY NEXT CAREER FIXTURE", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SkillUpgradeRow(
    label: String,
    rating: Int,
    canUpgrade: Boolean,
    onUpgrade: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("Rating: $rating / 99", style = MaterialTheme.typography.labelSmall, color = ChampionGold)
        }
        Surface(color = TurfCardHigh, shape = RoundedCornerShape(10.dp)) {
            Button(
                onClick = onUpgrade,
                enabled = canUpgrade && rating < 99,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Upgrade $label", modifier = Modifier.size(16.dp))
                Text("+2")
            }
        }
    }
}

@Composable
private fun StatMini(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineLarge, color = FloodlightMint)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
