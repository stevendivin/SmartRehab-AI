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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steven.smartrehab.data.local.UserPreferences

data class ExerciseInfo(
    val id: String,
    val name: String,
    val hint: String,
    val instructions: List<String>,
    val isAvailable: Boolean
)

val exerciseList = listOf(
    ExerciseInfo(
        id = "knee_flexion",
        name = "Flexion du genou",
        hint = "Jambe entièrement visible, de face ou de profil",
        instructions = listOf(
            "Posez le téléphone contre un support stable, à hauteur de hanche.",
            "Placez-vous à environ 2 mètres, la jambe entière visible de la hanche à la cheville.",
            "Position de départ : jambe tendue.",
            "Pliez le genou le plus possible, puis revenez jambe tendue. Une répétition est comptée quand le mouvement est complet."
        ),
        isAvailable = true
    ),
    ExerciseInfo(
        id = "arm_raise",
        name = "Élévation du bras",
        hint = "De face, hanche, épaule et coude visibles",
        instructions = listOf(
            "Posez le téléphone contre un support stable, à hauteur de poitrine.",
            "Placez-vous à environ 2 mètres, face à la caméra, de la tête aux hanches visibles.",
            "Position de départ : bras tendu le long du corps.",
            "Levez le bras tendu sur le côté jusqu'à l'horizontale ou plus haut, puis redescendez en contrôlant le mouvement."
        ),
        isAvailable = true
    ),
    ExerciseInfo(
        id = "squat",
        name = "Squat",
        hint = "De profil, reculer pour voir tout le corps",
        instructions = listOf(
            "Posez le téléphone contre un support stable, à hauteur de hanche.",
            "Placez-vous de profil, le côté suivi vers la caméra, à environ 2,5 mètres pour voir la tête et les pieds.",
            "Position de départ : debout, bien droit, pieds écartés à la largeur des épaules. Restez immobile une seconde.",
            "Descendez jusqu'à ce que les cuisses soient proches de l'horizontale, puis remontez complètement."
        ),
        isAvailable = true
    ),
    ExerciseInfo(
        id = "leg_raise",
        name = "Élévation de jambe",
        hint = "De profil, jambe tendue",
        instructions = listOf(
            "Posez le téléphone contre un support stable, à hauteur de hanche.",
            "Placez-vous de profil, le côté suivi vers la caméra, à environ 2,5 mètres pour voir tout le corps.",
            "Position de départ : debout, corps droit. Appuyez-vous sur un mur ou une chaise si besoin.",
            "Levez la jambe tendue vers l'avant jusqu'à l'horizontale, puis redescendez en contrôlant le mouvement."
        ),
        isAvailable = true
    )
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
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    var side by remember { mutableStateOf(prefs.defaultSide) }

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