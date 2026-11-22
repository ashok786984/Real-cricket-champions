package com.example.engine

import com.example.data.model.BallOutcome
import com.example.data.model.CricketStadium
import com.example.data.model.CricketTeam
import com.example.data.model.ExtrasType
import com.example.data.model.FallOfWicket
import com.example.data.model.GameMode
import com.example.data.model.InningsState
import com.example.data.model.MatchPhase
import com.example.data.model.MatchState
import com.example.data.model.TimeOfDay
import com.example.data.model.WeatherCondition
import java.util.UUID

object CricketRulesEngine {

    fun createMatch(
        mode: GameMode,
        userTeam: CricketTeam,
        opponentTeam: CricketTeam,
        stadium: CricketStadium,
        timeOfDay: TimeOfDay,
        weather: WeatherCondition,
        userBatsFirst: Boolean,
        oversLimit: Int = mode.defaultOvers,
        isOnlineMatch: Boolean = false,
        onlineRoomId: String? = null
    ): MatchState {
        val battingFirst = if (userBatsFirst) userTeam else opponentTeam
        val bowlingFirst = if (userBatsFirst) opponentTeam else userTeam
        val firstInnings = InningsState(
            inningsNumber = 1,
            battingTeam = battingFirst,
            bowlingTeam = bowlingFirst
        )
        return MatchState(
            matchId = UUID.randomUUID().toString().take(12),
            mode = mode,
            stadium = stadium,
            timeOfDay = timeOfDay,
            weather = weather,
            userTeam = userTeam,
            opponentTeam = opponentTeam,
            userBatsFirst = userBatsFirst,
            oversLimit = oversLimit,
            isUserCurrentlyBatting = userBatsFirst,
            inningsList = listOf(firstInnings),
            activeInningsIndex = 0,
            phase = MatchPhase.PRE_BALL,
            commentary = "${battingFirst.name} win the toss decision and open the batting!",
            isOnlineMatch = isOnlineMatch,
            onlineRoomId = onlineRoomId
        )
    }

