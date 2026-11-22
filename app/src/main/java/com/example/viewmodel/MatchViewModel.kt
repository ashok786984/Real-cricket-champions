package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BallOutcome
import com.example.data.model.DeliveryType
import com.example.data.model.DismissalType
import com.example.data.model.FieldingPreset
import com.example.data.model.MatchPhase
import com.example.data.model.MatchState
import com.example.data.model.ShotType
import com.example.engine.CricketAiEngine
import com.example.engine.CricketPhysics
import com.example.engine.CricketRulesEngine
import com.example.engine.Vec3
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class AnimationFrameState(
    val ballPos: Vec3 = Vec3(0f, 1.8f, 20.12f),
    val ballVisible: Boolean = false,
    val bowlerRunup: Float = 0f,
    val batterSwing: Float = 0f,
    val timingCursor: Float = 0f, // -1f .. +1f; 0f is green sweet spot
    val paceCursor: Float = 0f,   // 0f .. 1f; 0.5f is pinpoint speed/accuracy
    val stumpsBroken: Boolean = false,
    val trailPoints: List<Vec3> = emptyList(),
    val pendingRunOutcome: BallOutcome? = null,
    val pendingRunningRuns: Int = 0
)

class MatchViewModel(
    initialMatch: MatchState,
    private val onMatchFinishedCallback: (MatchState) -> Unit = {},
    private val onOnlineBallSynced: ((MatchState, BallOutcome) -> Unit)? = null
) : ViewModel() {

    private val _matchState = MutableStateFlow(initialMatch)
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    private val _animState = MutableStateFlow(AnimationFrameState())
    val animState: StateFlow<AnimationFrameState> = _animState.asStateFlow()

    // User Batting Controls
    private val _selectedShotType = MutableStateFlow(ShotType.DRIVE)
    val selectedShotType: StateFlow<ShotType> = _selectedShotType.asStateFlow()

    private val _selectedShotAngle = MutableStateFlow(25f) // degrees
    val selectedShotAngle: StateFlow<Float> = _selectedShotAngle.asStateFlow()

    // User Bowling Controls
    private val _selectedDelivery = MutableStateFlow(DeliveryType.FAST_STRAIGHT)
    val selectedDelivery: StateFlow<DeliveryType> = _selectedDelivery.asStateFlow()

    private val _pitchTargetX = MutableStateFlow(0.18f)
    val pitchTargetX: StateFlow<Float> = _pitchTargetX.asStateFlow()

    private val _pitchTargetZ = MutableStateFlow(0.55f)
    val pitchTargetZ: StateFlow<Float> = _pitchTargetZ.asStateFlow()

    private val _showHawkeyeReplay = MutableStateFlow(false)
    val showHawkeyeReplay: StateFlow<Boolean> = _showHawkeyeReplay.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private var activeBallJob: Job? = null
    private var swingTapped: Boolean = false
    private var capturedTimingOffset: Float = 0.0f
    private var paceReleaseTapped: Boolean = false
    private var capturedBowlingAccuracy: Float = 0.85f
    private var completionReported: Boolean = false

    fun selectShotType(shot: ShotType) {
        _selectedShotType.value = shot
    }

    fun selectShotAngle(angleDeg: Float) {
        _selectedShotAngle.value = angleDeg
    }

    fun selectDelivery(delivery: DeliveryType) {
        _selectedDelivery.value = delivery
    }

    fun setPitchTarget(x: Float, z: Float) {
        _pitchTargetX.value = x.coerceIn(-1.2f, 1.2f)
        _pitchTargetZ.value = z.coerceIn(0.08f, 0.96f)
    }

    fun setFieldingPreset(preset: FieldingPreset) {
        _matchState.update { it.copy(fieldingPreset = preset) }
    }

    fun togglePause(paused: Boolean) {
        _isPaused.value = paused
    }

    fun toggleHawkeyeReplay(show: Boolean) {
        _showHawkeyeReplay.value = show
    }

    fun togglePracticeRole() {
        _matchState.update {
            it.copy(
                isUserCurrentlyBatting = !it.isUserCurrentlyBatting,
                phase = MatchPhase.PRE_BALL,
                commentary = if (!it.isUserCurrentlyBatting) "Practice Nets: You are now Batting!" else "Practice Nets: You are now Bowling!"
            )
        }
    }

    fun startDelivery() {
        val state = _matchState.value
        if (state.phase != MatchPhase.PRE_BALL || _isPaused.value) return
        activeBallJob?.cancel()

        if (state.isUserCurrentlyBatting) {
            runUserBattingDelivery()
        } else {
            runUserBowlingDelivery()
        }
    }

    fun tapSwing() {
        if (_matchState.value.phase == MatchPhase.BALL_IN_FLIGHT_TO_BAT && !swingTapped) {
            swingTapped = true
            capturedTimingOffset = _animState.value.timingCursor
        }
    }

    fun tapBowlingRelease() {
        if (_matchState.value.phase == MatchPhase.BOWLING_RUNUP && !paceReleaseTapped) {
            paceReleaseTapped = true
            val distFromCenter = abs(_animState.value.paceCursor - 0.5f) * 2f
            capturedBowlingAccuracy = (1f - distFromCenter * 0.92f).coerceIn(0.12f, 0.99f)
        }
    }

    fun pushExtraRun() {
        val pending = _animState.value.pendingRunOutcome ?: return
        if (_matchState.value.phase != MatchPhase.RUNNING_DECISION) return
        val nextRuns = _animState.value.pendingRunningRuns + 1
        // Risk of run out increases sharply on 2nd/3rd/4th run
        val runOutRisk = when {
            nextRuns <= 1 -> 0.04f
            nextRuns == 2 -> 0.14f
            nextRuns == 3 -> 0.35f
            else -> 0.65f
        }
        if (Random.nextFloat() < runOutRisk && !_matchState.value.currentInnings.freeHitPending) {
            val runOutOutcome = pending.copy(
                runsOffBat = (nextRuns - 1).coerceAtLeast(0),
                isWicket = true,
                dismissalType = DismissalType.RUN_OUT,
                fielderName = "Direct Hit",
                commentary = "DIRECT HIT! Caught short of the crease going for run #$nextRuns!"
            )
            finishBallWithOutcome(runOutOutcome)
        } else if (nextRuns >= 3) {
            finishBallWithOutcome(
                pending.copy(
                    runsOffBat = nextRuns,
                    commentary = "Superb athleticism! Sprinted $nextRuns runs!"
                )
            )
        } else {
            _animState.update { it.copy(pendingRunningRuns = nextRuns) }
        }
    }

    fun holdCrease() {
        val pending = _animState.value.pendingRunOutcome ?: return
        if (_matchState.value.phase != MatchPhase.RUNNING_DECISION) return
        val finalRuns = _animState.value.pendingRunningRuns.coerceAtLeast(pending.runsOffBat)
        finishBallWithOutcome(
            pending.copy(
                runsOffBat = finalRuns,
                commentary = if (finalRuns == 0) pending.commentary else "Completed $finalRuns run${if (finalRuns > 1) "s" else ""} safely."
            )
        )
    }

    private fun runUserBattingDelivery() {
        activeBallJob = viewModelScope.launch {
            val state = _matchState.value
            val innings = state.currentInnings
            val aiBowl = CricketAiEngine.chooseBowlingPlan(state)

            swingTapped = false
            capturedTimingOffset = 0.85f // default late if player doesn't swing

            _matchState.update {
                it.copy(
                    phase = MatchPhase.BOWLING_RUNUP,
                    fieldingPreset = aiBowl.fieldingPreset,
                    commentary = "${innings.currentBowler.player.name} runs in with ${aiBowl.deliveryType.label}..."
                )
            }

            // 1. Bowler runup (500ms)
            val runupSteps = 15
            for (i in 1..runupSteps) {
                while (_isPaused.value) delay(80L)
                _animState.update {
                    it.copy(
                        bowlerRunup = i / runupSteps.toFloat(),
                        stumpsBroken = false,
                        batterSwing = 0f,
                        trailPoints = emptyList()
                    )
                }
                delay(28L)
            }

            // 2. Ball in flight to bat (900ms interactive timing window!)
            _matchState.update { it.copy(phase = MatchPhase.BALL_IN_FLIGHT_TO_BAT) }
            val trail = mutableListOf<Vec3>()
            val flightSteps = 30
            val flightDelay = if (aiBowl.deliveryType.isFast) 28L else 36L
            for (i in 0..flightSteps) {
                while (_isPaused.value) delay(80L)
                val t = i / flightSteps.toFloat()
                val z = 20.12f * (1f - t)
                val bounceT = aiBowl.pitchZ.coerceIn(0.15f, 0.85f)
                val y = if (t < bounceT) {
                    val u = t / bounceT
                    2.0f * (1f - u) + 0.08f
                } else {
                    val u = (t - bounceT) / (1f - bounceT).coerceAtLeast(0.05f)
                    0.08f + sin(u * PI).toFloat() * (if (aiBowl.deliveryType == DeliveryType.BOUNCER) 1.55f else 0.78f)
                }
                val x = aiBowl.pitchX * 0.9f + (aiBowl.deliveryType.swingBase * state.weather.swingMultiplier * t * 0.35f)
                val pos = Vec3(x, y, z)
                trail += pos

                // Timing cursor sweeps -1.0 -> +1.0, crossing 0.0 at t == 0.78
                val cursor = ((t - 0.78f) / 0.22f).coerceIn(-1f, 1f)
                _animState.update {
                    it.copy(
                        ballPos = pos,
                        ballVisible = true,
                        timingCursor = cursor,
                        trailPoints = trail.toList(),
                        batterSwing = if (swingTapped) 1f else 0f
                    )
                }
                delay(flightDelay)
            }

            // 3. Solve bat-ball physics
            val shot = if (!swingTapped && _selectedShotType.value != ShotType.LEAVE) ShotType.DEFEND else _selectedShotType.value
            val outcome = CricketPhysics.simulateDelivery(
                batter = innings.striker.player,
                bowler = innings.currentBowler.player,
                keeper = innings.bowlingTeam.squad[3],
                delivery = aiBowl.deliveryType,
                shotType = shot,
                shotAngleDeg = _selectedShotAngle.value,
                timingOffset = if (swingTapped) capturedTimingOffset else 0.72f,
                pitchTargetX = aiBowl.pitchX,
                pitchTargetZ = aiBowl.pitchZ,
                bowlingAccuracy = aiBowl.accuracy,
                stadium = state.stadium,
                weather = state.weather,
                preset = aiBowl.fieldingPreset,
                squad = innings.bowlingTeam.squad,
                freeHitActive = innings.freeHitPending
            )

            animateOutcomeAndApply(outcome, trail, allowRunningDecision = !outcome.isWicket && !outcome.isBoundaryFour && !outcome.isBoundarySix && outcome.runsOffBat in 1..2)
        }
    }

    private fun runUserBowlingDelivery() {
        activeBallJob = viewModelScope.launch {
            val state = _matchState.value
            val innings = state.currentInnings
            val delivery = _selectedDelivery.value
            paceReleaseTapped = false
            capturedBowlingAccuracy = 0.75f

            _matchState.update {
                it.copy(
                    phase = MatchPhase.BOWLING_RUNUP,
                    commentary = "Tap RELEASE inside the green zone for pinpoint accuracy!"
                )
            }

            // Pace meter sweeps back and forth for 1.1s or until tapped
            val steps = 28
            for (i in 0..steps) {
                while (_isPaused.value) delay(80L)
                val t = i / steps.toFloat()
                _animState.update {
                    it.copy(
                        bowlerRunup = t,
                        paceCursor = t,
                        stumpsBroken = false,
                        batterSwing = 0f,
                        trailPoints = emptyList()
                    )
                }
                if (paceReleaseTapped && i > 10) break
                delay(36L)
            }

            _matchState.update { it.copy(phase = MatchPhase.BALL_IN_FLIGHT_TO_BAT) }
            val trail = mutableListOf<Vec3>()
            val flightSteps = 22
            val pX = _pitchTargetX.value
            val pZ = _pitchTargetZ.value

            for (i in 0..flightSteps) {
                while (_isPaused.value) delay(80L)
                val t = i / flightSteps.toFloat()
                val z = 20.12f * (1f - t)
                val bounceT = pZ.coerceIn(0.15f, 0.85f)
                val y = if (t < bounceT) 2.0f * (1f - t / bounceT) + 0.08f
                else 0.08f + sin(((t - bounceT) / (1f - bounceT)).coerceIn(0f, 1f) * PI).toFloat() * 0.85f
                val x = pX * 0.9f + (delivery.swingBase * state.weather.swingMultiplier * t * 0.35f)
                val pos = Vec3(x, y, z)
                trail += pos
                _animState.update {
                    it.copy(ballPos = pos, ballVisible = true, trailPoints = trail.toList())
                }
                delay(24L)
            }

            val aiBat = CricketAiEngine.chooseBattingPlan(state)
            val outcome = CricketPhysics.simulateDelivery(
                batter = innings.striker.player,
                bowler = innings.currentBowler.player,
                keeper = innings.bowlingTeam.squad[3],
                delivery = delivery,
                shotType = aiBat.shotType,
                shotAngleDeg = aiBat.angleDeg,
                timingOffset = aiBat.timingOffset,
                pitchTargetX = pX,
                pitchTargetZ = pZ,
                bowlingAccuracy = capturedBowlingAccuracy,
                stadium = state.stadium,
                weather = state.weather,
                preset = state.fieldingPreset,
                squad = innings.bowlingTeam.squad,
                freeHitActive = innings.freeHitPending
            )

            animateOutcomeAndApply(outcome, trail, allowRunningDecision = false)
        }
    }

    private suspend fun animateOutcomeAndApply(
        outcome: BallOutcome,
        trail: MutableList<Vec3>,
        allowRunningDecision: Boolean
    ) {
        if (outcome.isWicket && outcome.dismissalType == DismissalType.BOWLED) {
            _animState.update { it.copy(stumpsBroken = true, batterSwing = 1f) }
            delay(450L)
            finishBallWithOutcome(outcome)
            return
        }

        if (outcome.distanceMeters > 4f) {
            _matchState.update { it.copy(phase = MatchPhase.BALL_IN_OUTFIELD, commentary = outcome.commentary) }
            val rad = (outcome.shotAngleDeg * PI / 180.0).toFloat()
            val outSteps = 20
            for (i in 1..outSteps) {
                while (_isPaused.value) delay(80L)
                val t = i / outSteps.toFloat()
                val dist = outcome.distanceMeters * t
                val pos = Vec3(
                    x = sin(rad) * dist,
                    y = (sin(t * PI).toFloat() * outcome.maxHeightMeters).coerceAtLeast(0.08f),
                    z = cos(rad) * dist
                )
                trail += pos
                _animState.update {
                    it.copy(ballPos = pos, ballVisible = true, batterSwing = 1f, trailPoints = trail.toList())
                }
                delay(26L)
            }
        }

        if (allowRunningDecision) {
            _animState.update {
                it.copy(pendingRunOutcome = outcome, pendingRunningRuns = outcome.runsOffBat)
            }
            _matchState.update {
                it.copy(
                    phase = MatchPhase.RUNNING_DECISION,
                    commentary = "Hit into the outfield! Tap +1 RUN to push for more or HOLD CREASE."
                )
            }
        } else {
            finishBallWithOutcome(outcome)
        }
    }

    private fun finishBallWithOutcome(outcome: BallOutcome) {
        val nextState = CricketRulesEngine.applyBallOutcome(_matchState.value, outcome)
        _matchState.value = nextState
        _animState.update {
            it.copy(
                ballVisible = false,
                bowlerRunup = 0f,
                batterSwing = 0f,
                pendingRunOutcome = null,
                pendingRunningRuns = 0
            )
        }
        onOnlineBallSynced?.invoke(nextState, outcome)

        if (nextState.phase == MatchPhase.MATCH_FINISHED && !completionReported) {
            completionReported = true
            onMatchFinishedCallback(nextState)
        }
    }

    fun startSecondInnings() {
        _matchState.update { CricketRulesEngine.startNextInnings(it) }
    }

    fun startSuperOver() {
        completionReported = false
        _matchState.update { CricketRulesEngine.startSuperOver(it) }
    }

    fun declareTestInnings() {
        val next = CricketRulesEngine.declareInnings(_matchState.value)
        _matchState.value = next
        if (next.phase == MatchPhase.MATCH_FINISHED && !completionReported) {
            completionReported = true
            onMatchFinishedCallback(next)
        }
    }
}
