package com.steven.smartrehab.presentation.session

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SessionSummaryScreen(
    exerciseName: String,
    durationSeconds: Int,
    totalReps: Int,
    correctReps: Int,
    incorrectReps: Int,
    onDone: () -> Unit = {}
) {
    val score = if (totalReps > 0) (correctReps * 100 / totalReps) else 0

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Séance terminée 🎉", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(exerciseName, style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(32.dp))

        SummaryRow("Durée", "${durationSeconds / 60}:${(durationSeconds % 60).toString().padStart(2, '0')}")
        SummaryRow("Répétitions", "$totalReps")
        SummaryRow("Correctes", "$correctReps")
        SummaryRow("Incorrectes", "$incorrectReps")
        SummaryRow("Score", "$score %")

        Spacer(modifier = Modifier.height(40.dp))

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Retour à l'accueil")
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}