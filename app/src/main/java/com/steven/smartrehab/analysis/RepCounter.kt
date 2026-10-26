package com.steven.smartrehab.analysis

enum class MovementState {
    EXTENDED,
    DESCENDING,
    FLEXED,
    ASCENDING
}

data class RepResult(
    val repCount: Int,
    val isCorrect: Boolean,
    val minAngleReached: Double
)

class RepCounter(
    private val extendedThreshold: Double = 160.0,
    private val flexedThreshold: Double = 110.0,
    private val goodAmplitudeThreshold: Double = 100.0
) {
    private var state = MovementState.EXTENDED
    private var minAngleInCurrentRep = Double.MAX_VALUE
    private var sumReachedAngles = 0.0

    var repCount = 0
        private set
    var correctReps = 0
        private set
    var incorrectReps = 0
        private set

    /** Moyenne, sur les repetitions terminees, de l'angle extreme atteint (0 si aucune). */
    val averageReachedAngle: Double
        get() = if (repCount > 0) sumReachedAngles / repCount else 0.0

    /**
     * A appeler a chaque nouvel angle mesure.
     * Retourne un RepResult si une repetition vient d'etre validee, sinon null.
     */
    fun update(angle: Double): RepResult? {
        var result: RepResult? = null

        when (state) {
            MovementState.EXTENDED -> {
                if (angle < extendedThreshold) {
                    state = MovementState.DESCENDING
                    minAngleInCurrentRep = angle
                }
            }
            MovementState.DESCENDING -> {
                minAngleInCurrentRep = minOf(minAngleInCurrentRep, angle)
                if (angle < flexedThreshold) {
                    state = MovementState.FLEXED
                } else if (angle >= extendedThreshold) {
                    state = MovementState.EXTENDED
                    minAngleInCurrentRep = Double.MAX_VALUE
                }
            }
            MovementState.FLEXED -> {
                minAngleInCurrentRep = minOf(minAngleInCurrentRep, angle)
                if (angle > flexedThreshold) {
                    state = MovementState.ASCENDING
                }
            }
            MovementState.ASCENDING -> {
                if (angle >= extendedThreshold) {
                    state = MovementState.EXTENDED
                    repCount++

                    val isCorrect = minAngleInCurrentRep <= goodAmplitudeThreshold
                    if (isCorrect) correctReps++ else incorrectReps++
                    sumReachedAngles += minAngleInCurrentRep

                    result = RepResult(repCount, isCorrect, minAngleInCurrentRep)
                    minAngleInCurrentRep = Double.MAX_VALUE
                } else if (angle < flexedThreshold) {
                    minAngleInCurrentRep = minOf(minAngleInCurrentRep, angle)
                    state = MovementState.FLEXED
                }
            }
        }

        return result
    }

    fun reset() {
        state = MovementState.EXTENDED
        repCount = 0
        correctReps = 0
        incorrectReps = 0
        sumReachedAngles = 0.0
        minAngleInCurrentRep = Double.MAX_VALUE
    }
}