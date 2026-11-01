package com.example.mabdtheque.importer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.example.mabdtheque.data.Category

data class SheetChoice(
    val sheetName: String,
    val albumCount: Int,
    val category: Category
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onImport: (List<Album>, (Int, Int) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("1. Sélectionnez votre fichier Excel (.xls / .xlsx)") }
    var importing by remember { mutableStateOf(false) }
    var sheetChoices by remember { mutableStateOf<List<SheetChoice>>(emptyList()) }
    var parsedAlbumsBySheet by remember { mutableStateOf<List<Pair<SheetChoice, List<Album>>>>(emptyList()) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            status = "Aucun fichier sélectionné."
            return@rememberLauncherForActivityResult
        }
        importing = true
        status = "Lecture du fichier…"
        val result = ExcelImporter.parseSheets(context, uri)
        if (result.sheets.isEmpty()) {
            importing = false
            status = "Aucune feuille exploitable. ${result.errors.joinToString(" ")}"
            return@rememberLauncherForActivityResult
        }
        // Devine une catégorie par feuille selon son nom
        val choices = result.sheets.map { (name, albums) ->
            val lower = name.lowercase()
            val category = when {
                lower.contains("manga") -> Category.MANGA
                lower.contains("jeun") || lower.contains("enfant") -> Category.JEUNESSE
                else -> Category.BD
            }
            SheetChoice(name, albums.size, category)
        }
        parsedAlbumsBySheet = result.sheets.mapIndexed { i, (_, albums) -> choices[i] to albums }
        sheetChoices = choices
        importing = false
        status = "2. Choisissez la catégorie de chaque feuille puis lancez l'import."
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
            if (sheetChoices.isEmpty()) {
                Text(
                    "Choisissez votre fichier Excel. Les colonnes reconnues : " +
                    "SERIE, TITRE, TOME, EDITEUR, SCENARISTE, DESSINATEUR, " +
                    "COLORISTE, Compte (0 = souhait).",
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
            } else {
                Text(
                    "Feuilles détectées — ajustez la catégorie de chacune :",
                    style = MaterialTheme.typography.titleSmall
                )
                sheetChoices.forEachIndexed { index, choice ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "${choice.sheetName} (${choice.albumCount} albums)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Category.entries.forEach { cat ->
                                    FilterChip(
                                        selected = sheetChoices[index].category == cat,
                                        onClick = {
                                            sheetChoices = sheetChoices.toMutableList().also {
                                                it[index] = choice.copy(category = cat)
                                            }
                                        },
                                        label = { Text(cat.label) }
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        importing = true
                        status = "Import en cours…"
                        val all = parsedAlbumsBySheet.flatMap { (choice, albums) ->
                            albums.map { it.copy(category = choice.category.name) }
                        }
                        onImport(all) { ok, dup ->
                            importing = false
                            status = "Import terminé : $ok albums importés" +
                                if (dup > 0) ", $dup doublons ignorés" else ""
                            sheetChoices = emptyList()
                            parsedAlbumsBySheet = emptyList()
                        }
                    },
                    enabled = !importing,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Importer ${sheetChoices.sumOf { it.albumCount }} albums") }
            }

            if (importing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text(status, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(16.dp))
        }
    }
}
