package com.example.engine

import com.example.data.model.CricketPlayer
import com.example.data.model.DeliveryType
import com.example.data.model.FieldingPreset
import com.example.data.model.MatchState
import com.example.data.model.ShotType
import com.example.data.model.WeatherCondition
import kotlin.math.abs
import kotlin.random.Random

data class AiBattingPlan(
    val shotType: ShotType,
    val angleDeg: Float,
    val timingOffset: Float
)

data class AiBowlingPlan(
    val deliveryType: DeliveryType,
    val pitchX: Float,
    val pitchZ: Float,
    val accuracy: Float,
    val fieldingPreset: FieldingPreset
)

object CricketAiEngine {

    fun chooseBattingPlan(matchState: MatchState, random: Random = Random.Default): AiBattingPlan {
        val innings = matchState.currentInnings
        val batter = innings.striker.player
        val oversLeft = (matchState.oversLimit - innings.legalBalls / 6.0).coerceAtLeast(0.2)
        val rrr = matchState.requiredRunRate ?: 7.0
        val wicketsInHand = 10 - innings.wickets

        val aggression = when {
            innings.freeHitPending -> 0.95f
            matchState.mode.isMultiInnings -> 0.30f + (wicketsInHand * 0.02f)
            rrr > 11.5 -> 0.92f
            rrr > 8.5 -> 0.72f
            oversLeft < 3.0 && wicketsInHand >= 4 -> 0.88f
            wicketsInHand <= 3 -> 0.38f
            else -> 0.56f
        }.coerceIn(0.15f, 0.98f)

        val shot = when {
            innings.freeHitPending -> ShotType.LOFT
            random.nextFloat() < aggression * 0.42f -> ShotType.LOFT
            random.nextFloat() < aggression * 0.65f -> ShotType.PULL
            random.nextFloat() < 0.75f -> ShotType.DRIVE
            else -> ShotType.DEFEND
        }

        val angles = listOf(-120f, -75f, -35f, 0f, 35f, 70f, 115f, 145f)
        val angle = angles[random.nextInt(angles.size)]

        // Skill determines timing variance around 0.0
        val maxSpread = ((100 - batter.battingSkill) / 100f * 0.65f + 0.14f).coerceIn(0.14f, 0.78f)
        val timingOffset = ((random.nextFloat() * 2f - 1f) * maxSpread)

        return AiBattingPlan(shot, angle, timingOffset)
    }

    fun chooseBowlingPlan(matchState: MatchState, random: Random = Random.Default): AiBowlingPlan {
        val innings = matchState.currentInnings
        val bowler = innings.currentBowler.player
        val oversProgress = innings.legalBalls.toFloat() / (matchState.oversLimit * 6f).coerceAtLeast(6f)
        val rrr = matchState.requiredRunRate ?: 7.5
        val wicketsLeft = 10 - innings.wickets

        val preset = when {
            oversProgress > 0.78f -> FieldingPreset.DEATH
            rrr > 9.5 || wicketsLeft <= 4 -> FieldingPreset.DEFENSIVE
            innings.legalBalls < 18 || matchState.mode.isMultiInnings -> FieldingPreset.ATTACKING
            else -> FieldingPreset.BALANCED
        }

        val delivery = if (bowler.isSpinBowler) {
            if (random.nextFloat() < 0.55f) DeliveryType.OFF_SPIN else DeliveryType.LEG_SPIN
        } else {
            when {
                oversProgress > 0.78f && random.nextFloat() < 0.55f -> DeliveryType.YORKER
                matchState.weather == WeatherCondition.OVERCAST && random.nextFloat() < 0.65f ->
                    if (random.nextBoolean()) DeliveryType.OUTSWINGER else DeliveryType.INSWINGER
                random.nextFloat() < 0.22f -> DeliveryType.BOUNCER
                else -> DeliveryType.FAST_STRAIGHT
            }
        }

        val pitchZ = when (delivery) {
            DeliveryType.YORKER -> 0.10f
            DeliveryType.BOUNCER -> 0.90f
            DeliveryType.OFF_SPIN, DeliveryType.LEG_SPIN -> 0.34f + random.nextFloat() * 0.24f
            else -> 0.44f + random.nextFloat() * 0.28f
        }

        val pitchX = when (delivery) {
            DeliveryType.OUTSWINGER -> 0.28f
            DeliveryType.INSWINGER -> -0.10f
            DeliveryType.YORKER -> (random.nextFloat() - 0.5f) * 0.22f
            else -> 0.18f + (random.nextFloat() - 0.5f) * 0.32f
        }

        val accuracy = (0.58f + (bowler.bowlingSkill / 100f) * 0.36f - abs(random.nextFloat() * 0.12f))
            .coerceIn(0.45f, 0.98f)

        return AiBowlingPlan(delivery, pitchX, pitchZ, accuracy, preset)
    }

    fun selectNextBowlerIndex(
        squad: List<CricketPlayer>,
        currentBowlerIndex: Int,
        oversLimit: Int,
        bowledOversByIdx: List<Int>
    ): Int {
        val maxOversPerBowler = when {
            oversLimit <= 5 -> 2
            oversLimit <= 10 -> 3
            oversLimit <= 20 -> 4
            oversLimit <= 50 -> 10
            else -> 99
        }
        val candidates = squad.indices.filter { idx ->
            idx != currentBowlerIndex &&
                squad[idx].bowlingSkill >= 55 &&
                (bowledOversByIdx.getOrElse(idx) { 0 } < maxOversPerBowler)
        }
        if (candidates.isNotEmpty()) {
            return candidates.maxByOrNull { squad[it].bowlingSkill } ?: candidates.first()
        }
        return squad.indices.firstOrNull { it != currentBowlerIndex } ?: 10
    }
}