    /**
     * Applies a completed BallOutcome (including any extra runs from running between wickets)
     * according to official cricket laws: overs, extras, free hit, wickets, strike rotation,
     * innings transitions, target chase, and match winner resolution.
     */
    fun applyBallOutcome(state: MatchState, rawOutcome: BallOutcome): MatchState {
        if (state.phase == MatchPhase.MATCH_FINISHED) return state

        val innings = state.currentInnings
        val newBatters = innings.batterStats.toMutableList()
        val newBowlers = innings.bowlerStats.toMutableList()
        val newFow = innings.fallOfWickets.toMutableList()

        val strikerStats = newBatters[innings.strikerIndex]
        val bowlerStats = newBowlers[innings.currentBowlerIndex]

        val ballCountedToBatter = rawOutcome.extrasType != ExtrasType.WIDE
        val legalIncrement = if (rawOutcome.isLegalDelivery) 1 else 0
        val newLegalBalls = innings.legalBalls + legalIncrement

        // Update Batter stats
        newBatters[innings.strikerIndex] = strikerStats.copy(
            runs = strikerStats.runs + rawOutcome.runsOffBat,
            balls = strikerStats.balls + (if (ballCountedToBatter) 1 else 0),
            fours = strikerStats.fours + (if (rawOutcome.isBoundaryFour) 1 else 0),
            sixes = strikerStats.sixes + (if (rawOutcome.isBoundarySix) 1 else 0),
            isOut = rawOutcome.isWicket,
            dismissalText = if (rawOutcome.isWicket) {
                val type = rawOutcome.dismissalType?.label ?: "out"
                val f = rawOutcome.fielderName?.let { "$it " } ?: ""
                "$type $f(b ${bowlerStats.player.name})".trim()
            } else {
                strikerStats.dismissalText
            }
        )

        // Update Bowler stats
        val bowlerRuns = rawOutcome.runsOffBat +
            (if (rawOutcome.extrasType == ExtrasType.WIDE || rawOutcome.extrasType == ExtrasType.NO_BALL) rawOutcome.extraRuns else 0)
        newBowlers[innings.currentBowlerIndex] = bowlerStats.copy(
            legalBalls = bowlerStats.legalBalls + legalIncrement,
            runsConceded = bowlerStats.runsConceded + bowlerRuns,
            wickets = bowlerStats.wickets + (if (rawOutcome.isWicket && rawOutcome.dismissalType?.label != "run out") 1 else 0),
            wides = bowlerStats.wides + (if (rawOutcome.extrasType == ExtrasType.WIDE) 1 else 0),
            noBalls = bowlerStats.noBalls + (if (rawOutcome.extrasType == ExtrasType.NO_BALL) 1 else 0)
        )

        val newRuns = innings.runs + rawOutcome.totalRuns
        val newWickets = innings.wickets + (if (rawOutcome.isWicket) 1 else 0)
        val newExtras = innings.extras + rawOutcome.extraRuns

        var strikerIdx = innings.strikerIndex
        var nonStrikerIdx = innings.nonStrikerIndex
        var nextBatterIdx = innings.nextBatterIndex

        if (rawOutcome.isWicket) {
            newFow += FallOfWicket(
                wicketNumber = newWickets,
                scoreAtFall = newRuns,
                oversAtFall = "${newLegalBalls / 6}.${newLegalBalls % 6}",
                batterName = strikerStats.player.name
            )
            if (newWickets < 10 && nextBatterIdx < newBatters.size) {
                strikerIdx = nextBatterIdx
                nextBatterIdx++
            }
        } else if (rawOutcome.totalRuns % 2 == 1 && rawOutcome.extrasType != ExtrasType.WIDE) {
            val tmp = strikerIdx
            strikerIdx = nonStrikerIdx
            nonStrikerIdx = tmp
        }

        val overEnded = rawOutcome.isLegalDelivery && newLegalBalls % 6 == 0
        val updatedBadges = if (overEnded) emptyList() else (innings.currentOverBadges + rawOutcome.shortBadge)

        var bowlerIdx = innings.currentBowlerIndex
        var lastBowlerIdx = innings.lastBowlerIndex
        if (overEnded) {
            // Rotate strike at end of over
            val tmp = strikerIdx
            strikerIdx = nonStrikerIdx
            nonStrikerIdx = tmp

            lastBowlerIdx = bowlerIdx
            val oversBowledList = newBowlers.map { it.legalBalls / 6 }
            bowlerIdx = CricketAiEngine.selectNextBowlerIndex(
                squad = innings.bowlingTeam.squad,
                currentBowlerIndex = bowlerIdx,
                oversLimit = state.oversLimit,
                bowledOversByIdx = oversBowledList
            )
        }

        val updatedInnings = innings.copy(
            runs = newRuns,
            wickets = newWickets,
            legalBalls = newLegalBalls,
            extras = newExtras,
            strikerIndex = strikerIdx,
            nonStrikerIndex = nonStrikerIdx,
            nextBatterIndex = nextBatterIdx,
            currentBowlerIndex = bowlerIdx,
            lastBowlerIndex = lastBowlerIdx,
            freeHitPending = rawOutcome.extrasType == ExtrasType.NO_BALL,
            batterStats = newBatters,
            bowlerStats = newBowlers,
            fallOfWickets = newFow,
            currentOverBadges = updatedBadges,
            recentBalls = (innings.recentBalls + rawOutcome).takeLast(18)
        )

        val newInningsList = state.inningsList.toMutableList()
        newInningsList[state.activeInningsIndex] = updatedInnings

        // Evaluate innings/match completion
        val target = state.targetScore
        val oversExhausted = state.mode != GameMode.PRACTICE && newLegalBalls >= state.oversLimit * 6
        val allOut = newWickets >= 10
        val targetChased = target != null && newRuns >= target

        if (targetChased) {
            val wicketsRemaining = (10 - newWickets).coerceAtLeast(1)
            val winner = updatedInnings.battingTeam
            return state.copy(
                inningsList = newInningsList,
                lastOutcome = rawOutcome,
                phase = MatchPhase.MATCH_FINISHED,
                winnerTeamId = winner.id,
                resultSummary = "${winner.name} won by $wicketsRemaining wickets!",
                commentary = "${rawOutcome.commentary} ${winner.name} seal a famous victory!"
            )
        }

        if (oversExhausted || allOut) {
            return handleInningsEnd(
                state.copy(
                    inningsList = newInningsList,
                    lastOutcome = rawOutcome,
                    commentary = rawOutcome.commentary
                )
            )
        }

        return state.copy(
            inningsList = newInningsList,
            lastOutcome = rawOutcome,
            phase = MatchPhase.PRE_BALL,
            commentary = rawOutcome.commentary
        )
    }

