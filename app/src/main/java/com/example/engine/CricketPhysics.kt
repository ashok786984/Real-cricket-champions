package com.example.engine

import com.example.data.model.BallOutcome
import com.example.data.model.CricketPlayer
import com.example.data.model.CricketStadium
import com.example.data.model.DeliveryType
import com.example.data.model.DismissalType
import com.example.data.model.ExtrasType
import com.example.data.model.FieldingPreset
import com.example.data.model.ShotType
import com.example.data.model.WeatherCondition
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class Vec3(val x: Float, val y: Float, val z: Float)

data class FielderPosition(
    val name: String,
    val xMeters: Float,
    val zMeters: Float,
    val fieldingSkill: Int = 75
)

object CricketPhysics {

    /**
     * Generates 9 outfielders (excluding bowler & wicketkeeper) based on preset.
     * Coordinates are centered on striker's stumps (0, 0), +Z is toward bowler/straight boundary,
     * +X is off-side (for right-handed batsman).
     */
    fun getFielderPositions(preset: FieldingPreset, squad: List<CricketPlayer>): List<FielderPosition> {
        val baseCoords = when (preset) {
            FieldingPreset.ATTACKING -> listOf(
                -4f to 2f,    // First Slip
                8f to 6f,     // Gully
                20f to 16f,   // Cover
                14f to 27f,   // Mid-Off
                -14f to 27f,  // Mid-On
                -22f to 15f,  // Mid-Wicket
                -26f to 2f,   // Square Leg
                -18f to -18f, // Fine Leg
                22f to -16f   // Third Man
            )
            FieldingPreset.BALANCED -> listOf(
                -4f to 2f,    // Slip
                26f to 14f,   // Cover Point
                18f to 36f,   // Mid-Off
                -16f to 35f,  // Mid-On
                -30f to 18f,  // Mid-Wicket
                -42f to 4f,   // Deep Square Leg
                -28f to -28f, // Fine Leg
                34f to -22f,  // Third Man
                48f to 24f    // Deep Cover
            )
            FieldingPreset.DEFENSIVE -> listOf(
                45f to 20f,   // Deep Cover
                32f to 52f,   // Long-Off
                -28f to 54f,  // Long-On
                -46f to 26f,  // Deep Mid-Wicket
                -56f to -4f,  // Deep Square
                -34f to -42f, // Deep Fine Leg
                42f to -38f,  // Deep Third Man
                18f to 25f,   // Extra Cover
                -18f to 24f   // Mid-Wicket Ring
            )
            FieldingPreset.DEATH -> listOf(
                30f to 58f,   // Long-Off Rope
                -26f to 59f,  // Long-On Rope
                -52f to 28f,  // Cow Corner
                -58f to -2f,  // Deep Square Rope
                54f to 24f,   // Deep Cover Rope
                -38f to -46f, // Deep Fine Leg
                42f to -42f,  // Deep Third Man
                16f to 26f,   // Ring Off
                -16f to 24f   // Ring On
            )
        }
        return baseCoords.mapIndexed { i, (x, z) ->
            val fielder = squad.getOrElse((i + 1) % squad.size) { squad.first() }
            FielderPosition(fielder.name, x, z, fielder.fieldingSkill)
        }
    }

