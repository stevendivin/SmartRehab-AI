package com.steven.smartrehab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.steven.smartrehab.camera.CameraScreen
import com.steven.smartrehab.presentation.exercise.ExerciseScreen
import com.steven.smartrehab.presentation.exercise.exerciseFullName
import com.steven.smartrehab.presentation.history.HistoryScreen
import com.steven.smartrehab.presentation.home.HomeScreen
import com.steven.smartrehab.presentation.session.SessionSummaryScreen
import com.steven.smartrehab.ui.theme.SmartRehabAITheme

enum class AppScreen {
    HOME, EXERCISES, CAMERA, SESSION_SUMMARY, HISTORY
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

                when (currentScreen) {
                    AppScreen.HOME -> HomeScreen(
                        onStartSession = { currentScreen = AppScreen.EXERCISES },
                        onExercisesClick = { currentScreen = AppScreen.EXERCISES },
                        onHistoryClick = { currentScreen = AppScreen.HISTORY }
                    )
                    AppScreen.EXERCISES -> ExerciseScreen(
                        onExerciseSelected = { id, side ->
                            selectedExerciseId = id
                            selectedSide = side
                            currentScreen = AppScreen.CAMERA
                        },
                        onBack = { currentScreen = AppScreen.HOME }
                    )
                    AppScreen.CAMERA -> CameraScreen(
                        exerciseId = selectedExerciseId,
                        side = selectedSide,
                        onStop = { duration, total, correct, incorrect ->
                            lastDuration = duration
                            lastTotalReps = total
                            lastCorrectReps = correct
                            lastIncorrectReps = incorrect
                            currentScreen = AppScreen.SESSION_SUMMARY
                        }
                    )
                    AppScreen.SESSION_SUMMARY -> SessionSummaryScreen(
                        exerciseName = exerciseFullName(selectedExerciseId, selectedSide),
                        durationSeconds = lastDuration,
                        totalReps = lastTotalReps,
                        correctReps = lastCorrectReps,
                        incorrectReps = lastIncorrectReps,
                        onDone = { currentScreen = AppScreen.HOME }
                    )
                    AppScreen.HISTORY -> HistoryScreen(
                        onBack = { currentScreen = AppScreen.HOME }
                    )
                }
            }
        }
    }
}