    fun declareInnings(state: MatchState): MatchState {
        if (!state.mode.isMultiInnings || state.phase == MatchPhase.MATCH_FINISHED) return state
        val declared = state.currentInnings.copy(isDeclared = true)
        val list = state.inningsList.toMutableList()
        list[state.activeInningsIndex] = declared
        return handleInningsEnd(state.copy(inningsList = list))
    }

    private fun handleInningsEnd(state: MatchState): MatchState {
        val current = state.currentInnings
        if (!state.mode.isMultiInnings) {
            if (state.activeInningsIndex == 0) {
                return state.copy(
                    phase = MatchPhase.INNINGS_BREAK,
                    commentary = "End of 1st Innings! ${current.battingTeam.name}: ${current.runs}/${current.wickets}. Target is ${current.runs + 1}."
                )
            } else {
                val first = state.inningsList[0]
                return when {
                    current.runs > first.runs -> state.copy(
                        phase = MatchPhase.MATCH_FINISHED,
                        winnerTeamId = current.battingTeam.id,
                        resultSummary = "${current.battingTeam.name} won by ${10 - current.wickets} wickets!",
                        commentary = "${current.battingTeam.name} chase down ${first.runs + 1}!"
                    )
                    current.runs < first.runs -> {
                        val margin = first.runs - current.runs
                        state.copy(
                            phase = MatchPhase.MATCH_FINISHED,
                            winnerTeamId = first.battingTeam.id,
                            resultSummary = "${first.battingTeam.name} won by $margin runs!",
                            commentary = "${first.battingTeam.name} defend the target by $margin runs!"
                        )
                    }
                    else -> state.copy(
                        phase = MatchPhase.SUPER_OVER_PROMPT,
                        resultSummary = "Match Tied! Ready for Super Over!",
                        commentary = "SCORES LEVEL! We are heading to a thrilling Super Over!"
                    )
                }
            }
        } else {
            // Test match (up to 4 innings)
            if (state.activeInningsIndex < 3) {
                if (state.activeInningsIndex == 2) {
                    val t0 = state.inningsList[0].runs + state.inningsList[2].runs
                    val t1 = state.inningsList[1].runs
                    if (t0 < t1) {
                        val winner = state.inningsList[1].battingTeam
                        return state.copy(
                            phase = MatchPhase.MATCH_FINISHED,
                            winnerTeamId = winner.id,
                            resultSummary = "${winner.name} won by an innings and ${t1 - t0} runs!"
                        )
                    }
                }
                return state.copy(
                    phase = MatchPhase.INNINGS_BREAK,
                    commentary = "Innings ${state.activeInningsIndex + 1} complete (${current.runs}/${current.wickets})."
                )
            } else {
                val team0Total = state.inningsList[0].runs + state.inningsList[2].runs
                val team1Total = state.inningsList[1].runs + state.inningsList[3].runs
                val winner = if (team0Total >= team1Total) state.inningsList[0].battingTeam else state.inningsList[1].battingTeam
                val margin = if (team0Total > team1Total) "${team0Total - team1Total} runs" else "${10 - current.wickets} wickets"
                return state.copy(
                    phase = MatchPhase.MATCH_FINISHED,
                    winnerTeamId = winner.id,
                    resultSummary = if (team0Total == team1Total) "Test Match Tied!" else "${winner.name} won by $margin!"
                )
            }
        }
    }

    fun startNextInnings(state: MatchState): MatchState {
        val prev = state.currentInnings
        val nextInnings = InningsState(
            inningsNumber = state.activeInningsIndex + 2,
            battingTeam = prev.bowlingTeam,
            bowlingTeam = prev.battingTeam
        )
        return state.copy(
            inningsList = state.inningsList + nextInnings,
            activeInningsIndex = state.activeInningsIndex + 1,
            isUserCurrentlyBatting = nextInnings.battingTeam.id == state.userTeam.id,
            phase = MatchPhase.PRE_BALL,
            commentary = "${nextInnings.battingTeam.name} begin their chase!"
        )
    }

    fun startSuperOver(state: MatchState): MatchState {
        return createMatch(
            mode = GameMode.QUICK,
            userTeam = state.userTeam,
            opponentTeam = state.opponentTeam,
            stadium = state.stadium,
            timeOfDay = state.timeOfDay,
            weather = state.weather,
            userBatsFirst = !state.userBatsFirst,
            oversLimit = 1,
            isOnlineMatch = state.isOnlineMatch,
            onlineRoomId = state.onlineRoomId
        ).copy(commentary = "SUPER OVER! 6 balls each—everything on the line!")
    }
}
