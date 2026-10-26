package com.steven.smartrehab.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

@Composable
fun ProfileScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }

    var name by remember { mutableStateOf(prefs.userName) }
    var side by remember { mutableStateOf(prefs.defaultSide) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Mon profil", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = {
                if (it.length <= 40) {
                    name = it
                    saved = false
                }
            },
            label = { Text("Prénom ou nom du patient") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Côté suivi par défaut", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Le côté à rééduquer. Vous pourrez le changer avant chaque séance.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = side == "left",
                onClick = {
                    side = "left"
                    saved = false
                },
                label = { Text("Côté gauche") }
            )
            FilterChip(
                selected = side == "right",
                onClick = {
                    side = "right"
                    saved = false
                },
                label = { Text("Côté droit") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                prefs.userName = name
                prefs.defaultSide = side
                saved = true
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Enregistrer")
        }

        if (saved) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Profil enregistré", color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("À propos", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "SmartRehab AI est un outil de suivi personnel. Il ne constitue pas un dispositif médical " +
                            "et ne remplace pas l'avis d'un professionnel de santé. Les données restent sur ce téléphone.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Retour")
        }
    }
}