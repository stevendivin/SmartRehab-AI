package com.steven.smartrehab.presentation.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steven.smartrehab.data.local.AppDatabase
import com.steven.smartrehab.data.local.SessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val sessions by database.sessionDao().getAllSessions().collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Historique", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        ProgressChart(sessions = sessions)
        Spacer(modifier = Modifier.height(24.dp))

        if (sessions.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Aucune séance enregistrée pour l'instant")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sessions) { session ->
                    SessionItem(session)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Retour")
        }
    }
}

@Composable
fun SessionItem(session: SessionEntity) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.FRANCE) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(session.exerciseName, fontWeight = FontWeight.Bold)
            Text(dateFormat.format(Date(session.date)))
            Text("${session.totalReps} répétitions (${session.correctReps} correctes, ${session.incorrectReps} à corriger)")
            Text("Durée : ${session.durationSeconds} s")
        }
    }
}