package com.example.mabdtheque.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsScreen(
    onSyncNow: (onDone: (String) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var url by remember { mutableStateOf(FirebaseSync.getUrl(context)) }
    var code by remember { mutableStateOf(FirebaseSync.getCode(context)) }
    var busy by remember { mutableStateOf(false) }
    var status by remember {
        mutableStateOf(
            if (FirebaseSync.isConfigured(context))
                "Synchro configurée. Utilisez « Synchroniser » pour échanger les albums."
            else "Renseignez l'URL de votre base Firebase et un code d'accès familial."
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Synchronisation") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) { Text("✕") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL de la base Firebase") },
                placeholder = { Text("https://mon-projet-default-rtdb.europe-west1.firebasedatabase.app") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Code d'accès familial") },
                placeholder = { Text("ex. famille-boulet") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    FirebaseSync.saveConfig(context, url, code)
                    status = "Configuration enregistrée."
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enregistrer la configuration") }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    busy = true
                    onSyncNow { message ->
                        busy = false
                        status = message
                    }
                },
                enabled = !busy && FirebaseSync.isConfigured(context),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "Synchronisation…" else "Synchroniser maintenant") }

            if (busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text(status, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(16.dp))
            Text(
                "Comment créer la base (une seule fois) :\n" +
                "1. console.firebase.google.com → créer un projet\n" +
                "2. Menu « Realtime Database » → créer une base\n" +
                "3. Copier son URL ici (affichée en haut de la base)\n" +
                "4. Choisir un code d'accès identique sur tous les téléphones",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
