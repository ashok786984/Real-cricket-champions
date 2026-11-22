package com.example

import com.example.data.model.BallOutcome
import com.example.data.model.DeliveryType
import com.example.data.model.DismissalType
import com.example.data.model.ExtrasType
import com.example.data.model.FieldingPreset
import com.example.data.model.GameMode
import com.example.data.model.MatchPhase
import com.example.data.model.ShotType
import com.example.data.model.TimeOfDay
import com.example.data.model.WeatherCondition
import com.example.engine.CricketAiEngine
import com.example.engine.CricketCatalog
import com.example.engine.CricketPhysics
import com.example.engine.CricketRulesEngine
import com.example.engine.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CricketEngineTest {

    @Test
    fun `catalog squads contain 11 players and tournament brackets generate cleanly`() {
        assertEquals(8, CricketCatalog.teams.size)
        CricketCatalog.teams.forEach { team ->
            assertEquals(11, team.squad.size)
        }
        val wc = CricketCatalog.buildWorldCup(CricketCatalog.teams[0].id)
        assertEquals(4, wc.fixtures.size)

        val fl = CricketCatalog.buildFranchiseLeague(CricketCatalog.teams[0].id)
        assertEquals(15, fl.fixtures.size)
        assertEquals(6, fl.standings.size)
    }

    @Test
    fun `rules engine handles legal over progression, strike rotation, and no-ball free hit`() {
        val teamA = CricketCatalog.teams[0]
        val teamB = CricketCatalog.teams[1]
        val stadium = CricketCatalog.stadiums[0]
        var state = CricketRulesEngine.createMatch(
            mode = GameMode.QUICK,
            userTeam = teamA,
            opponentTeam = teamB,
            stadium = stadium,
            timeOfDay = TimeOfDay.NIGHT,
            weather = WeatherCondition.CLEAR,
            userBatsFirst = true,
            oversLimit = 2
        )

        // Ball 1: No-ball + 2 runs -> legalBalls stays 0, runs = 3, freeHitPending = true
        state = CricketRulesEngine.applyBallOutcome(
            state,
            BallOutcome(runsOffBat = 2, extraRuns = 1, extrasType = ExtrasType.NO_BALL, isLegalDelivery = false)
        )
        assertEquals(3, state.currentInnings.runs)
        assertEquals(0, state.currentInnings.legalBalls)
        assertTrue(state.currentInnings.freeHitPending)

        // Complete 6 legal deliveries -> over ends, freeHit clears, striker rotates
        val initialStriker = state.currentInnings.strikerIndex
        for (i in 1..6) {
            state = CricketRulesEngine.applyBallOutcome(state, BallOutcome(runsOffBat = 0, isLegalDelivery = true))
        }
        assertEquals(6, state.currentInnings.legalBalls)
        assertEquals("1.0", state.currentInnings.oversFormatted)
        assertFalse(state.currentInnings.freeHitPending)
        assertTrue(state.currentInnings.strikerIndex != initialStriker)
    }

    @Test
    fun `rules engine handles wicket fall, innings break, and target chase victory`() {
        val teamA = CricketCatalog.teams[0]
        val teamB = CricketCatalog.teams[1]
        val stadium = CricketCatalog.stadiums[0]
        var state = CricketRulesEngine.createMatch(
            mode = GameMode.QUICK,
            userTeam = teamA,
            opponentTeam = teamB,
            stadium = stadium,
            timeOfDay = TimeOfDay.DAY,
            weather = WeatherCondition.CLEAR,
            userBatsFirst = true,
            oversLimit = 1
        )

        // 5 dot balls + 1 six -> 1st innings ends at 6/0
        repeat(5) {
            state = CricketRulesEngine.applyBallOutcome(state, BallOutcome(runsOffBat = 0))
        }
        state = CricketRulesEngine.applyBallOutcome(
            state,
            BallOutcome(runsOffBat = 6, isBoundarySix = true)
        )
        assertEquals(MatchPhase.INNINGS_BREAK, state.phase)

        state = CricketRulesEngine.startNextInnings(state)
        assertEquals(7, state.targetScore)

        // Wicket falls on ball 1, then 4 + 4 chases target -> opponent wins by 9 wickets
        state = CricketRulesEngine.applyBallOutcome(
            state,
            BallOutcome(runsOffBat = 0, isWicket = true, dismissalType = DismissalType.BOWLED)
        )
        assertEquals(1, state.currentInnings.wickets)
        state = CricketRulesEngine.applyBallOutcome(state, BallOutcome(runsOffBat = 4, isBoundaryFour = true))
        state = CricketRulesEngine.applyBallOutcome(state, BallOutcome(runsOffBat = 4, isBoundaryFour = true))

        assertEquals(MatchPhase.MATCH_FINISHED, state.phase)
        assertEquals(teamB.id, state.winnerTeamId)
        assertTrue(state.resultSummary.contains("9 wickets"))
    }

    @Test
    fun `physics projection and simulation produce valid bounded outcomes`() {
        val (xNear, yNear) = CricketPhysics.projectWorldToScreen(Vec3(0f, 0f, 0f))
        val (xFar, yFar) = CricketPhysics.projectWorldToScreen(Vec3(0f, 0f, 20f))
        assertEquals(0.5f, xNear, 0.001f)
        assertEquals(0.5f, xFar, 0.001f)
        assertTrue(yNear > yFar) // Closer points project lower on screen

        val teamA = CricketCatalog.teams[0]
        val teamB = CricketCatalog.teams[1]
        val outcome = CricketPhysics.simulateDelivery(
            batter = teamA.squad[0],
            bowler = teamB.squad[10],
            keeper = teamB.squad[3],
            delivery = DeliveryType.FAST_STRAIGHT,
            shotType = ShotType.LOFT,
            shotAngleDeg = 0f,
            timingOffset = 0.0f,
            pitchTargetX = 0.1f,
            pitchTargetZ = 0.5f,
            bowlingAccuracy = 0.9f,
            stadium = CricketCatalog.stadiums[0],
            weather = WeatherCondition.CLEAR,
            preset = FieldingPreset.BALANCED,
            squad = teamB.squad,
            freeHitActive = false,
            random = Random(42)
        )
        assertTrue(outcome.speedKmh in 70f..160f)
        assertNotNull(outcome.commentary)
    }

    @Test
    fun `ai engine adapts batting aggression and bowling strategy`() {
        val state = CricketRulesEngine.createMatch(
            mode = GameMode.T20,
            userTeam = CricketCatalog.teams[0],
            opponentTeam = CricketCatalog.teams[1],
            stadium = CricketCatalog.stadiums[0],
            timeOfDay = TimeOfDay.NIGHT,
            weather = WeatherCondition.OVERCAST,
            userBatsFirst = false,
            oversLimit = 20
        )
        val batPlan = CricketAiEngine.chooseBattingPlan(state, Random(7))
        val bowlPlan = CricketAiEngine.chooseBowlingPlan(state, Random(7))
        assertNotNull(batPlan.shotType)
        assertTrue(bowlPlan.accuracy in 0.4f..1.0f)
    }
}
