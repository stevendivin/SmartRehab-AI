package com.steven.smartrehab.presentation.exercise

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val generalTips = listOf(
    "Éclairage de face, évitez le contre-jour (fenêtre derrière vous).",
    "Portez des vêtements ajustés, les vêtements amples gênent la détection.",
    "Une seule personne dans le champ de la caméra.",
    "Le téléphone ne doit pas bouger pendant la séance."
)

@Composable
fun GuideScreen(
    exerciseId: String,
    side: String,
    onStart: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val exercise = exerciseList.firstOrNull { it.id == exerciseId }
    val steps = exercise?.instructions ?: emptyList()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Avant de commencer", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(exerciseFullName(exerciseId, side), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Vous suivez votre côté ${sideLabel(side)} : la jambe ou le bras ${sideLabel(side)} doit être visible.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Placement et mouvement", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    steps.forEachIndexed { index, step ->
                        Text("${index + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Conseils généraux", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    generalTips.forEach { tip ->
                        Text("- $tip", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Commencer")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Retour")
        }
    }
}