package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CareerProfileLocal
import com.example.data.model.GameMode
import com.example.data.model.UserProfile
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.FloodlightMint
import com.example.ui.theme.StadiumObsidian
import com.example.ui.theme.TurfCardElevated
import com.example.ui.theme.TurfCardHigh

@Composable
fun HomeHubScreen(
    displayName: String,
    isAuthenticated: Boolean,
    cloudProfile: UserProfile?,
    localCareer: CareerProfileLocal,
    onSelectMode: (GameMode) -> Unit,
    onRequestGoogleSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val level = cloudProfile?.careerLevel?.toInt() ?: localCareer.level
    val totalRuns = cloudProfile?.totalRuns?.toInt() ?: localCareer.totalRuns
    val wins = cloudProfile?.matchesWon?.toInt() ?: localCareer.matchesWon

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Bar
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.img_captain_avatar),
                        contentDescription = "Captain Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "WELCOME BACK, ${displayName.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = FloodlightMint
                        )
                        Text(
                            text = "REAL CRICKET CHAMPIONS",
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                }
                Surface(
                    color = ChampionGold.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("LVL $level", style = MaterialTheme.typography.labelMedium, color = ChampionGold)
                        Text("$wins Wins • $totalRuns Runs", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Guest Banner if offline
        if (!isAuthenticated) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TurfCardHigh),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Playing Offline (Guest)", style = MaterialTheme.typography.titleMedium, color = ChampionGold)
                            Text("Sign in with Google for Cloud Saves & Online 1v1.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = onRequestGoogleSignIn,
                            colors = ButtonDefaults.buttonColors(containerColor = FloodlightMint, contentColor = StadiumObsidian)
                        ) {
                            Text("Sign In")
                        }
                    }
                }
            }
        }

        // Featured Hero Banner
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clickable { onSelectMode(GameMode.T20) }
                    .testTag("hero_play_t20_card")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_stadium_hero),
                        contentDescription = "Featured Stadium Match",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(StadiumObsidian.copy(alpha = 0.92f), StadiumObsidian.copy(alpha = 0.35f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = FloodlightMint,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "FEATURED FLOODLIT T20",
                                style = MaterialTheme.typography.labelSmall,
                                color = StadiumObsidian,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Column {
                            Text("STEP TO THE CREASE", style = MaterialTheme.typography.displayMedium, color = Color.White)
                            Text("Under lights • Live Swing & Spin Physics", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                        }
                        Button(
                            onClick = { onSelectMode(GameMode.QUICK) },
                            colors = ButtonDefaults.buttonColors(containerColor = ChampionGold, contentColor = StadiumObsidian),
                            modifier = Modifier.testTag("quick_kickoff_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Instant 5-Over Clash", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section Label
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "ALL 10 CHAMPIONSHIP MODES",
                style = MaterialTheme.typography.labelMedium,
                color = FloodlightMint,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // All 10 Game Modes
        items(GameMode.entries) { mode ->
            ModeCard(
                mode = mode,
                icon = iconForMode(mode),
                onClick = { onSelectMode(mode) }
            )
        }
    }
}

@Composable
private fun ModeCard(
    mode: GameMode,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val accent = when (mode) {
        GameMode.ONLINE -> FloodlightMint
        GameMode.WORLD_CUP, GameMode.FRANCHISE -> ChampionGold
        else -> MaterialTheme.colorScheme.primary
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = TurfCardElevated),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("mode_card_${mode.id.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = mode.title, tint = accent)
            }
            Text(mode.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                mode.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2
            )
        }
    }
}

private fun iconForMode(mode: GameMode): ImageVector = when (mode) {
    GameMode.QUICK -> Icons.Default.Bolt
    GameMode.T20 -> Icons.Default.SportsCricket
    GameMode.ODI -> Icons.Default.Shield
    GameMode.TEST -> Icons.Default.MilitaryTech
    GameMode.WORLD_CUP -> Icons.Default.EmojiEvents
    GameMode.FRANCHISE -> Icons.Default.Star
    GameMode.CAREER -> Icons.Default.Groups
    GameMode.PRACTICE -> Icons.Default.TrackChanges
    GameMode.TOURNAMENT -> Icons.Default.EmojiEvents
    GameMode.ONLINE -> Icons.Default.Public
}
