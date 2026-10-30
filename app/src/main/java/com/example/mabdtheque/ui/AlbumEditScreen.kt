package com.example.mabdtheque.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.mabdtheque.data.Album
import com.example.mabdtheque.lookup.LookupResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumEditScreen(
    initial: Album?,
    onSave: (Album) -> Unit,
    onScanIsbn: () -> Unit,
    onTakePhoto: () -> Unit,
    onLookupIsbn: (String, (LookupResult?) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(initial?.seriesTitle ?: "") }
    var albumTitle by remember { mutableStateOf(initial?.albumTitle ?: "") }
    var volume by remember {
        mutableStateOf(
            com.example.mabdtheque.data.VolumeParser.format(
                initial?.volumeNumber, initial?.volumeEnd, initial?.volumeSuffix ?: ""
            ) ?: ""
        )
    }
    var author by remember { mutableStateOf(initial?.author ?: "") }
    var artist by remember { mutableStateOf(initial?.artist ?: "") }
    var colorist by remember { mutableStateOf(initial?.colorist ?: "") }
    var publisher by remember { mutableStateOf(initial?.publisher ?: "") }
    var year by remember {
        mutableStateOf(initial?.publicationYear?.toString() ?: "")
    }
    var isbn by remember { mutableStateOf(initial?.isbn ?: "") }
    var signed by remember { mutableStateOf(initial?.isSigned ?: false) }
    var owned by remember { mutableStateOf(initial?.owned ?: true) }
    var lookupBusy by remember { mutableStateOf(false) }
    var lookupError by remember { mutableStateOf(false) }
    var pendingResult by remember { mutableStateOf<LookupResult?>(null) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var coverPath by remember { mutableStateOf(initial?.coverPhotoPath) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial == null) "Nouvel album" else "Modifier l'album") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("✕") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = isbn,
                    onValueChange = { isbn = it },
                    label = { Text("ISBN (code-barres)") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onScanIsbn) { Text("Scanner") }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        if (isbn.isNotBlank()) {
                            lookupBusy = true
                            lookupError = false
                            onLookupIsbn(isbn.trim()) { result ->
                                lookupBusy = false
                                if (result == null) lookupError = true
                                else pendingResult = result
                            }
                        }
                    },
                    enabled = isbn.isNotBlank() && !lookupBusy
                ) { Text(if (lookupBusy) "Recherche…" else "Rechercher les infos") }
                if (lookupError) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Introuvable : complétez à la main",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            pendingResult?.let { result ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Infos trouvées — vérifiez avant d'appliquer :",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (result.title.isNotBlank()) {
                        Text("Titre : " + result.title,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (result.authors.isNotEmpty()) {
                        Text("Auteur : " + result.authors.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    result.year?.let { y ->
                        Text("Année : $y",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    result.publisher?.let { p ->
                        Text("Éditeur : $p",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (result.title.isNotBlank() && title.isBlank()) {
                                title = result.title
                            }
                            if (result.authors.isNotEmpty() && author.isBlank()) {
                                author = result.authors.first()
                            }
                            result.year?.let { y ->
                                if (year.isBlank()) year = y.toString()
                            }
                            pendingResult = null
                        }) { Text("Utiliser ces infos") }
                        OutlinedButton(onClick = { pendingResult = null }) {
                            Text("Ignorer")
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (coverPath != null) {
                    AsyncImage(
                        model = coverPath,
                        contentDescription = "Couverture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                }
                OutlinedButton(onClick = onTakePhoto) {
                    Text(if (coverPath == null) "Photo de la couverture" else "Reprendre la photo")
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre de la série") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = albumTitle,
                onValueChange = { albumTitle = it },
                label = { Text("Titre de l'album") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = volume,
                onValueChange = { volume = it },
                label = { Text("Tome (3, 1-3 intégrale, ou 6.1 s'il est découpé)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text("Auteur (scénariste)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text("Dessinateur") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = colorist,
                onValueChange = { colorist = it },
                label = { Text("Coloriste") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = publisher,
                onValueChange = { publisher = it },
                label = { Text("Éditeur") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = year,
                onValueChange = { year = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text("Année de parution") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = signed, onCheckedChange = { signed = it })
                Text("Dédicacé")
                Spacer(Modifier.width(16.dp))
                Checkbox(checked = owned, onCheckedChange = { owned = it })
                Text("Possédé")
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onSave(
                        (initial ?: Album()).copy(
                            seriesTitle = title.trim(),
                            albumTitle = albumTitle.trim(),
                            volumeNumber = com.example.mabdtheque.data.VolumeParser.parse(volume)?.start,
                            volumeEnd = com.example.mabdtheque.data.VolumeParser.parse(volume)?.end,
                            volumeSuffix = com.example.mabdtheque.data.VolumeParser.parse(volume)?.suffix ?: "",
                            author = author.trim(),
                            artist = artist.trim(),
                            colorist = colorist.trim(),
                            publisher = publisher.trim(),
                            publicationYear = year.toIntOrNull(),
                            isbn = isbn.trim(),
                            isSigned = signed,
                            owned = owned,
                            notes = notes.trim(),
                            coverPhotoPath = coverPath
                        )
                    )
                },
                enabled = title.isNotBlank() || albumTitle.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enregistrer") }

            Spacer(Modifier.height(16.dp))
        }
    }
}
