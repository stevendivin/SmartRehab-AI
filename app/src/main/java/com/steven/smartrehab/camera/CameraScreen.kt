package com.steven.smartrehab.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.steven.smartrehab.analysis.AngleCalculator
import com.steven.smartrehab.analysis.RepCounter
import com.steven.smartrehab.data.local.AppDatabase
import com.steven.smartrehab.data.local.SessionEntity
import com.steven.smartrehab.pose.PoseDetectorHelper
import com.steven.smartrehab.pose.PoseOverlay
import com.steven.smartrehab.presentation.exercise.exerciseDisplayName
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    exerciseId: String = "knee_flexion",
    onStop: (durationSeconds: Int, totalReps: Int, correctReps: Int, incorrectReps: Int) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    var currentAngle by remember { mutableStateOf(0.0) }
    var repCount by remember { mutableStateOf(0) }
    var correctReps by remember { mutableStateOf(0) }
    var incorrectReps by remember { mutableStateOf(0) }
    var lastFeedback by remember { mutableStateOf("") }

    // Squelette à afficher
    var overlayLandmarks by remember { mutableStateOf<List<NormalizedLandmark>>(emptyList()) }
    var overlayWidth by remember { mutableStateOf(0) }
    var overlayHeight by remember { mutableStateOf(0) }

    // Position la plus haute de la hanche observée (pour détecter une vraie descente en squat)
    val hipTopY = remember { floatArrayOf(Float.MAX_VALUE) }

    val repCounter = remember {
        when (exerciseId) {
            "squat" -> RepCounter(extendedThreshold = 170.0, flexedThreshold = 100.0, goodAmplitudeThreshold = 90.0)
            "arm_raise" -> RepCounter(extendedThreshold = 30.0, flexedThreshold = 80.0, goodAmplitudeThreshold = 90.0)
            "leg_raise" -> RepCounter(extendedThreshold = 160.0, flexedThreshold = 120.0, goodAmplitudeThreshold = 100.0)
            else -> RepCounter()
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val sessionStartTime = remember { System.currentTimeMillis() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val exerciseName = exerciseDisplayName(exerciseId)
    val angleLabel = when (exerciseId) {
        "arm_raise" -> "Angle du bras"
        "squat" -> "Angle du genou (squat)"
        "leg_raise" -> "Angle de la hanche"
        else -> "Angle du genou"
    }
    val joints = when (exerciseId) {
        "arm_raise" -> listOf(23, 11, 13)   // hanche, épaule, coude
        "leg_raise" -> listOf(11, 23, 25)   // épaule, hanche, genou
        else -> listOf(23, 25, 27)          // hanche, genou, cheville
    }
    val badRepMessage = when (exerciseId) {
        "arm_raise" -> "⚠ Levez le bras plus haut"
        "leg_raise" -> "⚠ Levez la jambe plus haut"
        else -> "⚠ Descendez un peu plus"
    }

    val poseDetectorHelper = remember {
        PoseDetectorHelper(context) { result, imgW, imgH ->
            val landmarks = result.landmarks().firstOrNull()
            if (landmarks != null && landmarks.size >= 29) {
                overlayLandmarks = landmarks
                overlayWidth = imgW
                overlayHeight = imgH

                currentAngle = AngleCalculator.calculateAngle(
                    landmarks[joints[0]], landmarks[joints[1]], landmarks[joints[2]]
                )

                val angleToFeed = if (exerciseId == "squat") {
                    val hipY = landmarks[23].y()
                    if (hipY < hipTopY[0]) hipTopY[0] = hipY
                    val hipDropped = (hipY - hipTopY[0]) > 0.06f
                    // Sans vraie descente de la hanche, un simple lever de jambe ne compte pas
                    if (hipDropped) currentAngle else 180.0
                } else {
                    currentAngle
                }

                val repResult = repCounter.update(angleToFeed)
                repCount = repCounter.repCount
                correctReps = repCounter.correctReps
                incorrectReps = repCounter.incorrectReps
                repResult?.let {
                    lastFeedback = if (it.isCorrect) "✓ Bonne amplitude" else badRepMessage
                }
            } else {
                overlayLandmarks = emptyList()
            }
        }
    }

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    // Libération propre en quittant l'écran : caméra, détecteur, thread d'analyse
    DisposableEffect(Unit) {
        onDispose {
            try {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            poseDetectorHelper.close()
            analysisExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(analysisExecutor) { imageProxy ->
                                    poseDetectorHelper.detectAsync(imageProxy)
                                }
                            }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_FRONT_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (exc: Exception) {
                            exc.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            PoseOverlay(
                landmarks = overlayLandmarks,
                imageWidth = overlayWidth,
                imageHeight = overlayHeight,
                highlight = joints,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Text("$angleLabel : ${currentAngle.toInt()}°", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Text("Répétitions : $repCount ($correctReps ✓ / $incorrectReps ⚠)", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                if (lastFeedback.isNotEmpty()) {
                    Text(lastFeedback, color = Color.Yellow, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val durationSeconds = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt()
                    val total = repCounter.repCount
                    val correct = repCounter.correctReps
                    val incorrect = repCounter.incorrectReps

                    // Une séance sans aucune répétition n'est pas sauvegardée
                    if (total > 0) {
                        coroutineScope.launch {
                            // NonCancellable : la sauvegarde doit finir même si l'écran disparaît
                            withContext(NonCancellable) {
                                database.sessionDao().insertSession(
                                    SessionEntity(
                                        exerciseName = exerciseName,
                                        date = sessionStartTime,
                                        durationSeconds = durationSeconds,
                                        totalReps = total,
                                        correctReps = correct,
                                        incorrectReps = incorrect,
                                        averageAmplitude = 0.0
                                    )
                                )
                            }
                        }
                    }
                    onStop(durationSeconds, total, correct, incorrect)
                }) {
                    Text("STOP")
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Permission caméra requise pour continuer")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Autoriser la caméra")
                }
            }
        }
    }
}