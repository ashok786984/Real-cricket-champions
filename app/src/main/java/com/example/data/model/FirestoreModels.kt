package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Champion",
    val matchesPlayed: Long = 0L,
    val matchesWon: Long = 0L,
    val totalRuns: Long = 0L,
    val totalWickets: Long = 0L,
    val highScore: Long = 0L,
    val sixesHit: Long = 0L,
    val foursHit: Long = 0L,
    val careerLevel: Long = 1L,
    val rating: Long = 1000L,
    val preferredTeamId: String = "ind_tigers",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MatchHistoryRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mode: String = "QUICK",
    val homeTeam: String = "",
    val awayTeam: String = "",
    val homeScore: String = "0/0 (0.0)",
    val awayScore: String = "0/0 (0.0)",
    val resultSummary: String = "",
    val won: Boolean = false,
    val playerRuns: Long = 0L,
    val playerWickets: Long = 0L,
    val stadiumName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Champion",
    val teamCode: String = "IND",
    val rating: Long = 1000L,
    val matchesWon: Long = 0L,
    val totalRuns: Long = 0L,
    val sixesHit: Long = 0L,
    val publicVisible: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OnlineRoom(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val hostUid: String = "",
    val hostName: String = "",
    val guestUid: String = "",
    val guestName: String = "",
    val participantUids: List<String> = emptyList(),
    val status: String = "WAITING",
    val format: String = "QUICK_5",
    val oversLimit: Long = 5L,
    val stadiumId: String = "emerald_bowl",
    val hostTeamId: String = "ind_tigers",
    val guestTeamId: String = "aus_sharks",
    val currentInnings: Long = 1L,
    val battingSide: String = "HOST",
    val hostRuns: Long = 0L,
    val hostWickets: Long = 0L,
    val hostBalls: Long = 0L,
    val guestRuns: Long = 0L,
    val guestWickets: Long = 0L,
    val guestBalls: Long = 0L,
    val lastBallEvent: String = "Room created",
    val pendingDelivery: String = "",
    val turnNumber: Long = 0L,
    val winnerUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
