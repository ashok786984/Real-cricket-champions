package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CareerProfileLocal
import com.example.data.model.CricketStadium
import com.example.data.model.CricketTeam
import com.example.data.model.FranchiseStanding
import com.example.data.model.GameMode
import com.example.data.model.GraphicsQuality
import com.example.data.model.LeaderboardEntry
import com.example.data.model.MatchHistoryRecord
import com.example.data.model.MatchState
import com.example.data.model.OnlineRoom
import com.example.data.model.TimeOfDay
import com.example.data.model.TournamentFixture
import com.example.data.model.TournamentState
import com.example.data.model.UserProfile
import com.example.data.model.WeatherCondition
import com.example.data.repository.AppSettings
import com.example.data.repository.CricketCloudRepository
import com.example.data.repository.LocalPreferencesRepository
import com.example.engine.CricketCatalog
import com.example.engine.CricketRulesEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class MatchSetupConfig(
    val mode: GameMode = GameMode.QUICK,
    val userTeam: CricketTeam = CricketCatalog.teams[0],
    val opponentTeam: CricketTeam = CricketCatalog.teams[1],
    val stadium: CricketStadium = CricketCatalog.stadiums[0],
    val timeOfDay: TimeOfDay = TimeOfDay.NIGHT,
    val weather: WeatherCondition = WeatherCondition.CLEAR,
    val oversLimit: Int = 5,
    val tossCompleted: Boolean = false,
    val userWonToss: Boolean = true,
    val userBatsFirst: Boolean = true,
    val tournamentFixtureId: String? = null
)

