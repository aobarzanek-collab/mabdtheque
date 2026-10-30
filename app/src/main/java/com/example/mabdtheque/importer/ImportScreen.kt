package com.example.mabdtheque.importer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.mabdtheque.data.Album

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onImport: (List<Album>, (Int, Int) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Sélectionnez votre fichier Excel (.xls)") }
    var importing by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            status = "Aucun fichier sélectionné."
            return@rememberLauncherForActivityResult
        }
        importing = true
        status = "Lecture du fichier…"
        val result = ExcelImporter.parse(context, uri)
        if (result.albums.isEmpty()) {
            importing = false
            status = "Aucun album trouvé. ${result.errors.joinToString(" ")}"
            return@rememberLauncherForActivityResult
        }
        status = "${result.albums.size} albums trouvés. Import en cours…"
        onImport(result.albums) { ok, dup ->
            importing = false
            status = "Import terminé : $ok albums importés" +
                if (dup > 0) ", $dup doublons ignorés" else ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importer depuis Excel") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Text("✕")
                    }
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
            Text(
                "Choisissez votre fichier Excel (.xls). " +
                "Les colonnes reconnues automatiquement : " +
                "titre/série, tome, auteur, dessinateur, coloriste, " +
                "année, ISBN, dédicace, notes.",
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = {
                    filePicker.launch(
                        arrayOf(
                            "application/vnd.ms-excel",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "application/octet-stream"
                        )
                    )
                },
                enabled = !importing,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Choisir le fichier Excel") }

            if (importing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text(status, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(16.dp))
        }
    }
}
