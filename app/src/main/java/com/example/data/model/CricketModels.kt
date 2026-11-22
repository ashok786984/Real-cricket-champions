package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class GameMode(
    val id: String,
    val title: String,
    val subtitle: String,
    val defaultOvers: Int,
    val isMultiInnings: Boolean = false
) {
    QUICK("QUICK", "Quick Match", "2 or 5 over instant clash", 5),
    T20("T20", "T20 Cricket", "20-over floodlit thriller", 20),
    ODI("ODI", "ODI Cricket", "50-over international battle", 50),
    TEST("TEST", "Test Cricket", "Multi-innings red-ball classic", 90, isMultiInnings = true),
    WORLD_CUP("WORLD_CUP", "World Cup", "8-nation knockout trophy", 20),
    FRANCHISE("FRANCHISE", "Franchise League", "6-city premier T20 season", 20),
    CAREER("CAREER", "Career Mode", "Rise from rookie to legend", 10),
    PRACTICE("PRACTICE", "Practice Nets", "Open batting & bowling drills", 99),
    TOURNAMENT("TOURNAMENT", "Offline Cup", "Knockout championship bracket", 10),
    ONLINE("ONLINE", "Online 1v1", "Live multiplayer & rooms", 5)
}

enum class PlayerRole(val label: String) {
    BATSMAN("BAT"),
    ALL_ROUNDER("AR"),
    WICKETKEEPER("WK"),
    FAST_BOWLER("PACE"),
    SPIN_BOWLER("SPIN")
}

data class CricketPlayer(
    val id: String,
    val name: String,
    val role: PlayerRole,
    val battingSkill: Int,
    val bowlingSkill: Int,
    val fieldingSkill: Int,
    val isLeftHanded: Boolean = false,
    val isSpinBowler: Boolean = false
)

data class CricketTeam(
    val id: String,
    val name: String,
    val shortCode: String,
    val primaryColor: Color,
    val accentColor: Color,
    val squad: List<CricketPlayer>
) {
    val teamRating: Int
        get() = (squad.map { (it.battingSkill + it.bowlingSkill) / 2 }.average()).toInt()
}

enum class TimeOfDay(val label: String) {
    DAY("Daylight"),
    TWILIGHT("Twilight"),
    NIGHT("Floodlit Night")
}

enum class WeatherCondition(
    val label: String,
    val swingMultiplier: Float,
    val spinMultiplier: Float,
    val bounceMultiplier: Float
) {
    CLEAR("Clear Sky", 1.0f, 1.0f, 1.0f),
    OVERCAST("Overcast Swing", 1.45f, 0.9f, 1.05f),
    HUMID_DEW("Humid Dew", 1.15f, 0.8f, 0.95f),
    DRY_DUSTY("Dry & Turning", 0.85f, 1.45f, 0.9f)
}

data class CricketStadium(
    val id: String,
    val name: String,
    val city: String,
    val pitchPace: Float,
    val pitchBounce: Float,
    val pitchTurn: Float,
    val boundaryMeters: Int,
    val defaultTime: TimeOfDay,
    val defaultWeather: WeatherCondition
)

enum class ShotType(
    val label: String,
    val powerMultiplier: Float,
    val riskFactor: Float,
    val lofted: Boolean
) {
    LEAVE("Leave", 0.0f, 0.02f, false),
    DEFEND("Defend", 0.35f, 0.08f, false),
    DRIVE("Drive", 0.95f, 0.20f, false),
    PULL("Pull / Sweep", 1.10f, 0.28f, false),
    LOFT("Loft / Six", 1.40f, 0.42f, true)
}

enum class DeliveryType(
    val label: String,
    val speedKmhRange: IntRange,
    val swingBase: Float,
    val spinBase: Float,
    val isFast: Boolean
) {
    FAST_STRAIGHT("Seam Up", 136..148, 0.15f, 0.0f, true),
    OUTSWINGER("Outswinger", 132..144, 0.72f, 0.0f, true),
    INSWINGER("Inswinger", 134..145, -0.72f, 0.0f, true),
    YORKER("Toe-Crushing Yorker", 140..152, 0.18f, 0.0f, true),
    BOUNCER("Sharp Bouncer", 138..150, 0.10f, 0.0f, true),
    OFF_SPIN("Off-Break", 82..94, 0.0f, 0.70f, false),
    LEG_SPIN("Leg-Spin / Googly", 78..90, 0.0f, -0.78f, false)
}

