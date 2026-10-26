package com.steven.smartrehab.pose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class PoseDetectorHelper(
    context: Context,
    private val onResult: (result: PoseLandmarkerResult, imageWidth: Int, imageHeight: Int) -> Unit
) {
    private var poseLandmarker: PoseLandmarker? = null

    @Volatile private var lastWidth = 0
    @Volatile private var lastHeight = 0

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker_lite.task")
            .setDelegate(Delegate.CPU)
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setMinPoseDetectionConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setMinPosePresenceConfidence(0.5f)
            .setResultListener { result, _ -> onResult(result, lastWidth, lastHeight) }
            .setErrorListener { error -> Log.e("PoseDetectorHelper", "Erreur: ${error.message}") }
            .build()

        poseLandmarker = PoseLandmarker.createFromOptions(context, options)
    }

    fun detectAsync(imageProxy: ImageProxy) {
        val rotation = imageProxy.imageInfo.rotationDegrees
        val source = imageProxy.toBitmap()
        imageProxy.close()

        // On remet l'image à l'endroit : y = vrai haut/bas de l'écran
        val bitmap: Bitmap = if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        lastWidth = bitmap.width
        lastHeight = bitmap.height

        val mpImage = BitmapImageBuilder(bitmap).build()
        poseLandmarker?.detectAsync(mpImage, SystemClock.uptimeMillis())
    }

    fun close() {
        poseLandmarker?.close()
    }
}