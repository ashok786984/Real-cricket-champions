package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.LeaderboardEntry
import com.example.data.model.MatchHistoryRecord
import com.example.data.model.OnlineRoom
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CricketCloudRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || uid != userId) {
            emit(null)
            return@flow
        }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logStreamErrorIfAuthenticated(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeMatchHistory(userId: String): Flow<List<MatchHistoryRecord>> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || uid != userId) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/matchHistory"
        emitAll(
            db.collection("users").document(userId).collection("matchHistory")
                .snapshots()
                .map { snap ->
                    snap.toObjects(MatchHistoryRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfAuthenticated(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "leaderboard"
        emitAll(
            db.collection(path)
                .whereEqualTo("publicVisible", true)
                .limit(50)
                .snapshots()
                .map { snap ->
                    snap.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.rating }
                }
                .catch { error ->
                    logStreamErrorIfAuthenticated(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeWaitingRooms(): Flow<List<OnlineRoom>> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "onlineRooms"
        emitAll(
            db.collection(path)
                .whereEqualTo("status", "WAITING")
                .limit(20)
                .snapshots()
                .map { snap ->
                    snap.toObjects(OnlineRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfAuthenticated(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeMyRooms(userId: String): Flow<List<OnlineRoom>> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || uid != userId) {
            emit(emptyList())
            return@flow
        }
        val path = "onlineRooms"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantUids", userId)
                .limit(20)
                .snapshots()
                .map { snap ->
                    snap.toObjects(OnlineRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfAuthenticated(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeRoom(roomId: String): Flow<OnlineRoom?> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || roomId.isBlank()) {
            emit(null)
            return@flow
        }
        val path = "onlineRooms/$roomId"
        emitAll(
            db.collection("onlineRooms").document(roomId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(OnlineRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logStreamErrorIfAuthenticated(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        displayName: String,
        preferredTeamId: String,
        teamCode: String
    ): Result<Unit> {
        val cleanName = displayName.trim().take(50).ifBlank { "Champion" }
        val cleanTeamId = preferredTeamId.trim().take(40).ifBlank { "ind_tigers" }
        val cleanCode = teamCode.trim().take(6).ifBlank { "EMT" }
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            docPath = "users/$uid"
            val userRef = db.collection("users").document(uid)
            val snap = userRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "matchesPlayed" to 0L,
                    "matchesWon" to 0L,
                    "totalRuns" to 0L,
                    "totalWickets" to 0L,
                    "highScore" to 0L,
                    "sixesHit" to 0L,
                    "foursHit" to 0L,
                    "careerLevel" to 1L,
                    "rating" to 1000L,
                    "preferredTeamId" to cleanTeamId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(payload).await()
            }
            val lbRef = db.collection("leaderboard").document(uid)
            if (!lbRef.get().await().exists()) {
                val lbPayload = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "teamCode" to cleanCode,
                    "rating" to 1000L,
                    "matchesWon" to 0L,
                    "totalRuns" to 0L,
                    "sixesHit" to 0L,
                    "publicVisible" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                lbRef.set(lbPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordCompletedMatch(
        modeId: String,
        homeTeam: String,
        awayTeam: String,
        homeScore: String,
        awayScore: String,
        resultSummary: String,
        won: Boolean,
        playerRuns: Int,
        playerWickets: Int,
        sixesHit: Int,
        foursHit: Int,
        stadiumName: String,
        teamCode: String,
        displayName: String
    ): Result<Unit> {
        var path = "users/unknown/matchHistory"
        return try {
            val uid = requireUserId()
            val matchId = "match_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            path = "users/$uid/matchHistory/$matchId"
            val matchPayload = mapOf(
                "userId" to uid,
                "mode" to modeId.take(20),
                "homeTeam" to homeTeam.take(40).ifBlank { "Home XI" },
                "awayTeam" to awayTeam.take(40).ifBlank { "Away XI" },
                "homeScore" to homeScore.take(30).ifBlank { "0/0 (0.0)" },
                "awayScore" to awayScore.take(30).ifBlank { "0/0 (0.0)" },
                "resultSummary" to resultSummary.take(120).ifBlank { "Match Completed" },
                "won" to won,
                "playerRuns" to playerRuns.coerceIn(0, 2000).toLong(),
                "playerWickets" to playerWickets.coerceIn(0, 20).toLong(),
                "stadiumName" to stadiumName.take(60).ifBlank { "Emerald Bowl" },
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("matchHistory").document(matchId)
                .set(matchPayload).await()

            val userRef = db.collection("users").document(uid)
            val existing = userRef.get().await()
            if (existing.exists()) {
                val prevMatches = (existing.getLong("matchesPlayed") ?: 0L) + 1L
                val prevWon = (existing.getLong("matchesWon") ?: 0L) + (if (won) 1L else 0L)
                val prevRuns = (existing.getLong("totalRuns") ?: 0L) + playerRuns.coerceAtLeast(0)
                val prevWickets = (existing.getLong("totalWickets") ?: 0L) + playerWickets.coerceAtLeast(0)
                val prevHigh = maxOf(existing.getLong("highScore") ?: 0L, playerRuns.coerceIn(0, 1000).toLong())
                val prevSixes = (existing.getLong("sixesHit") ?: 0L) + sixesHit.coerceAtLeast(0)
                val prevFours = (existing.getLong("foursHit") ?: 0L) + foursHit.coerceAtLeast(0)
                val nextLevel = ((prevRuns / 250L) + 1L).coerceIn(1L, 100L)
                val nextRating = ((existing.getLong("rating") ?: 1000L) + if (won) 35L else -15L).coerceIn(100L, 10000L)

                userRef.update(
                    mapOf(
                        "matchesPlayed" to prevMatches,
                        "matchesWon" to prevWon,
                        "totalRuns" to prevRuns,
                        "totalWickets" to prevWickets,
                        "highScore" to prevHigh,
                        "sixesHit" to prevSixes,
                        "foursHit" to prevFours,
                        "careerLevel" to nextLevel,
                        "rating" to nextRating,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                db.collection("leaderboard").document(uid).set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to displayName.take(50).ifBlank { "Champion" },
                        "teamCode" to teamCode.take(6).ifBlank { "EMT" },
                        "rating" to nextRating,
                        "matchesWon" to prevWon,
                        "totalRuns" to prevRuns,
                        "sixesHit" to prevSixes,
                        "publicVisible" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createOnlineRoom(
        hostName: String,
        format: String,
        oversLimit: Long,
        stadiumId: String,
        hostTeamId: String
    ): Result<String> {
        var path = "onlineRooms"
        return try {
            val uid = requireUserId()
            val roomId = "room_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            path = "onlineRooms/$roomId"
            val roomCode = "RCC${(100..999).random()}"
            val safeFormat = if (format in listOf("QUICK_5", "T20", "SUPER_OVER")) format else "QUICK_5"
            val payload = mapOf(
                "roomCode" to roomCode,
                "hostUid" to uid,
                "hostName" to hostName.trim().take(50).ifBlank { "Host Captain" },
                "guestUid" to "",
                "guestName" to "",
                "participantUids" to listOf(uid),
                "status" to "WAITING",
                "format" to safeFormat,
                "oversLimit" to oversLimit.coerceIn(1L, 20L),
                "stadiumId" to stadiumId.take(40).ifBlank { "emerald_bowl" },
                "hostTeamId" to hostTeamId.take(40).ifBlank { "ind_tigers" },
                "guestTeamId" to "aus_sharks",
                "currentInnings" to 1L,
                "battingSide" to "HOST",
                "hostRuns" to 0L,
                "hostWickets" to 0L,
                "hostBalls" to 0L,
                "guestRuns" to 0L,
                "guestWickets" to 0L,
                "guestBalls" to 0L,
                "lastBallEvent" to "Room $roomCode open. Waiting for challenger.",
                "pendingDelivery" to "",
                "turnNumber" to 0L,
                "winnerUid" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("onlineRooms").document(roomId).set(payload).await()
            Result.success(roomId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinOnlineRoomById(
        roomId: String,
        guestName: String,
        guestTeamId: String
    ): Result<Unit> {
        val path = "onlineRooms/$roomId"
        return try {
            val uid = requireUserId()
            val ref = db.collection("onlineRooms").document(roomId)
            val snap = ref.get().await()
            val room = snap.toObject(OnlineRoom::class.java)
                ?: return Result.failure(IllegalStateException("Room not found"))
            if (room.hostUid == uid) return Result.success(Unit)
            if (room.status != "WAITING") {
                return Result.failure(IllegalStateException("Room is already full or finished"))
            }
            ref.update(
                mapOf(
                    "guestUid" to uid,
                    "guestName" to guestName.trim().take(50).ifBlank { "Challenger" },
                    "guestTeamId" to guestTeamId.take(40).ifBlank { "aus_sharks" },
                    "participantUids" to listOf(room.hostUid, uid),
                    "status" to "IN_PROGRESS",
                    "lastBallEvent" to "${guestName.take(20)} joined! Match live.",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinOnlineRoomByCode(
        roomCode: String,
        guestName: String,
        guestTeamId: String
    ): Result<String> {
        val cleanCode = roomCode.trim().uppercase()
        return try {
            requireUserId()
            val matches = db.collection("onlineRooms")
                .whereEqualTo("status", "WAITING")
                .limit(20)
                .get()
                .await()
                .toObjects(OnlineRoom::class.java)
            val target = matches.firstOrNull { it.roomCode.equals(cleanCode, ignoreCase = true) }
                ?: return Result.failure(IllegalStateException("No waiting room with code $cleanCode"))
            val joinResult = joinOnlineRoomById(target.id, guestName, guestTeamId)
            if (joinResult.isSuccess) Result.success(target.id)
            else Result.failure(joinResult.exceptionOrNull() ?: IllegalStateException("Join failed"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "onlineRooms")
            Result.failure(e)
        }
    }

    suspend fun submitRoomTurn(
        roomId: String,
        updates: Map<String, Any>
    ): Result<Unit> {
        val path = "onlineRooms/$roomId"
        return try {
            requireUserId()
            val payload = updates.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("onlineRooms").document(roomId).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private fun logStreamErrorIfAuthenticated(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
