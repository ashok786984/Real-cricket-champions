package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import com.example.data.model.GraphicsQuality
import com.example.data.model.LeaderboardEntry
import com.example.data.model.MatchHistoryRecord
import com.example.data.model.UserProfile
import com.example.data.repository.AppSettings
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.CricketCrimson
import com.example.ui.theme.FloodlightMint
import com.example.ui.theme.StadiumObsidian
import com.example.ui.theme.TurfCardElevated
import com.example.ui.theme.TurfCardHigh
import com.example.viewmodel.UiState

@Composable
fun ProfileAndSettingsScreen(
    isAuthenticated: Boolean,
    displayName: String,
    userProfileState: UiState<UserProfile?>,
    localCareer: CareerProfileLocal,
    leaderboardState: UiState<List<LeaderboardEntry>>,
    matchHistoryState: UiState<List<MatchHistoryRecord>>,
    offlineMatchHistory: List<MatchHistoryRecord>,
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var subTab by remember { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = subTab == 0, onClick = { subTab = 0 }, label = { Text("Profile & History") }, modifier = Modifier.weight(1f))
            FilterChip(selected = subTab == 1, onClick = { subTab = 1 }, label = { Text("Leaderboard") }, modifier = Modifier.weight(1f))
            FilterChip(selected = subTab == 2, onClick = { subTab = 2 }, label = { Text("Settings") }, modifier = Modifier.weight(1f))
        }

        when (subTab) {
            0 -> ProfileHistoryTab(
                isAuthenticated = isAuthenticated,
                displayName = displayName,
                userProfileState = userProfileState,
                localCareer = localCareer,
                matchHistoryState = matchHistoryState,
                offlineMatchHistory = offlineMatchHistory,
                onSignIn = onSignIn
            )
            1 -> LeaderboardTab(
                isAuthenticated = isAuthenticated,
                leaderboardState = leaderboardState,
                onSignIn = onSignIn
            )
            else -> SettingsTab(
                isAuthenticated = isAuthenticated,
                settings = settings,
                onUpdateSettings = onUpdateSettings,
                onSignIn = onSignIn,
                onSignOut = onSignOut
            )
        }
    }
}

