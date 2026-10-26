package com.steven.smartrehab.presentation.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steven.smartrehab.data.local.AppDatabase
import com.steven.smartrehab.data.local.SessionEntity
import com.steven.smartrehab.presentation.exercise.exerciseDisplayName
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val historyFilters = listOf(
    "all" to "Tous",
    "knee_flexion" to "Genou",
    "arm_raise" to "Bras",
    "squat" to "Squat",
    "leg_raise" to "Jambe"
)

@Composable
fun HistoryScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val allSessions by database.sessionDao().getAllSessions().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var selectedFilter by remember { mutableStateOf("all") }
    var sessionToDelete by remember { mutableStateOf<SessionEntity?>(null) }

    val sessions = if (selectedFilter == "all") {
        allSessions
    } else {
        val name = exerciseDisplayName(selectedFilter)
        allSessions.filter { it.exerciseName.startsWith(name) }
    }

    val totalReps = sessions.sumOf { it.totalReps }
    val averageScore = if (totalReps > 0) sessions.sumOf { it.correctReps } * 100 / totalReps else null

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Historique", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            historyFilters.forEach { (id, label) ->
                FilterChip(
                    selected = selectedFilter == id,
                    onClick = { selectedFilter = id },
                    label = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ProgressChart(sessions = sessions)

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (averageScore != null) {
                "${sessions.size} séance(s), score moyen $averageScore %"
            } else {
                "Aucune donnée pour ce filtre"
            },
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (sessions.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Aucune séance enregistrée pour l'instant")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sessions, key = { it.id }) { session ->
                    SessionItem(session = session, onDelete = { sessionToDelete = session })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Retour")
        }
    }

    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Supprimer cette séance ?") },
            text = { Text("${session.exerciseName}, ${session.totalReps} répétitions. Cette action est définitive.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { database.sessionDao().deleteSessionById(session.id) }
                        sessionToDelete = null
                    }
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun SessionItem(session: SessionEntity, onDelete: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.FRANCE) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.exerciseName, fontWeight = FontWeight.Bold)
                Text(dateFormat.format(Date(session.date)), style = MaterialTheme.typography.bodySmall)
                Text("${session.totalReps} répétitions (${session.correctReps} correctes, ${session.incorrectReps} à corriger)")
                if (session.averageAmplitude > 0.0) {
                    Text("Angle moyen atteint : ${session.averageAmplitude.toInt()}°")
                }
                Text("Durée : ${session.durationSeconds} s")
            }
            TextButton(onClick = onDelete) {
                Text("Supprimer")
            }
        }
    }
}