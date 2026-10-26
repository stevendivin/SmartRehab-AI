package com.steven.smartrehab.presentation.exercise

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ExerciseInfo(
    val id: String,
    val name: String,
    val hint: String,
    val isAvailable: Boolean
)

val exerciseList = listOf(
    ExerciseInfo("knee_flexion", "Flexion du genou", "Jambe entièrement visible, de face ou de profil", true),
    ExerciseInfo("arm_raise", "Élévation du bras", "De face, hanche, épaule et coude visibles", true),
    ExerciseInfo("squat", "Squat", "De profil, reculer pour voir tout le corps", true),
    ExerciseInfo("leg_raise", "Élévation de jambe", "De profil, jambe tendue", true)
)

fun exerciseDisplayName(id: String): String =
    exerciseList.firstOrNull { it.id == id }?.name ?: "Flexion du genou"

fun sideLabel(side: String): String = if (side == "right") "droite" else "gauche"

fun exerciseFullName(id: String, side: String): String =
    "${exerciseDisplayName(id)} (${sideLabel(side)})"

@Composable
fun ExerciseScreen(
    onExerciseSelected: (exerciseId: String, side: String) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    var side by remember { mutableStateOf("left") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Exercices", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Text("Côté à suivre", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = side == "left",
                onClick = { side = "left" },
                label = { Text("Côté gauche") }
            )
            FilterChip(
                selected = side == "right",
                onClick = { side = "right" },
                label = { Text("Côté droit") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(exerciseList) { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    onClick = { if (exercise.isAvailable) onExerciseSelected(exercise.id, side) }
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
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(exercise.name, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (exercise.isAvailable) exercise.hint else "Bientôt disponible",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}