@Composable
private fun ProfileHistoryTab(
    isAuthenticated: Boolean,
    displayName: String,
    userProfileState: UiState<UserProfile?>,
    localCareer: CareerProfileLocal,
    matchHistoryState: UiState<List<MatchHistoryRecord>>,
    offlineMatchHistory: List<MatchHistoryRecord>,
    onSignIn: () -> Unit
) {
    val cloud = (userProfileState as? UiState.Success)?.data
    val matches = cloud?.matchesPlayed?.toInt() ?: localCareer.matchesPlayed
    val wins = cloud?.matchesWon?.toInt() ?: localCareer.matchesWon
    val runs = cloud?.totalRuns?.toInt() ?: localCareer.totalRuns
    val wickets = cloud?.totalWickets?.toInt() ?: localCareer.totalWickets
    val rating = cloud?.rating?.toInt() ?: 1000

    val historyItems = if (isAuthenticated && matchHistoryState is UiState.Success) {
        matchHistoryState.data
    } else {
        offlineMatchHistory
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = TurfCardElevated), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_captain_avatar),
                            contentDescription = "Player Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(64.dp).clip(CircleShape)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cloud?.displayName ?: displayName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(
                                if (isAuthenticated) "Cloud Synced • Rating $rating" else "Offline Guest Profile",
                                style = MaterialTheme.typography.labelMedium,
                                color = FloodlightMint
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ProfileStat("Matches", "$matches")
                        ProfileStat("Wins", "$wins")
                        ProfileStat("Runs", "$runs")
                        ProfileStat("Wickets", "$wickets")
                    }
                    if (!isAuthenticated) {
                        Button(
                            onClick = onSignIn,
                            colors = ButtonDefaults.buttonColors(containerColor = FloodlightMint, contentColor = StadiumObsidian),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Link Google Account for Cloud Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, tint = ChampionGold)
                Spacer(Modifier.width(6.dp))
                Text("RECENT MATCH HISTORY", style = MaterialTheme.typography.labelMedium, color = ChampionGold)
            }
        }

        if (historyItems.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = TurfCardElevated)) {
                    Text(
                        "No completed matches recorded yet. Play any match to populate your career archive!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(historyItems) { rec ->
                Card(colors = CardDefaults.cardColors(containerColor = TurfCardElevated), shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${rec.mode} • ${rec.stadiumName}", style = MaterialTheme.typography.labelSmall, color = ChampionGold)
                            Text(if (rec.won) "WON" else "PLAYED", style = MaterialTheme.typography.labelSmall, color = FloodlightMint)
                        }
                        Text("${rec.homeTeam} (${rec.homeScore}) vs ${rec.awayTeam} (${rec.awayScore})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(rec.resultSummary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardTab(
    isAuthenticated: Boolean,
    leaderboardState: UiState<List<LeaderboardEntry>>,
    onSignIn: () -> Unit
) {
    if (!isAuthenticated) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Leaderboard, contentDescription = null, tint = ChampionGold, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(10.dp))
            Text("Global Champions Leaderboard", style = MaterialTheme.typography.titleLarge)
            Text("Sign in with Google to view and compete on the live cloud leaderboard.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onSignIn) { Text("Sign in with Google") }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (leaderboardState) {
            is UiState.Loading -> item { CircularProgressIndicator() }
            is UiState.Error -> item { Text(leaderboardState.message, color = MaterialTheme.colorScheme.error) }
            is UiState.Success -> {
                if (leaderboardState.data.isEmpty()) {
                    item { Text("No leaderboard entries yet. Complete a match to claim #1!", style = MaterialTheme.typography.bodyMedium) }
                } else {
                    itemsIndexed(leaderboardState.data) { idx, entry ->
                        Card(colors = CardDefaults.cardColors(containerColor = if (idx == 0) TurfCardHigh else TurfCardElevated)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("#${idx + 1}", style = MaterialTheme.typography.headlineLarge, color = ChampionGold)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("${entry.displayName} (${entry.teamCode})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("${entry.matchesWon} Wins • ${entry.totalRuns} Runs • ${entry.sixesHit} Sixes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (idx == 0) Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = ChampionGold)
                                    Spacer(Modifier.width(6.dp))
                                    Text("${entry.rating} ELO", style = MaterialTheme.typography.labelLarge, color = FloodlightMint)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(
    isAuthenticated: Boolean,
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = FloodlightMint)
                Spacer(Modifier.width(8.dp))
                Text("GRAPHICS & GAMEPLAY SETTINGS", style = MaterialTheme.typography.labelMedium, color = FloodlightMint)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = TurfCardElevated)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Graphics Quality", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GraphicsQuality.entries.forEach { q ->
                            FilterChip(
                                selected = settings.graphicsQuality == q,
                                onClick = { onUpdateSettings { it.copy(graphicsQuality = q) } },
                                label = { Text(q.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Bat Impact Haptics", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = settings.hapticsEnabled, onCheckedChange = { v -> onUpdateSettings { it.copy(hapticsEnabled = v) } })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Stadium Ambience Cues", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = settings.commentarySoundEnabled, onCheckedChange = { v -> onUpdateSettings { it.copy(commentarySoundEnabled = v) } })
                    }
                }
            }
        }
        item {
            if (isAuthenticated) {
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = CricketCrimson)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", color = CricketCrimson)
                }
            } else {
                Button(
                    onClick = onSignIn,
                    colors = ButtonDefaults.buttonColors(containerColor = FloodlightMint, contentColor = StadiumObsidian),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Sign in with Google", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineLarge, color = ChampionGold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