enum class FieldingPreset(val label: String) {
    ATTACKING("Attacking"),
    BALANCED("Balanced"),
    DEFENSIVE("Boundary Guard"),
    DEATH("Death Overs")
}

enum class DismissalType(val label: String) {
    BOWLED("b"),
    CAUGHT("c"),
    LBW("lbw"),
    RUN_OUT("run out"),
    STUMPED("st"),
    HIT_WICKET("hit wkt")
}

enum class ExtrasType {
    NONE, WIDE, NO_BALL, BYE, LEG_BYE
}

enum class GraphicsQuality(val label: String) {
    LOW("Battery Saver"),
    MEDIUM("Balanced"),
    HIGH("Ultra Broadcast")
}

data class BatterMatchStats(
    val player: CricketPlayer,
    val runs: Int = 0,
    val balls: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val isOut: Boolean = false,
    val dismissalText: String = "not out"
) {
    val strikeRate: Double
        get() = if (balls == 0) 0.0 else (runs * 100.0) / balls
}

data class BowlerMatchStats(
    val player: CricketPlayer,
    val legalBalls: Int = 0,
    val runsConceded: Int = 0,
    val wickets: Int = 0,
    val maidens: Int = 0,
    val wides: Int = 0,
    val noBalls: Int = 0
) {
    val oversFormatted: String
        get() = "${legalBalls / 6}.${legalBalls % 6}"
    val economy: Double
        get() = if (legalBalls == 0) 0.0 else (runsConceded * 6.0) / legalBalls
}

data class FallOfWicket(
    val wicketNumber: Int,
    val scoreAtFall: Int,
    val oversAtFall: String,
    val batterName: String
)

data class BallOutcome(
    val runsOffBat: Int,
    val extraRuns: Int = 0,
    val extrasType: ExtrasType = ExtrasType.NONE,
    val isLegalDelivery: Boolean = true,
    val isWicket: Boolean = false,
    val dismissalType: DismissalType? = null,
    val fielderName: String? = null,
    val isBoundaryFour: Boolean = false,
    val isBoundarySix: Boolean = false,
    val speedKmh: Float = 138f,
    val shotAngleDeg: Float = 0f,
    val distanceMeters: Float = 0f,
    val maxHeightMeters: Float = 0f,
    val pitchX: Float = 0f,
    val pitchZ: Float = 0.72f,
    val timingQuality: String = "GOOD",
    val commentary: String = "",
    val lbwPitchInLine: Boolean = false,
    val lbwImpactInLine: Boolean = false,
    val lbwHittingStumps: Boolean = false
) {
    val totalRuns: Int get() = runsOffBat + extraRuns
    val shortBadge: String
        get() = when {
            isWicket -> "W"
            extrasType == ExtrasType.WIDE -> "${1 + extraRuns - 1}Wd"
            extrasType == ExtrasType.NO_BALL -> "${runsOffBat}NB"
            isBoundarySix -> "6"
            isBoundaryFour -> "4"
            totalRuns == 0 -> "•"
            else -> totalRuns.toString()
        }
}

data class InningsState(
    val inningsNumber: Int,
    val battingTeam: CricketTeam,
    val bowlingTeam: CricketTeam,
    val runs: Int = 0,
    val wickets: Int = 0,
    val legalBalls: Int = 0,
    val extras: Int = 0,
    val strikerIndex: Int = 0,
    val nonStrikerIndex: Int = 1,
    val nextBatterIndex: Int = 2,
    val currentBowlerIndex: Int = 10,
    val lastBowlerIndex: Int = -1,
    val freeHitPending: Boolean = false,
    val batterStats: List<BatterMatchStats> = battingTeam.squad.map { BatterMatchStats(it) },
    val bowlerStats: List<BowlerMatchStats> = bowlingTeam.squad.map { BowlerMatchStats(it) },
    val fallOfWickets: List<FallOfWicket> = emptyList(),
    val currentOverBadges: List<String> = emptyList(),
    val recentBalls: List<BallOutcome> = emptyList(),
    val isDeclared: Boolean = false
) {
    val oversFormatted: String
        get() = "${legalBalls / 6}.${legalBalls % 6}"

    val runRate: Double
        get() = if (legalBalls == 0) 0.0 else (runs * 6.0) / legalBalls

    val striker: BatterMatchStats
        get() = batterStats.getOrElse(strikerIndex) { batterStats.first() }

    val nonStriker: BatterMatchStats
        get() = batterStats.getOrElse(nonStrikerIndex) { batterStats.first() }

    val currentBowler: BowlerMatchStats
        get() = bowlerStats.getOrElse(currentBowlerIndex) { bowlerStats.last() }
}

