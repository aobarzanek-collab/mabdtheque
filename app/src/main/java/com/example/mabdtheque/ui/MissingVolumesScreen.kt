package com.example.mabdtheque.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mabdtheque.data.Album

data class SeriesMissing(
    val seriesTitle: String,
    val ownedVolumes: List<Int>,
    val missingVolumes: List<Int>,
    val totalCounted: Int
)

fun computeMissing(albums: List<Album>): List<SeriesMissing> {
    return albums
        .filter { it.volumeNumber != null }
        .groupBy { it.seriesTitle.trim().lowercase() }
        .map { (series, list) ->
            val owned = list.filter { it.owned }
                .mapNotNull { it.volumeNumber }.distinct().sorted()
            val known = list.mapNotNull { it.volumeNumber }.distinct().sorted()
            val missing = when {
                owned.isEmpty() -> emptyList()
                else -> (owned.first()..owned.last())
                    .filter { it !in known }
            }
            SeriesMissing(
                seriesTitle = list.first().seriesTitle.ifBlank { series },
                ownedVolumes = owned,
                missingVolumes = missing,
                totalCounted = owned.size
            )
        }
        .filter { it.missingVolumes.isNotEmpty() }
        .sortedBy { it.seriesTitle.lowercase() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissingVolumesScreen(
    missing: List<SeriesMissing>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tomes manquants") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Text("✕")
                    }
                }
            )
        }
    ) { padding ->
        if (missing.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Aucun tome manquant détecté. Toutes vos séries sont complètes !",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(missing) { series ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                series.seriesTitle,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Possédés : ${series.ownedVolumes.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Manquants : ${series.missingVolumes.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
