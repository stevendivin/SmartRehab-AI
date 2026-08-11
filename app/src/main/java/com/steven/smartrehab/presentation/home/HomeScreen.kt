package com.steven.smartrehab.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steven.smartrehab.ui.theme.SmartRehabAITheme

@Composable
fun HomeScreen(
    userName: String = "Steven",
    progress: Int = 85,
    onStartSession: () -> Unit = {},
    onExercisesClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "SmartRehab AI", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Bonjour $userName 👋", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "Votre progression", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "$progress %", fontSize = 40.sp, fontWeight = FontWeight.Bold)
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
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    SmartRehabAITheme {
        HomeScreen()
    }
}