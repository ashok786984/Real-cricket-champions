package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.OnlineRoom
import com.example.engine.CricketCatalog
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.FloodlightMint
import com.example.ui.theme.StadiumObsidian
import com.example.ui.theme.TurfCardElevated
import com.example.ui.theme.TurfCardHigh
import com.example.viewmodel.UiState

@Composable
fun OnlineLobbyScreen(
    isAuthenticated: Boolean,
    currentUserId: String?,
    preferredTeamId: String,
    waitingRoomsState: UiState<List<OnlineRoom>>,
    myRoomsState: UiState<List<OnlineRoom>>,
    onCreateRoom: (format: String, overs: Long, stadiumId: String, teamId: String) -> Unit,
    onJoinRoomById: (String, String) -> Unit,
    onJoinRoomByCode: (String, String) -> Unit,
    onEnterActiveRoom: (OnlineRoom) -> Unit,
    onRequestSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isAuthenticated) {
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Public, contentDescription = null, tint = FloodlightMint, modifier = Modifier.height(64.dp))
            Spacer(Modifier.height(12.dp))
            Text("ONLINE 1v1 MULTIPLAYER", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Sign in with Google to host private rooms, matchmake against rivals, reconnect to active innings, and post scores to the global leaderboard.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onRequestSignIn,
                colors = ButtonDefaults.buttonColors(containerColor = FloodlightMint, contentColor = StadiumObsidian),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("online_signin_cta")
            ) {
                Icon(Icons.Default.CloudDone, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sign in with Google", fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    var roomCodeInput by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf("QUICK_5") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("ONLINE 1v1 LOBBY", style = MaterialTheme.typography.headlineLarge, color = FloodlightMint)
            Text("Real-time synchronized innings with server-authoritative score validation", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Create Room Card
        item {
            Card(colors = CardDefaults.cardColors(containerColor = TurfCardElevated), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("HOST A MATCH ROOM", style = MaterialTheme.typography.labelMedium, color = ChampionGold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SUPER_OVER" to "1 Ov", "QUICK_5" to "5 Ov", "T20" to "20 Ov").forEach { (fmt, lbl) ->
                            FilterChip(
                                selected = selectedFormat == fmt,
                                onClick = { selectedFormat = fmt },
                                label = { Text(lbl) }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val overs = when (selectedFormat) {
                                "SUPER_OVER" -> 1L
                                "T20" -> 20L
                                else -> 5L
                            }
                            onCreateRoom(selectedFormat, overs, "emerald_bowl", preferredTeamId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FloodlightMint, contentColor = StadiumObsidian),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("create_room_button")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create Match Room", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Join by Invite Code
        item {
            Card(colors = CardDefaults.cardColors(containerColor = TurfCardElevated), shape = RoundedCornerShape(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = roomCodeInput,
                        onValueChange = { roomCodeInput = it.uppercase().take(8) },
                        label = { Text("Room Code (e.g. RCC482)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("room_code_input")
                    )
                    Button(
                        onClick = { if (roomCodeInput.isNotBlank()) onJoinRoomByCode(roomCodeInput, preferredTeamId) },
                        modifier = Modifier.height(52.dp).testTag("join_code_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Join")
                    }
                }
            }
        }

        // My Active Rooms (Reconnection)
        item {
            Text("MY ROOMS & RECONNECTION", style = MaterialTheme.typography.labelMedium, color = ChampionGold)
        }
        when (myRoomsState) {
            is UiState.Loading -> item { CircularProgressIndicator() }
            is UiState.Error -> item { Text(myRoomsState.message, color = MaterialTheme.colorScheme.error) }
            is UiState.Success -> {
                val active = myRoomsState.data.filter { it.status != "FINISHED" }
                if (active.isEmpty()) {
                    item {
                        Text("No active multiplayer rooms yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(active, key = { it.id }) { room ->
                        RoomRowCard(
                            room = room,
                            isMyRoom = true,
                            currentUserId = currentUserId,
                            onAction = { onEnterActiveRoom(room) }
                        )
                    }
                }
            }
        }

        // Open Matchmaking Rooms
        item {
            Text("OPEN MATCHMAKING ROOMS", style = MaterialTheme.typography.labelMedium, color = FloodlightMint)
        }
        when (waitingRoomsState) {
            is UiState.Loading -> item { CircularProgressIndicator() }
            is UiState.Error -> item { Text(waitingRoomsState.message, color = MaterialTheme.colorScheme.error) }
            is UiState.Success -> {
                val joinable = waitingRoomsState.data.filter { it.hostUid != currentUserId }
                if (joinable.isEmpty()) {
                    item {
                        Text("No open challengers waiting right now. Create a room above!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(joinable, key = { it.id }) { room ->
                        RoomRowCard(
                            room = room,
                            isMyRoom = false,
                            currentUserId = currentUserId,
                            onAction = { onJoinRoomById(room.id, preferredTeamId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomRowCard(
    room: OnlineRoom,
    isMyRoom: Boolean,
    currentUserId: String?,
    onAction: () -> Unit
) {
    val hostTeam = CricketCatalog.teamById(room.hostTeamId)
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isMyRoom) TurfCardHigh else TurfCardElevated),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = FloodlightMint.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                        Text(room.roomCode, style = MaterialTheme.typography.labelMedium, color = FloodlightMint, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Text("${room.oversLimit} Ov • ${room.status}", style = MaterialTheme.typography.labelSmall, color = ChampionGold)
                }
                Text("${room.hostName} (${hostTeam.shortCode}) ${if (room.guestName.isNotBlank()) "vs ${room.guestName}" else ""}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(room.lastBallEvent, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onAction) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text(if (isMyRoom) (if (room.hostUid == currentUserId && room.status == "WAITING") "Open" else "Resume") else "Join")
            }
        }
    }
}
