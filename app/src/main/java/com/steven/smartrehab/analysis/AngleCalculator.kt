package com.steven.smartrehab.analysis

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.acos
import kotlin.math.sqrt

object AngleCalculator {

    /**
     * Angle en degres au point B, forme par les segments B-A et B-C.
     * Les coordonnees doivent etre dans la meme unite (pixels de preference).
     */
    fun calculateAngle(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float
    ): Double {
        val abX = (x1 - x2).toDouble()
        val abY = (y1 - y2).toDouble()
        val cbX = (x3 - x2).toDouble()
        val cbY = (y3 - y2).toDouble()

        val dot = abX * cbX + abY * cbY
        val magAB = sqrt(abX * abX + abY * abY)
        val magCB = sqrt(cbX * cbX + cbY * cbY)

        if (magAB == 0.0 || magCB == 0.0) return 0.0

        val cos = (dot / (magAB * magCB)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cos))
    }

    /**
     * Version pour les points MediaPipe (normalises entre 0 et 1).
     * On les remet a l'echelle de l'image pour ne pas deformer les angles.
     */
    fun calculateAngle(
        a: NormalizedLandmark,
        b: NormalizedLandmark,
        c: NormalizedLandmark,
        imageWidth: Int,
        imageHeight: Int
    ): Double {
        val w = if (imageWidth > 0) imageWidth.toFloat() else 1f
        val h = if (imageHeight > 0) imageHeight.toFloat() else 1f
        return calculateAngle(
            a.x() * w, a.y() * h,
            b.x() * w, b.y() * h,
            c.x() * w, c.y() * h
        )
    }
}