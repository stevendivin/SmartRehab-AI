package com.steven.smartrehab.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steven.smartrehab.data.local.AppDatabase
import com.steven.smartrehab.data.local.UserPreferences
import com.steven.smartrehab.ui.theme.SmartRehabAITheme

@Composable
fun HomeScreen(
    onStartSession: () -> Unit = {},
    onExercisesClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val userName = remember { UserPreferences(context).userName }
    val sessions by database.sessionDao().getAllSessions().collectAsState(initial = emptyList())

    // Taux de répétitions correctes sur les 10 dernières séances (null si aucune donnée)
    val recent = sessions.take(10)
    val totalReps = recent.sumOf { it.totalReps }
    val progress: Int? = if (totalReps > 0) {
        recent.sumOf { it.correctReps } * 100 / totalReps
    } else null

    HomeContent(
        userName = userName,
        progress = progress,
        sessionCount = sessions.size,
        onStartSession = onStartSession,
        onExercisesClick = onExercisesClick,
        onHistoryClick = onHistoryClick,
        onProfileClick = onProfileClick
    )
}

@Composable
fun HomeContent(
    userName: String,
    progress: Int?,
    sessionCount: Int,
    onStartSession: () -> Unit,
    onExercisesClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "SmartRehab AI", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (userName.isBlank()) "Bonjour" else "Bonjour $userName",
            fontSize = 18.sp
        )
        if (userName.isBlank()) {
            Text(
                text = "Renseignez votre nom dans Mon profil",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "Votre progression", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (progress != null) "$progress %" else "--",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (sessionCount == 0) "Faites votre première séance"
            else "$sessionCount séance(s) enregistrée(s)",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(40.dp))
        Button(onClick = onStartSession, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Commencer une séance")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onExercisesClick, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Mes exercices")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onHistoryClick, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Historique")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onProfileClick, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Mon profil")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    SmartRehabAITheme {
        HomeContent(
            userName = "Steven",
            progress = 85,
            sessionCount = 3,
            onStartSession = {},
            onExercisesClick = {},
            onHistoryClick = {},
            onProfileClick = {}
        )
    }
}