    /**
     * Simulates ball trajectory and bat/ball collision physics for one delivery.
     *
     * @param timingOffset -1.0 (very early) to +1.0 (very late); 0.0 is dead-center sweet spot.
     * @param pitchTargetX Lateral aim on pitch (-1.0 leg side .. +1.0 off side, 0.0 middle stump).
     * @param pitchTargetZ Length down pitch (0.0 yorker at bat .. 1.0 short pitched).
     * @param shotAngleDeg 0 = straight down ground, +90 = cover/point, -90 = square leg, 180 = fine leg/third man.
     * @param bowlingAccuracy 0.0..1.0 (1.0 = pinpoint bowler accuracy).
     */
    fun simulateDelivery(
        batter: CricketPlayer,
        bowler: CricketPlayer,
        keeper: CricketPlayer,
        delivery: DeliveryType,
        shotType: ShotType,
        shotAngleDeg: Float,
        timingOffset: Float,
        pitchTargetX: Float,
        pitchTargetZ: Float,
        bowlingAccuracy: Float,
        stadium: CricketStadium,
        weather: WeatherCondition,
        preset: FieldingPreset,
        squad: List<CricketPlayer>,
        freeHitActive: Boolean,
        random: Random = Random.Default
    ): BallOutcome {
        val errScale = (1f - bowlingAccuracy).coerceIn(0f, 1f)
        val actualPitchX = (pitchTargetX + (random.nextFloat() - 0.5f) * 0.65f * errScale).coerceIn(-1.45f, 1.45f)
        val actualPitchZ = (pitchTargetZ + (random.nextFloat() - 0.5f) * 0.25f * errScale).coerceIn(0.05f, 1.0f)

        val baseSpeed = delivery.speedKmhRange.first +
            (delivery.speedKmhRange.last - delivery.speedKmhRange.first) * (bowler.bowlingSkill / 100f)
        val speedKmh = (baseSpeed + (random.nextFloat() - 0.5f) * 5f).coerceIn(72f, 155f)

        // Check No-Ball (overstepping when bowling accuracy very poor)
        val isNoBall = !freeHitActive && bowlingAccuracy < 0.20f && random.nextFloat() < 0.55f

        // Check Wide (too far outside off/leg and batter leaves or misses)
        val swingMovement = delivery.swingBase * weather.swingMultiplier * 0.26f
        val spinMovement = delivery.spinBase * weather.spinMultiplier * stadium.pitchTurn * 0.34f
        val arrivalX = (actualPitchX + swingMovement + spinMovement).coerceIn(-1.6f, 1.6f)
        val isWideLine = abs(arrivalX) > 1.05f || (delivery == DeliveryType.BOUNCER && actualPitchZ > 0.96f && random.nextFloat() < 0.35f)

        if (isWideLine && (shotType == ShotType.LEAVE || abs(timingOffset) > 0.55f) && !isNoBall) {
            return BallOutcome(
                runsOffBat = 0,
                extraRuns = 1,
                extrasType = ExtrasType.WIDE,
                isLegalDelivery = false,
                speedKmh = speedKmh,
                pitchX = actualPitchX,
                pitchZ = actualPitchZ,
                timingQuality = "WIDE",
                commentary = "Wide signalled! Drifting well outside the tramline."
            )
        }

        // Handle LEAVE
        if (shotType == ShotType.LEAVE) {
            val hitsStumps = abs(arrivalX) < 0.26f && actualPitchZ < 0.62f
            if (hitsStumps && !freeHitActive) {
                return BallOutcome(
                    runsOffBat = 0,
                    isLegalDelivery = !isNoBall,
                    extraRuns = if (isNoBall) 1 else 0,
                    extrasType = if (isNoBall) ExtrasType.NO_BALL else ExtrasType.NONE,
                    isWicket = !isNoBall,
                    dismissalType = if (!isNoBall) DismissalType.BOWLED else null,
                    speedKmh = speedKmh,
                    pitchX = actualPitchX,
                    pitchZ = actualPitchZ,
                    timingQuality = "SHOULDERED ARMS",
                    commentary = if (isNoBall) "Bowled off a NO-BALL! Saved by the siren!" else "TIMBER! Left it alone and lost off stump!"
                )
            }
            return BallOutcome(
                runsOffBat = 0,
                extraRuns = if (isNoBall) 1 else 0,
                extrasType = if (isNoBall) ExtrasType.NO_BALL else ExtrasType.NONE,
                isLegalDelivery = !isNoBall,
                speedKmh = speedKmh,
                pitchX = actualPitchX,
                pitchZ = actualPitchZ,
                timingQuality = "GOOD LEAVE",
                commentary = "Watchful leave outside off, carries comfortably to ${keeper.name}."
            )
        }

        // Compute Bat-Ball contact quality
        val absTiming = abs(timingOffset).coerceIn(0f, 1f)
        val skillDiff = (batter.battingSkill - bowler.bowlingSkill) / 100f
        val timingWindowBonus = (skillDiff * 0.14f).coerceIn(-0.10f, 0.12f)
        val effectiveMiss = (absTiming - timingWindowBonus).coerceIn(0f, 1f)

        val timingLabel = when {
            effectiveMiss < 0.16f -> "PERFECT"
            effectiveMiss < 0.34f -> "GOOD"
            effectiveMiss < 0.56f -> "EARLY/LATE"
            else -> "MISTIMED"
        }

        // Complete beat / bowled / LBW check when timing is terrible
        if (effectiveMiss > 0.68f && random.nextFloat() < (0.45f + shotType.riskFactor)) {
            val hittingStumps = abs(arrivalX) < 0.28f
            val pitchInLine = actualPitchX > -0.35f
            val impactInLine = abs(arrivalX) < 0.30f

            if (hittingStumps && !freeHitActive) {
                val bowledRoll = random.nextFloat()
                return if (bowledRoll < 0.58f) {
                    BallOutcome(
                        runsOffBat = 0,
                        extraRuns = if (isNoBall) 1 else 0,
                        extrasType = if (isNoBall) ExtrasType.NO_BALL else ExtrasType.NONE,
                        isLegalDelivery = !isNoBall,
                        isWicket = !isNoBall,
                        dismissalType = if (!isNoBall) DismissalType.BOWLED else null,
                        speedKmh = speedKmh,
                        pitchX = actualPitchX,
                        pitchZ = actualPitchZ,
                        timingQuality = "BEATEN",
                        commentary = "CLEAN BOWLED! ${bowler.name} shatters the timber!"
                    )
                } else {
                    val lbwOut = pitchInLine && impactInLine && actualPitchZ in 0.15f..0.72f
                    BallOutcome(
                        runsOffBat = 0,
                        extraRuns = if (isNoBall) 1 else 0,
                        extrasType = if (isNoBall) ExtrasType.NO_BALL else ExtrasType.NONE,
                        isLegalDelivery = !isNoBall,
                        isWicket = lbwOut && !isNoBall,
                        dismissalType = if (lbwOut && !isNoBall) DismissalType.LBW else null,
                        speedKmh = speedKmh,
                        pitchX = actualPitchX,
                        pitchZ = actualPitchZ,
                        timingQuality = "TRAPPED",
                        lbwPitchInLine = pitchInLine,
                        lbwImpactInLine = impactInLine,
                        lbwHittingStumps = lbwOut,
                        commentary = if (lbwOut) "UP GOES THE FINGER! Plumb LBW in front!" else "Huge appeal for LBW—just sliding down leg!"
                    )
                }
            }

            // Thick edge behind to keeper/slip
            if (random.nextFloat() < 0.35f && !freeHitActive) {
                return BallOutcome(
                    runsOffBat = 0,
                    extraRuns = if (isNoBall) 1 else 0,
                    extrasType = if (isNoBall) ExtrasType.NO_BALL else ExtrasType.NONE,
                    isLegalDelivery = !isNoBall,
                    isWicket = !isNoBall,
                    dismissalType = if (!isNoBall) DismissalType.CAUGHT else null,
                    fielderName = keeper.name,
                    speedKmh = speedKmh,
                    shotAngleDeg = 165f,
                    distanceMeters = 6f,
                    pitchX = actualPitchX,
                    pitchZ = actualPitchZ,
                    timingQuality = "OUTSIDE EDGE",
                    commentary = "EDGED AND TAKEN! Sharp grab behind the stumps by ${keeper.name}!"
                )
            }

            return BallOutcome(
                runsOffBat = 0,
                extraRuns = if (isNoBall) 1 else 0,
                extrasType = if (isNoBall) ExtrasType.NO_BALL else ExtrasType.NONE,
                isLegalDelivery = !isNoBall,
                speedKmh = speedKmh,
                pitchX = actualPitchX,
                pitchZ = actualPitchZ,
                timingQuality = "SWING & A MISS",
                commentary = "Beaten all ends up! Whizzes past the outside edge."
            )
        }

        // Compute exit velocity & distance
        val contactQuality = (1f - effectiveMiss * 0.92f).coerceIn(0.18f, 1.0f)
        val powerFactor = (batter.battingSkill / 85f) * shotType.powerMultiplier * contactQuality
        val rawDistance = (powerFactor * (stadium.boundaryMeters + 18f) * (0.85f + random.nextFloat() * 0.28f))
            .coerceIn(4f, 118f)
        val maxHeight = if (shotType.lofted) (8f + contactQuality * 22f) else (0.5f + random.nextFloat() * 3.5f)

        val fielders = getFielderPositions(preset, squad)
        val rad = (shotAngleDeg * PI / 180.0).toFloat()
        val hitX = sin(rad) * rawDistance
        val hitZ = cos(rad) * rawDistance

        // Check boundary Six
        if (shotType.lofted && rawDistance >= stadium.boundaryMeters && maxHeight >= 5f) {
            return BallOutcome(
                runsOffBat = 6,
                extraRuns = if (isNoBall) 1 else 0,
                extrasType = if (isNoBall) ExtrasType.NO_BALL else ExtrasType.NONE,
                isLegalDelivery = !isNoBall,
                isBoundarySix = true,
                speedKmh = speedKmh,
                shotAngleDeg = shotAngleDeg,
                distanceMeters = rawDistance,
                maxHeightMeters = maxHeight,
                pitchX = actualPitchX,
                pitchZ = actualPitchZ,
                timingQuality = timingLabel,
                commentary = "MAXIMUM SIX! Launched ${rawDistance.toInt()}m into the stands!"
            )
        }

        // Check lofted catch in the deep
        val nearestFielder = fielders.minByOrNull { f ->
            val dx = f.xMeters - hitX
            val dz = f.zMeters - hitZ
            sqrt(dx * dx + dz * dz)
        }
        val fielderDist = nearestFielder?.let { f ->
            val dx = f.xMeters - hitX
            val dz = f.zMeters - hitZ
            sqrt(dx * dx + dz * dz)
        } ?: 99f

        if (shotType.lofted && !freeHitActive && fielderDist < (11f + (1f - contactQuality) * 14f) && rawDistance < stadium.boundaryMeters) {
            val catchProb = (0.45f + (nearestFielder?.fieldingSkill ?: 75) / 200f) * (0.4f + effectiveMiss)
            if (random.nextFloat() < catchProb) {
                return BallOutcome(
                    runsOffBat = 0,
                    extraRuns = if (isNoBall) 1 else 0,
                    extrasType = if (isNoBall) ExtrasType.NO_BALL else ExtrasType.NONE,
                    isLegalDelivery = !isNoBall,
                    isWicket = !isNoBall,
                    dismissalType = if (!isNoBall) DismissalType.CAUGHT else null,
                    fielderName = nearestFielder?.name ?: "Fielder",
                    speedKmh = speedKmh,
                    shotAngleDeg = shotAngleDeg,
                    distanceMeters = rawDistance,
                    maxHeightMeters = maxHeight,
                    pitchX = actualPitchX,
                    pitchZ = actualPitchZ,
                    timingQuality = timingLabel,
                    commentary = "IN THE AIR... CAUGHT! ${nearestFielder?.name} settles underneath it!"
                )
            }
        }

        // Check boundary Four
        if (rawDistance >= stadium.boundaryMeters && fielderDist > 7.5f) {
            return BallOutcome(
                runsOffBat = 4,
                extraRuns = if (isNoBall) 1 else 0,
                extrasType = if (isNoBall) ExtrasType.NO_BALL else ExtrasType.NONE,
                isLegalDelivery = !isNoBall,
                isBoundaryFour = true,
                speedKmh = speedKmh,
                shotAngleDeg = shotAngleDeg,
                distanceMeters = stadium.boundaryMeters.toFloat(),
                maxHeightMeters = maxHeight,
                pitchX = actualPitchX,
                pitchZ = actualPitchZ,
                timingQuality = timingLabel,
                commentary = "CRACKING FOUR! Races past the ropes in a flash!"
            )
        }

        // Non-boundary runs based on distance & fielder gap
        val baseRuns = when {
            rawDistance < 12f -> 0
            fielderDist < 5.5f && rawDistance < 28f -> 0
            rawDistance < 34f -> 1
            rawDistance < 54f -> if (fielderDist > 12f) 2 else 1
            else -> if (fielderDist > 14f) 3 else 2
        }

        val comm = when (baseRuns) {
            0 -> "Solidly struck straight to ${nearestFielder?.name ?: "the fielder"}. No run."
            1 -> "Placed neatly into the gap for a quick single."
            2 -> "Good running between the wickets! Comfortable two."
            else -> "Deep in the outfield—excellent hustle for three runs!"
        }

        return BallOutcome(
            runsOffBat = baseRuns,
            extraRuns = if (isNoBall) 1 else 0,
            extrasType = if (isNoBall) ExtrasType.NO_BALL else ExtrasType.NONE,
            isLegalDelivery = !isNoBall,
            speedKmh = speedKmh,
            shotAngleDeg = shotAngleDeg,
            distanceMeters = rawDistance,
            maxHeightMeters = maxHeight,
            pitchX = actualPitchX,
            pitchZ = actualPitchZ,
            timingQuality = timingLabel,
            commentary = if (isNoBall) "NO BALL! $comm" else comm
        )
    }

    /**
     * Projects 3D world coordinates (meters: x lateral, y height, z down-ground from batter)
     * into normalized 0..1 screen coordinates for the perspective camera behind the bowler/batter.
     */
    fun projectWorldToScreen(world: Vec3): Pair<Float, Float> {
        // Camera sits behind batsman/keeper at z = -14m, height = 8.5m looking down +Z
        val camZ = -14f
        val depth = (world.z - camZ).coerceAtLeast(3f)
        val scale = 26f / depth
        val screenX = 0.5f + (world.x * scale * 0.032f)
        val horizonY = 0.34f
        val groundScreenY = horizonY + (12.5f / depth) * 0.52f
        val screenY = groundScreenY - (world.y * scale * 0.034f)
        return screenX to screenY
    }
}
