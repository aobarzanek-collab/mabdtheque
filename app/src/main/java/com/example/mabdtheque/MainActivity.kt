package com.example.mabdtheque

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.mabdtheque.camera.CoverPhotoScreen
import com.example.mabdtheque.data.Album
import com.example.mabdtheque.export.CsvExporter
import com.example.mabdtheque.scanner.BarcodeScannerScreen
import com.example.mabdtheque.ui.AlbumEditScreen
import com.example.mabdtheque.ui.AlbumListScreen
import com.example.mabdtheque.ui.CollectionViewModel
import kotlinx.coroutines.launch

private sealed interface Screen {
    data object List : Screen
    data object ScanIsbn : Screen
    data object TakePhoto : Screen
    data class Edit(val albumId: Long?) : Screen
}

class MainActivity : ComponentActivity() {

    private val collectionViewModel: CollectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val albums by collectionViewModel.albums.collectAsState()
            val total by collectionViewModel.totalCount.collectAsState()
            val query by collectionViewModel.searchQuery.collectAsState()

            var screen by remember { mutableStateOf<Screen>(Screen.List) }
            var draft by remember { mutableStateOf<Album?>(null) }

            when (val current = screen) {
                Screen.List -> {
                    AlbumListScreen(
                        albums = albums,
                        totalCount = total,
                        searchQuery = query,
                        onSearchQueryChange = collectionViewModel::onSearchQueryChange,
                        onAdd = { draft = null; screen = Screen.Edit(null) },
                        onEdit = { draft = it; screen = Screen.Edit(it.id) },
                        onDelete = { collectionViewModel.delete(it) },
                        onExportCsv = { exportCsv() }
                    )
                }

                Screen.ScanIsbn -> {
                    BarcodeScannerScreen(
                        onBarcode = { code ->
                            draft = (draft ?: Album()).copy(isbn = code)
                            screen = Screen.Edit(draft?.id)
                        },
                        onClose = { screen = Screen.Edit(draft?.id) }
                    )
                }

                Screen.TakePhoto -> {
                    CoverPhotoScreen(
                        onPhotoTaken = { _, path ->
                            draft = (draft ?: Album()).copy(coverPhotoPath = path)
                            screen = Screen.Edit(draft?.id)
                        },
                        onClose = { screen = Screen.Edit(draft?.id) }
                    )
                }

                is Screen.Edit -> {
                    LaunchedEffect(current.albumId) {
                        if (draft == null && current.albumId != null) {
                            screen = Screen.List
                        }
                    }
                    AlbumEditScreen(
                        initial = draft,
                        onSave = { album ->
                            collectionViewModel.save(album) { screen = Screen.List }
                        },
                        onScanIsbn = { screen = Screen.ScanIsbn },
                        onTakePhoto = { screen = Screen.TakePhoto },
                        onBack = { screen = Screen.List }
                    )
                }
            }
        }
    }

    private fun exportCsv() {
        androidx.lifecycle.lifecycleScope.launch {
            val all = collectionViewModel.repositoryAll()
            val uri = CsvExporter.export(this@MainActivity, all)
            uri?.let { startActivity(CsvExporter.shareIntent(this@MainActivity, it)) }
        }
    }
}