class MainViewModel(
    private val cloudRepository: CricketCloudRepository?,
    private val localPrefs: LocalPreferencesRepository,
    val currentUserId: String?,
    val userDisplayName: String
) : ViewModel() {

    val isAuthenticated: Boolean = !currentUserId.isNullOrBlank() && cloudRepository != null

    val settings: StateFlow<AppSettings> = localPrefs.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppSettings())

    val localCareer: StateFlow<CareerProfileLocal> = localPrefs.careerFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CareerProfileLocal())

    val userProfileState: StateFlow<UiState<UserProfile?>> = if (isAuthenticated && currentUserId != null && cloudRepository != null) {
        cloudRepository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error -> emit(handleFlowError(error, "profile")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    } else {
        MutableStateFlow(UiState.Success(null))
    }

    val matchHistoryState: StateFlow<UiState<List<MatchHistoryRecord>>> = if (isAuthenticated && currentUserId != null && cloudRepository != null) {
        cloudRepository.observeMatchHistory(currentUserId)
            .map<List<MatchHistoryRecord>, UiState<List<MatchHistoryRecord>>> { UiState.Success(it) }
            .catch { error -> emit(handleFlowError(error, "match history")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    } else {
        MutableStateFlow(UiState.Success(emptyList()))
    }

    val leaderboardState: StateFlow<UiState<List<LeaderboardEntry>>> = if (isAuthenticated && cloudRepository != null) {
        cloudRepository.observeLeaderboard()
            .map<List<LeaderboardEntry>, UiState<List<LeaderboardEntry>>> { UiState.Success(it) }
            .catch { error -> emit(handleFlowError(error, "leaderboard")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    } else {
        MutableStateFlow(UiState.Success(emptyList()))
    }

    val waitingRoomsState: StateFlow<UiState<List<OnlineRoom>>> = if (isAuthenticated && cloudRepository != null) {
        cloudRepository.observeWaitingRooms()
            .map<List<OnlineRoom>, UiState<List<OnlineRoom>>> { UiState.Success(it) }
            .catch { error -> emit(handleFlowError(error, "open rooms")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    } else {
        MutableStateFlow(UiState.Success(emptyList()))
    }

    val myRoomsState: StateFlow<UiState<List<OnlineRoom>>> = if (isAuthenticated && currentUserId != null && cloudRepository != null) {
        cloudRepository.observeMyRooms(currentUserId)
            .map<List<OnlineRoom>, UiState<List<OnlineRoom>>> { UiState.Success(it) }
            .catch { error -> emit(handleFlowError(error, "active rooms")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    } else {
        MutableStateFlow(UiState.Success(emptyList()))
    }

    private val _setupConfig = MutableStateFlow(MatchSetupConfig())
    val setupConfig: StateFlow<MatchSetupConfig> = _setupConfig.asStateFlow()

    private val _worldCupState = MutableStateFlow(CricketCatalog.buildWorldCup("ind_tigers"))
    val worldCupState: StateFlow<TournamentState> = _worldCupState.asStateFlow()

    private val _franchiseState = MutableStateFlow(CricketCatalog.buildFranchiseLeague("ind_tigers"))
    val franchiseState: StateFlow<TournamentState> = _franchiseState.asStateFlow()

    private val _offlineCupState = MutableStateFlow(CricketCatalog.buildOfflineCup("ind_tigers"))
    val offlineCupState: StateFlow<TournamentState> = _offlineCupState.asStateFlow()

    private val _offlineMatchHistory = MutableStateFlow<List<MatchHistoryRecord>>(emptyList())
    val offlineMatchHistory: StateFlow<List<MatchHistoryRecord>> = _offlineMatchHistory.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    init {
        if (isAuthenticated && cloudRepository != null) {
            viewModelScope.launch {
                val team = settings.value.preferredTeamId
                val teamObj = CricketCatalog.teamById(team)
                cloudRepository.ensureUserProfile(
                    displayName = userDisplayName,
                    preferredTeamId = teamObj.id,
                    teamCode = teamObj.shortCode
                )
            }
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun startModeSetup(mode: GameMode) {
        val currentSettings = settings.value
        val userTeam = CricketCatalog.teamById(currentSettings.preferredTeamId)
        val oppTeam = CricketCatalog.teams.firstOrNull { it.id != userTeam.id } ?: CricketCatalog.teams[1]
        _setupConfig.value = MatchSetupConfig(
            mode = mode,
            userTeam = userTeam,
            opponentTeam = oppTeam,
            stadium = CricketCatalog.stadiums.first(),
            timeOfDay = currentSettings.defaultTimeOfDay,
            weather = currentSettings.defaultWeather,
            oversLimit = mode.defaultOvers,
            tossCompleted = mode == GameMode.PRACTICE,
            userBatsFirst = true
        )
    }

    fun startTournamentFixtureSetup(tournamentMode: GameMode, fixture: TournamentFixture) {
        val tournament = when (tournamentMode) {
            GameMode.WORLD_CUP -> _worldCupState.value
            GameMode.FRANCHISE -> _franchiseState.value
            else -> _offlineCupState.value
        }
        val userIsHome = fixture.homeTeamId == tournament.userTeamId
        val userTeam = CricketCatalog.teamById(tournament.userTeamId)
        val oppId = if (userIsHome) fixture.awayTeamId else fixture.homeTeamId
        val oppTeam = CricketCatalog.teamById(oppId)
        val s = settings.value
        _setupConfig.value = MatchSetupConfig(
            mode = tournamentMode,
            userTeam = userTeam,
            opponentTeam = oppTeam,
            stadium = CricketCatalog.stadiums.random(),
            timeOfDay = s.defaultTimeOfDay,
            weather = s.defaultWeather,
            oversLimit = tournamentMode.defaultOvers,
            tossCompleted = false,
            tournamentFixtureId = fixture.id
        )
    }

    fun updateSetup(transform: (MatchSetupConfig) -> MatchSetupConfig) {
        _setupConfig.update(transform)
    }

    fun performCoinToss(userCalledHeads: Boolean): Boolean {
        val coinIsHeads = Random.nextBoolean()
        val userWon = (userCalledHeads == coinIsHeads)
        val aiBatsFirst = Random.nextBoolean()
        _setupConfig.update {
            it.copy(
                tossCompleted = true,
                userWonToss = userWon,
                userBatsFirst = if (userWon) true else !aiBatsFirst
            )
        }
        return userWon
    }

    fun setTossDecision(userBatsFirst: Boolean) {
        _setupConfig.update { it.copy(userBatsFirst = userBatsFirst, tossCompleted = true) }
    }

    fun createMatchFromSetup(): MatchState {
        val c = _setupConfig.value
        return CricketRulesEngine.createMatch(
            mode = c.mode,
            userTeam = c.userTeam,
            opponentTeam = c.opponentTeam,
            stadium = c.stadium,
            timeOfDay = c.timeOfDay,
            weather = c.weather,
            userBatsFirst = c.userBatsFirst,
            oversLimit = c.oversLimit
        )
    }

    fun upgradeCareerSkill(skill: String) {
        viewModelScope.launch {
            localPrefs.updateCareer { c ->
                if (c.skillPoints <= 0) return@updateCareer c
                when (skill) {
                    "BAT" -> c.copy(skillPoints = c.skillPoints - 1, battingRating = (c.battingRating + 2).coerceAtMost(99))
                    "BOWL" -> c.copy(skillPoints = c.skillPoints - 1, bowlingRating = (c.bowlingRating + 2).coerceAtMost(99))
                    "FIELD" -> c.copy(skillPoints = c.skillPoints - 1, fieldingRating = (c.fieldingRating + 2).coerceAtMost(99))
                    else -> c
                }
            }
        }
    }

    fun resetTournament(mode: GameMode, userTeamId: String) {
        when (mode) {
            GameMode.WORLD_CUP -> _worldCupState.value = CricketCatalog.buildWorldCup(userTeamId)
            GameMode.FRANCHISE -> _franchiseState.value = CricketCatalog.buildFranchiseLeague(userTeamId)
            else -> _offlineCupState.value = CricketCatalog.buildOfflineCup(userTeamId)
        }
    }

    fun simulateAiTournamentFixtures(mode: GameMode) {
        val stateFlow = when (mode) {
            GameMode.WORLD_CUP -> _worldCupState
            GameMode.FRANCHISE -> _franchiseState
            else -> _offlineCupState
        }
        val t = stateFlow.value
        val updatedFixtures = t.fixtures.map { fx ->
            if (fx.winnerTeamId == null && fx.homeTeamId != t.userTeamId && fx.awayTeamId != t.userTeamId) {
                val home = CricketCatalog.teamById(fx.homeTeamId)
                val away = CricketCatalog.teamById(fx.awayTeamId)
                val homeWins = (home.teamRating + Random.nextInt(-8, 9)) >= away.teamRating
                val winner = if (homeWins) home else away
                val r1 = Random.nextInt(142, 196)
                val r2 = if (homeWins) r1 - Random.nextInt(5, 25) else r1 + Random.nextInt(1, 14)
                fx.copy(
                    winnerTeamId = winner.id,
                    scoreSummary = "${home.shortCode} $r1 - $r2 ${away.shortCode}"
                )
            } else fx
        }
        stateFlow.value = advanceTournamentBracket(t.copy(fixtures = updatedFixtures))
    }

    fun onMatchCompleted(matchState: MatchState) {
        val userInnings = matchState.inningsList.firstOrNull { it.battingTeam.id == matchState.userTeam.id }
        val oppInnings = matchState.inningsList.firstOrNull { it.battingTeam.id == matchState.opponentTeam.id }
        val userRuns = userInnings?.runs ?: 0
        val userWicketsTaken = oppInnings?.wickets ?: 0
        val sixes = userInnings?.batterStats?.sumOf { it.sixes } ?: 0
        val fours = userInnings?.batterStats?.sumOf { it.fours } ?: 0
        val userWon = matchState.winnerTeamId == matchState.userTeam.id

        val homeScore = matchState.inningsList.getOrNull(0)?.let { "${it.runs}/${it.wickets} (${it.oversFormatted})" } ?: "0/0 (0.0)"
        val awayScore = matchState.inningsList.getOrNull(1)?.let { "${it.runs}/${it.wickets} (${it.oversFormatted})" } ?: "0/0 (0.0)"

        // Update local career stats
        viewModelScope.launch {
            localPrefs.updateCareer { c ->
                val gainedXp = 60 + userRuns + (userWicketsTaken * 15) + (if (userWon) 80 else 20)
                val totalXp = c.xp + gainedXp
                val newLevel = (c.level + totalXp / 250).coerceAtMost(100)
                val spGain = (newLevel - c.level).coerceAtLeast(0)
                c.copy(
                    level = newLevel,
                    xp = totalXp % 250,
                    skillPoints = c.skillPoints + spGain,
                    matchesPlayed = c.matchesPlayed + 1,
                    matchesWon = c.matchesWon + (if (userWon) 1 else 0),
                    totalRuns = c.totalRuns + userRuns,
                    totalWickets = c.totalWickets + userWicketsTaken,
                    highScore = maxOf(c.highScore, userRuns),
                    sixesHit = c.sixesHit + sixes,
                    foursHit = c.foursHit + fours
                )
            }
        }

        // Advance Tournament if applicable
        val fixtureId = _setupConfig.value.tournamentFixtureId
        if (fixtureId != null) {
            val stateFlow = when (matchState.mode) {
                GameMode.WORLD_CUP -> _worldCupState
                GameMode.FRANCHISE -> _franchiseState
                else -> _offlineCupState
            }
            val currentT = stateFlow.value
            val updated = currentT.fixtures.map { fx ->
                if (fx.id == fixtureId) {
                    fx.copy(
                        winnerTeamId = matchState.winnerTeamId ?: matchState.userTeam.id,
                        scoreSummary = "${matchState.userTeam.shortCode} $userRuns - ${oppInnings?.runs ?: 0} ${matchState.opponentTeam.shortCode}"
                    )
                } else fx
            }
            stateFlow.value = advanceTournamentBracket(currentT.copy(fixtures = updated))
        }

        // Record offline local copy
        val localRecord = MatchHistoryRecord(
            id = matchState.matchId,
            userId = currentUserId ?: "guest",
            mode = matchState.mode.id,
            homeTeam = matchState.userTeam.name,
            awayTeam = matchState.opponentTeam.name,
            homeScore = homeScore,
            awayScore = awayScore,
            resultSummary = matchState.resultSummary.ifBlank { "Match Finished" },
            won = userWon,
            playerRuns = userRuns.toLong(),
            playerWickets = userWicketsTaken.toLong(),
            stadiumName = matchState.stadium.name
        )
        _offlineMatchHistory.update { listOf(localRecord) + it.take(19) }

        // Sync to Firestore if signed in
        if (isAuthenticated && cloudRepository != null) {
            viewModelScope.launch {
                val res = cloudRepository.recordCompletedMatch(
                    modeId = matchState.mode.id,
                    homeTeam = matchState.userTeam.name,
                    awayTeam = matchState.opponentTeam.name,
                    homeScore = homeScore,
                    awayScore = awayScore,
                    resultSummary = matchState.resultSummary.ifBlank { "Match Completed" },
                    won = userWon,
                    playerRuns = userRuns,
                    playerWickets = userWicketsTaken,
                    sixesHit = sixes,
                    foursHit = fours,
                    stadiumName = matchState.stadium.name,
                    teamCode = matchState.userTeam.shortCode,
                    displayName = userDisplayName
                )
                if (res.isFailure) {
                    _statusBanner.value = "Match saved locally. Cloud sync error: ${res.exceptionOrNull()?.localizedMessage}"
                }
            }
        }
    }

    private fun advanceTournamentBracket(t: TournamentState): TournamentState {
        if (t.mode == GameMode.FRANCHISE) {
            val standingsMap = t.standings.associateBy { it.teamId }.toMutableMap()
            t.standings.forEach { s -> standingsMap[s.teamId] = FranchiseStanding(teamId = s.teamId) }
            t.fixtures.filter { it.winnerTeamId != null }.forEach { fx ->
                val winner = fx.winnerTeamId!!
                val loser = if (winner == fx.homeTeamId) fx.awayTeamId else fx.homeTeamId
                val wPrev = standingsMap[winner]
                val lPrev = standingsMap[loser]
                if (wPrev != null) standingsMap[winner] = wPrev.copy(played = wPrev.played + 1, won = wPrev.won + 1, points = wPrev.points + 2, netRunRate = wPrev.netRunRate + 0.42)
                if (lPrev != null) standingsMap[loser] = lPrev.copy(played = lPrev.played + 1, lost = lPrev.lost + 1, netRunRate = lPrev.netRunRate - 0.38)
            }
            val sortedStandings = standingsMap.values.sortedWith(compareByDescending<FranchiseStanding> { it.points }.thenByDescending { it.netRunRate })
            val allLeagueDone = t.fixtures.none { it.roundLabel.startsWith("League") && it.winnerTeamId == null }
            val hasFinal = t.fixtures.any { it.roundLabel == "Grand Final" }
            var newFixtures = t.fixtures
            if (allLeagueDone && !hasFinal && sortedStandings.size >= 2) {
                newFixtures = newFixtures + TournamentFixture("final", "Grand Final", sortedStandings[0].teamId, sortedStandings[1].teamId)
            }
            val champion = newFixtures.firstOrNull { it.roundLabel == "Grand Final" }?.winnerTeamId
            return t.copy(fixtures = newFixtures, standings = sortedStandings, championTeamId = champion)
        }

        // Knockout progression (World Cup / Offline Cup)
        val qfs = t.fixtures.filter { it.roundLabel.startsWith("Quarter") }
        val sfs = t.fixtures.filter { it.roundLabel.startsWith("Semi") }
        val fin = t.fixtures.firstOrNull { it.roundLabel == "Grand Final" }

        var nextFixtures = t.fixtures
        if (qfs.size == 4 && qfs.all { it.winnerTeamId != null } && sfs.isEmpty()) {
            nextFixtures = nextFixtures + listOf(
                TournamentFixture("sf1", "Semi-Final 1", qfs[0].winnerTeamId!!, qfs[1].winnerTeamId!!),
                TournamentFixture("sf2", "Semi-Final 2", qfs[2].winnerTeamId!!, qfs[3].winnerTeamId!!)
            )
        }
        val updatedSfs = nextFixtures.filter { it.roundLabel.startsWith("Semi") }
        if (updatedSfs.size == 2 && updatedSfs.all { it.winnerTeamId != null } && fin == null) {
            nextFixtures = nextFixtures + TournamentFixture("final", "Grand Final", updatedSfs[0].winnerTeamId!!, updatedSfs[1].winnerTeamId!!)
        }
        val champion = nextFixtures.firstOrNull { it.roundLabel == "Grand Final" }?.winnerTeamId
        return t.copy(fixtures = nextFixtures, championTeamId = champion)
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { localPrefs.updateSettings(transform) }
    }

    fun createOnlineRoom(format: String, overs: Long, stadiumId: String, teamId: String, onCreated: (String) -> Unit) {
        val repo = cloudRepository ?: run {
            _statusBanner.value = "Sign in with Google to host online rooms."
            return
        }
        viewModelScope.launch {
            val res = repo.createOnlineRoom(userDisplayName, format, overs, stadiumId, teamId)
            res.onSuccess { roomId -> onCreated(roomId) }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to create room" }
        }
    }

    fun joinRoomById(roomId: String, teamId: String, onJoined: (String) -> Unit) {
        val repo = cloudRepository ?: return
        viewModelScope.launch {
            val res = repo.joinOnlineRoomById(roomId, userDisplayName, teamId)
            res.onSuccess { onJoined(roomId) }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Could not join room" }
        }
    }

    fun joinRoomByCode(code: String, teamId: String, onJoined: (String) -> Unit) {
        val repo = cloudRepository ?: return
        viewModelScope.launch {
            val res = repo.joinOnlineRoomByCode(code, userDisplayName, teamId)
            res.onSuccess { id -> onJoined(id) }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Room code not found" }
        }
    }

    fun observeOnlineRoom(roomId: String) =
        cloudRepository?.observeRoom(roomId) ?: flowOf(null)

    fun submitOnlineRoomTurn(roomId: String, updates: Map<String, Any>) {
        val repo = cloudRepository ?: return
        viewModelScope.launch {
            val res = repo.submitRoomTurn(roomId, updates)
            if (res.isFailure) {
                _statusBanner.value = "Online sync issue: ${res.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    private fun handleFlowError(error: Throwable, label: String): UiState.Error {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w("MainVM", "Error observing $label", rootError)
        return UiState.Error(rootError.localizedMessage ?: "Failed to load $label")
    }
}
