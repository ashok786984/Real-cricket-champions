package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GameMode
import com.example.data.model.GraphicsQuality
import com.example.data.model.MatchPhase
import com.example.data.model.OnlineRoom
import com.example.engine.CricketCatalog
import com.example.engine.CricketRulesEngine
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.FloodlightMint
import com.example.ui.theme.StadiumObsidian
import com.example.ui.theme.TurfCardElevated
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MatchViewModel

@Composable
fun OnlineRoomMatchHost(
    roomId: String,
    mainViewModel: MainViewModel,
    graphicsQuality: GraphicsQuality,
    hapticsEnabled: Boolean,
    onChangeQuality: (GraphicsQuality) -> Unit,
    onExitRoom: () -> Unit
) {
    val roomFlow = remember(roomId) { mainViewModel.observeOnlineRoom(roomId) }
    val room by roomFlow.collectAsStateWithLifecycle(initialValue = null)

    BackHandler { onExitRoom() }

    val currentRoom = room
    if (currentRoom == null) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = FloodlightMint)
            Spacer(Modifier.height(12.dp))
            Text("Connecting to Online Stadium Room...", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    if (currentRoom.status == "WAITING") {
        WaitingRoomView(room = currentRoom, onBack = onExitRoom)
        return
    }

    // Room is IN_PROGRESS or FINISHED -> Launch LiveMatchScreen synchronized with Firestore
    val isHost = currentRoom.hostUid == mainViewModel.currentUserId
    val matchVm = remember(currentRoom.id) {
        val hostTeam = CricketCatalog.teamById(currentRoom.hostTeamId)
        val guestTeam = CricketCatalog.teamById(currentRoom.guestTeamId)
        val stadium = CricketCatalog.stadiumById(currentRoom.stadiumId)
        val initial = CricketRulesEngine.createMatch(
            mode = GameMode.ONLINE,
            userTeam = if (isHost) hostTeam else guestTeam,
            opponentTeam = if (isHost) guestTeam else hostTeam,
            stadium = stadium,
            timeOfDay = stadium.defaultTime,
            weather = stadium.defaultWeather,
            userBatsFirst = isHost,
            oversLimit = currentRoom.oversLimit.toInt(),
            isOnlineMatch = true,
            onlineRoomId = currentRoom.id
        )
        MatchViewModel(
            initialMatch = initial,
            onMatchFinishedCallback = { finished -> mainViewModel.onMatchCompleted(finished) },
            onOnlineBallSynced = { updatedMatch, outcome ->
                val active = updatedMatch.currentInnings
                val isFinished = updatedMatch.phase == MatchPhase.MATCH_FINISHED
                val hostIsBattingNow = active.battingTeam.id == currentRoom.hostTeamId
                val updates = mutableMapOf<String, Any>(
                    "currentInnings" to (updatedMatch.activeInningsIndex + 1).toLong(),
                    "battingSide" to (if (hostIsBattingNow) "HOST" else "GUEST"),
                    "lastBallEvent" to outcome.commentary.take(150),
                    "turnNumber" to (currentRoom.turnNumber + 1L)
                )
                if (hostIsBattingNow) {
                    updates["hostRuns"] = active.runs.toLong()
                    updates["hostWickets"] = active.wickets.toLong()
                    updates["hostBalls"] = active.legalBalls.toLong()
                } else {
                    updates["guestRuns"] = active.runs.toLong()
                    updates["guestWickets"] = active.wickets.toLong()
                    updates["guestBalls"] = active.legalBalls.toLong()
                }
                if (isFinished) {
                    updates["status"] = "FINISHED"
                    updates["winnerUid"] = when (updatedMatch.winnerTeamId) {
                        currentRoom.hostTeamId -> currentRoom.hostUid
                        currentRoom.guestTeamId -> currentRoom.guestUid
                        else -> "DRAW"
                    }
                }
                mainViewModel.submitOnlineRoomTurn(currentRoom.id, updates)
            }
        )
    }

    LiveMatchScreen(
        matchViewModel = matchVm,
        graphicsQuality = graphicsQuality,
        hapticsEnabled = hapticsEnabled,
        onChangeQuality = onChangeQuality,
        onExitMatch = onExitRoom
    )
}

@Composable
private fun WaitingRoomView(room: OnlineRoom, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Leave Room")
            }
            Text("PRIVATE MATCH ROOM", style = MaterialTheme.typography.headlineLarge, color = FloodlightMint)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = TurfCardElevated),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = ChampionGold)
                Text("SHARE ROOM INVITE CODE", style = MaterialTheme.typography.labelMedium, color = ChampionGold)
                Surface(color = StadiumObsidian, shape = RoundedCornerShape(16.dp)) {
                    Text(
                        text = room.roomCode,
                        style = MaterialTheme.typography.displayLarge,
                        color = FloodlightMint,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 10.dp)
                    )
                }
                Text("Host: ${room.hostName} • ${room.oversLimit} Overs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Waiting for opponent to join via Room Code or Open Matchmaking...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = TurfCardElevated),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Back to Lobby")
        }
    }
}
