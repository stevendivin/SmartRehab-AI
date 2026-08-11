package com.steven.smartrehab.analysis

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.acos
import kotlin.math.sqrt

object AngleCalculator {

    /**
     * Calcule l'angle (en degrés) au point B, formé par les segments A-B et C-B.
     * Exemple : A = hanche, B = genou, C = cheville → angle du genou.
     */
    fun calculateAngle(
        a: NormalizedLandmark,
        b: NormalizedLandmark,
        c: NormalizedLandmark
    ): Double {
        val abX = a.x() - b.x()
        val abY = a.y() - b.y()
        val cbX = c.x() - b.x()
        val cbY = c.y() - b.y()

        val dotProduct = (abX * cbX) + (abY * cbY)
        val magAB = sqrt((abX * abX + abY * abY).toDouble())
        val magCB = sqrt((cbX * cbX + cbY * cbY).toDouble())

        if (magAB == 0.0 || magCB == 0.0) return 0.0

        val cosAngle = (dotProduct / (magAB * magCB)).coerceIn(-1.0, 1.0)
        val angleRad = acos(cosAngle)

        return Math.toDegrees(angleRad)
    }
}