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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.steven.smartrehab.analysis.AngleCalculator
import com.steven.smartrehab.analysis.RepCounter
import com.steven.smartrehab.pose.PoseDetectorHelper
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    onStop: () -> Unit = {}
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
    var kneeAngle by remember { mutableStateOf(0.0) }
    val repCounter = remember { RepCounter() }
    var repCount by remember { mutableStateOf(0) }
    var correctReps by remember { mutableStateOf(0) }
    var incorrectReps by remember { mutableStateOf(0) }
    var lastFeedback by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Le helper de pose est créé une seule fois
    val poseDetectorHelper = remember {
        PoseDetectorHelper(context) { result ->
            val landmarks = result.landmarks().firstOrNull()
            if (landmarks != null && landmarks.size >= 29) {
                val hip = landmarks[23]
                val knee = landmarks[25]
                val ankle = landmarks[27]
                kneeAngle = AngleCalculator.calculateAngle(hip, knee, ankle)
                val repResult = repCounter.update(kneeAngle)
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
                Text("Angle du genou : ${kneeAngle.toInt()}°", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Text("Répétitions : $repCount ($correctReps ✓ / $incorrectReps ⚠)", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                if (lastFeedback.isNotEmpty()) {
                    Text(lastFeedback, color = Color.Yellow, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onStop) { Text("STOP") }
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