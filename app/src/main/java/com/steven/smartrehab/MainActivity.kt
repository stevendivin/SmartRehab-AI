package com.steven.smartrehab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.steven.smartrehab.camera.CameraScreen
import com.steven.smartrehab.presentation.exercise.ExerciseScreen
import com.steven.smartrehab.presentation.exercise.GuideScreen
import com.steven.smartrehab.presentation.exercise.exerciseFullName
import com.steven.smartrehab.presentation.history.HistoryScreen
import com.steven.smartrehab.presentation.home.HomeScreen
import com.steven.smartrehab.presentation.profile.ProfileScreen
import com.steven.smartrehab.presentation.session.SessionSummaryScreen
import com.steven.smartrehab.ui.theme.SmartRehabAITheme

enum class AppScreen {
    HOME, EXERCISES, GUIDE, CAMERA, SESSION_SUMMARY, HISTORY, PROFILE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartRehabAITheme {
                var selectedExerciseId by remember { mutableStateOf("knee_flexion") }
                var selectedSide by remember { mutableStateOf("left") }
                var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
                var lastDuration by remember { mutableStateOf(0) }
                var lastTotalReps by remember { mutableStateOf(0) }
                var lastCorrectReps by remember { mutableStateOf(0) }
                var lastIncorrectReps by remember { mutableStateOf(0) }
                var lastAverageAngle by remember { mutableStateOf(0.0) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // La camera occupe tout l'ecran, les autres ecrans respectent les barres systeme
                    Box(
                        modifier = if (currentScreen == AppScreen.CAMERA) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier.fillMaxSize().systemBarsPadding()
                        }
                    ) {
                        when (currentScreen) {
                            AppScreen.HOME -> HomeScreen(
                                onStartSession = { currentScreen = AppScreen.EXERCISES },
                                onExercisesClick = { currentScreen = AppScreen.EXERCISES },
                                onHistoryClick = { currentScreen = AppScreen.HISTORY },
                                onProfileClick = { currentScreen = AppScreen.PROFILE }
                            )
                            AppScreen.EXERCISES -> ExerciseScreen(
                                onExerciseSelected = { id, side ->
                                    selectedExerciseId = id
                                    selectedSide = side
                                    currentScreen = AppScreen.GUIDE
                                },
                                onBack = { currentScreen = AppScreen.HOME }
                            )
                            AppScreen.GUIDE -> GuideScreen(
                                exerciseId = selectedExerciseId,
                                side = selectedSide,
                                onStart = { currentScreen = AppScreen.CAMERA },
                                onBack = { currentScreen = AppScreen.EXERCISES }
                            )
                            AppScreen.CAMERA -> CameraScreen(
                                exerciseId = selectedExerciseId,
                                side = selectedSide,
                                onStop = { duration, total, correct, incorrect, averageAngle ->
                                    lastDuration = duration
                                    lastTotalReps = total
                                    lastCorrectReps = correct
                                    lastIncorrectReps = incorrect
                                    lastAverageAngle = averageAngle
                                    currentScreen = AppScreen.SESSION_SUMMARY
                                }
                            )
                            AppScreen.SESSION_SUMMARY -> SessionSummaryScreen(
                                exerciseName = exerciseFullName(selectedExerciseId, selectedSide),
                                durationSeconds = lastDuration,
                                totalReps = lastTotalReps,
                                correctReps = lastCorrectReps,
                                incorrectReps = lastIncorrectReps,
                                averageAngle = lastAverageAngle,
                                onDone = { currentScreen = AppScreen.HOME }
                            )
                            AppScreen.HISTORY -> HistoryScreen(
                                onBack = { currentScreen = AppScreen.HOME }
                            )
                            AppScreen.PROFILE -> ProfileScreen(
                                onBack = { currentScreen = AppScreen.HOME }
                            )
                        }
                    }
                }
            }
        }
    }
}