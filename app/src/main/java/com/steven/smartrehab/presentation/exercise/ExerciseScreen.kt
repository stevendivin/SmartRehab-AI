package com.steven.smartrehab.presentation.exercise

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ExerciseInfo(
    val id: String,
    val name: String,
    val emoji: String,
    val isAvailable: Boolean
)

val exerciseList = listOf(
    ExerciseInfo("knee_flexion", "Flexion du genou", "🦵", isAvailable = true),
    ExerciseInfo("leg_raise", "Élévation de jambe", "🦵", isAvailable = false),
    ExerciseInfo("squat", "Squat", "🏃", isAvailable = true),
    ExerciseInfo("arm_raise", "Élévation du bras", "💪", isAvailable = true)
)

@Composable
fun ExerciseScreen(
    onExerciseSelected: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Exercices", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(exerciseList) { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    onClick = { if (exercise.isAvailable) onExerciseSelected(exercise.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Retour")
        }
    }
}

@Composable
fun ExerciseCard(exercise: ExerciseInfo, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(exercise.emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, fontWeight = FontWeight.Bold)
                if (!exercise.isAvailable) {
                    Text(
                        "Bientôt disponible",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}