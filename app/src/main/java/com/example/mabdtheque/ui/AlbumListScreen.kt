package com.example.mabdtheque.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mabdtheque.data.Album
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumListScreen(
    albums: List<Album>,
    totalCount: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Album) -> Unit,
    onDelete: (Album) -> Unit,
    onExportCsv: () -> Unit,
    onImportExcel: () -> Unit,
    onShowMissing: () -> Unit,
    listState: LazyListState,
    categoryLabel: String,
    onBack: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Index alphabétique : lettres présentes dans la collection triée
    val letterPositions by remember(albums) {
        derivedStateOf {
            val map = LinkedHashMap<Char, Int>()
            albums.forEachIndexed { index, album ->
                val c = album.seriesTitle.trim().firstOrNull()?.uppercaseChar() ?: '?'
                if (!map.containsKey(c)) map[c] = index
            }
            map
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$categoryLabel ($totalCount) — v1.4") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        androidx.compose.material.icons.Icons
                        Icon(
                            androidx.compose.material.icons.automirrored.filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    androidx.compose.material3.DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Tomes manquants") },
                            onClick = { menuOpen = false; onShowMissing() }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Importer Excel") },
                            onClick = { menuOpen = false; onImportExcel() }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Exporter CSV") },
                            onClick = { menuOpen = false; onExportCsv() }
                        )
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un album")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Rechercher (titre, auteur, dessinateur…)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (albums.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (totalCount == 0) "Aucun album pour l'instant.\nAppuyez sur + pour en ajouter un."
                        else "Aucun résultat pour cette recherche.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 16.dp, vertical = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(albums, key = { it.id }) { album ->
                            AlbumRow(album, onEdit, onDelete)
                        }
                    }

                    // Index alphabétique latéral
                    if (letterPositions.size > 1) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(vertical = 64.dp, horizontal = 2.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            letterPositions.keys.forEach { letter ->
                                Text(
                                    text = letter.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            letterPositions[letter]?.let { index ->
                                                scope.launch {
                                                    listState.scrollToItem(index)
                                                }
                                            }
                                        }
                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumRow(
    album: Album,
    onEdit: (Album) -> Unit,
    onDelete: (Album) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onEdit(album) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = album.coverPhotoPath,
            contentDescription = "Couverture",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                album.displayTitle,
                style = MaterialTheme.typography.titleMedium
            )
            if (album.displaySubtitle.isNotBlank()) {
                Text(
                    album.displaySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (album.author.isNotBlank()) {
                Text(
                    "Scénario : " + album.author,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!album.owned) {
                Text(
                    "Souhait (pas encore possédé)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        IconButton(onClick = { onDelete(album) }) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Supprimer",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
