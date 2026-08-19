package com.steven.smartrehab.presentation.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.steven.smartrehab.data.local.SessionEntity

@Composable
fun ProgressChart(sessions: List<SessionEntity>) {
    // On affiche les 10 dernières séances, dans l'ordre chronologique (plus ancien à gauche)
    val recentSessions = sessions.take(10).reversed()

    if (recentSessions.size < 2) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            Text(
                "Fais au moins 2 séances pour voir ta progression",
                modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                style = MaterialTheme.typography.bodySmall
            )
        }
        return
    }

    val scores = recentSessions.map { session ->
        if (session.totalReps > 0) {
            (session.correctReps.toFloat() / session.totalReps.toFloat()) * 100f
        } else 0f
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Progression (score %)", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val width = size.width
            val height = size.height
            val stepX = width / (scores.size - 1)

            val points = scores.mapIndexed { index, score ->
                val x = index * stepX
                val y = height - (score / 100f * height)
                Offset(x, y)
            }

            // Ligne reliant les points
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = Color(0xFF4CAF50),
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }

            // Points sur chaque valeur
            points.forEach { point ->
                drawCircle(
                    color = Color(0xFF4CAF50),
                    radius = 8f,
                    center = point
                )
            }
        }
    }
}