enum class MatchPhase {
    PRE_BALL,
    BOWLING_RUNUP,
    BALL_IN_FLIGHT_TO_BAT,
    BALL_IN_OUTFIELD,
    RUNNING_DECISION,
    INNINGS_BREAK,
    SUPER_OVER_PROMPT,
    MATCH_FINISHED
}

data class MatchState(
    val matchId: String,
    val mode: GameMode,
    val stadium: CricketStadium,
    val timeOfDay: TimeOfDay,
    val weather: WeatherCondition,
    val userTeam: CricketTeam,
    val opponentTeam: CricketTeam,
    val userBatsFirst: Boolean,
    val oversLimit: Int,
    val isUserCurrentlyBatting: Boolean,
    val inningsList: List<InningsState>,
    val activeInningsIndex: Int = 0,
    val phase: MatchPhase = MatchPhase.PRE_BALL,
    val fieldingPreset: FieldingPreset = FieldingPreset.BALANCED,
    val lastOutcome: BallOutcome? = null,
    val commentary: String = "Players are walking out to the middle!",
    val resultSummary: String = "",
    val winnerTeamId: String? = null,
    val isOnlineMatch: Boolean = false,
    val onlineRoomId: String? = null
) {
    val currentInnings: InningsState
        get() = inningsList[activeInningsIndex]

    val targetScore: Int?
        get() = when {
            !mode.isMultiInnings && activeInningsIndex == 1 -> inningsList[0].runs + 1
            mode.isMultiInnings && activeInningsIndex == 3 -> {
                val team0Total = inningsList[0].runs + inningsList.getOrNull(2)?.runs.orZero()
                val team1First = inningsList[1].runs
                (team0Total - team1First + 1).coerceAtLeast(1)
            }
            else -> null
        }

    val requiredRunRate: Double?
        get() {
            val target = targetScore ?: return null
            val runsNeeded = (target - currentInnings.runs).coerceAtLeast(0)
            val ballsLeft = (oversLimit * 6 - currentInnings.legalBalls).coerceAtLeast(0)
            if (ballsLeft == 0) return null
            return (runsNeeded * 6.0) / ballsLeft
        }
}

private fun Int?.orZero(): Int = this ?: 0

data class CareerProfileLocal(
    val playerName: String = "Aarav Sharma",
    val level: Int = 1,
    val xp: Int = 0,
    val skillPoints: Int = 3,
    val battingRating: Int = 72,
    val bowlingRating: Int = 68,
    val fieldingRating: Int = 74,
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    val totalRuns: Int = 0,
    val totalWickets: Int = 0,
    val highScore: Int = 0,
    val sixesHit: Int = 0,
    val foursHit: Int = 0
)

data class TournamentFixture(
    val id: String,
    val roundLabel: String,
    val homeTeamId: String,
    val awayTeamId: String,
    val winnerTeamId: String? = null,
    val scoreSummary: String = "Scheduled"
)

data class FranchiseStanding(
    val teamId: String,
    val played: Int = 0,
    val won: Int = 0,
    val lost: Int = 0,
    val points: Int = 0,
    val netRunRate: Double = 0.0
)

data class TournamentState(
    val title: String,
    val mode: GameMode,
    val userTeamId: String,
    val fixtures: List<TournamentFixture>,
    val standings: List<FranchiseStanding> = emptyList(),
    val championTeamId: String? = null
)
