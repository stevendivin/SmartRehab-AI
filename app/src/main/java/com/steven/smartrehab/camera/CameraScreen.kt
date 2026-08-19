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
import com.steven.smartrehab.analysis.AngleCalculator
import com.steven.smartrehab.analysis.RepCounter
import com.steven.smartrehab.pose.PoseDetectorHelper
import java.util.concurrent.Executors
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.steven.smartrehab.data.local.AppDatabase
import com.steven.smartrehab.data.local.SessionEntity
import kotlinx.coroutines.launch

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

    // Etat mis à jour à chaque frame détectée
    var currentAngle by remember { mutableStateOf(0.0) }
    var hipBaselineY by remember { mutableStateOf<Float?>(null) }
    val repCounter = remember {
        when (exerciseId) {
            "squat" -> RepCounter(extendedThreshold = 170.0, flexedThreshold = 100.0, goodAmplitudeThreshold = 90.0)
            "arm_raise" -> RepCounter(extendedThreshold = 30.0, flexedThreshold = 80.0, goodAmplitudeThreshold = 90.0)
            else -> RepCounter() // knee_flexion garde les valeurs par défaut (160/110/100)
        }
    }
    var repCount by remember { mutableStateOf(0) }
    var correctReps by remember { mutableStateOf(0) }
    var incorrectReps by remember { mutableStateOf(0) }
    var lastFeedback by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val sessionStartTime = remember { System.currentTimeMillis() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val exerciseName = when (exerciseId) {
        "arm_raise" -> "Élévation du bras"
        "squat" -> "Squat"
        else -> "Flexion du genou"
    }
    val angleLabel = when (exerciseId) {
        "arm_raise" -> "Angle du bras"
        "squat" -> "Angle du genou (squat)"
        else -> "Angle du genou"
    }

    // Le helper de pose est créé une seule fois
    val poseDetectorHelper = remember {
        PoseDetectorHelper(context) { result ->
            val landmarks = result.landmarks().firstOrNull()
            if (landmarks != null && landmarks.size >= 29) {
                val (a, b, c) = when (exerciseId) {
                    "arm_raise" -> Triple(landmarks[23], landmarks[11], landmarks[13]) // hanche, épaule, coude gauche
                    else -> Triple(landmarks[23], landmarks[25], landmarks[27]) // hanche, genou, cheville gauche (knee_flexion et squat)
                }
                currentAngle = AngleCalculator.calculateAngle(a, b, c)

                val angleToFeed = if (exerciseId == "squat") {
                    val hipY = landmarks[23].y()
                    if (hipBaselineY == null) hipBaselineY = hipY
                    val hipDropped = (hipY - (hipBaselineY ?: hipY)) > 0.08f
                    // Si la hanche n'a pas vraiment descendu, on force l'angle à rester "étendu"
                    // pour empêcher un simple lever de jambe de compter comme un squat
                    if (hipDropped) currentAngle else 180.0
                } else {
                    currentAngle
                }

                val repResult = repCounter.update(angleToFeed)
                repCount = repCounter.repCount
                correctReps = repCounter.correctReps
                incorrectReps = repCounter.incorrectReps
                repResult?.let {
                    lastFeedback = if (it.isCorrect) "✓ Bonne amplitude" else "⚠ Descendez un peu plus"
                }
            }
        }
    }

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

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

                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
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
                    coroutineScope.launch {
                        database.sessionDao().insertSession(
                            SessionEntity(
                                exerciseName = exerciseName,
                                date = sessionStartTime,
                                durationSeconds = durationSeconds,
                                totalReps = repCounter.repCount,
                                correctReps = repCounter.correctReps,
                                incorrectReps = repCounter.incorrectReps,
                                averageAmplitude = 0.0
                            )
                        )
                    }
                    onStop(durationSeconds, repCounter.repCount, repCounter.correctReps, repCounter.incorrectReps)
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