package com.example.mabdtheque

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.mabdtheque.camera.CoverPhotoScreen
import com.example.mabdtheque.camera.CropScreen
import com.example.mabdtheque.data.Album
import com.example.mabdtheque.data.Category
import com.example.mabdtheque.export.CsvExporter
import com.example.mabdtheque.importer.ImportScreen
import com.example.mabdtheque.lookup.LookupResult
import com.example.mabdtheque.scanner.BarcodeScannerScreen
import com.example.mabdtheque.ui.AlbumEditScreen
import com.example.mabdtheque.ui.AlbumListScreen
import com.example.mabdtheque.ui.CollectionViewModel
import com.example.mabdtheque.ui.HomeScreen
import com.example.mabdtheque.ui.MissingVolumesScreen
import com.example.mabdtheque.ui.computeMissing
import kotlinx.coroutines.launch

private sealed interface Screen {
    data object Home : Screen
    data class List(val category: Category) : Screen
    data object ScanIsbn : Screen
    data object TakePhoto : Screen
    data class Crop(val photoPath: String) : Screen
    data class Edit(val albumId: Long?) : Screen
    data object Import : Screen
    data object Missing : Screen
}

class MainActivity : ComponentActivity() {

    private val collectionViewModel: CollectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val albums by collectionViewModel.albums.collectAsState()
            val total by collectionViewModel.totalCount.collectAsState()
            val query by collectionViewModel.searchQuery.collectAsState()
            val homeCounts by collectionViewModel.homeCounts.collectAsState()

            var screen by remember { mutableStateOf<Screen>(Screen.Home) }
            var draft by remember { mutableStateOf<Album?>(null) }
            var activeCategory by remember { mutableStateOf(Category.BD) }

            // État de défilement de la liste : survit aux allers-retours.
            val listState = rememberLazyListState()
            rememberSaveable(
                stateSaver = Saver(
                    save = {
                        listOf(
                            listState.firstVisibleItemIndex,
                            listState.firstVisibleItemScrollOffset
                        )
                    },
                    restore = { values ->
                        @Suppress("UNCHECKED_CAST")
                        val v = values as List<Int>
                        kotlinx.coroutines.runBlocking {
                            listState.scrollToItem(v[0], v[1])
                        }
                        true
                    }
                )
            ) { mutableStateOf(true) }

            LaunchedEffect(screen) {
                val cat = (screen as? Screen.List)?.category
                if (cat != null) {
                    activeCategory = cat
                    collectionViewModel.openCategory(cat)
                }
            }

            when (val current = screen) {
                Screen.Home -> {
                    HomeScreen(
                        counts = homeCounts,
                        onOpen = { screen = Screen.List(it) }
                    )
                }

                is Screen.List -> {
                    AlbumListScreen(
                        albums = albums,
                        totalCount = total,
                        searchQuery = query,
                        onSearchQueryChange = collectionViewModel::onSearchQueryChange,
                        listState = listState,
                        categoryLabel = activeCategory.label,
                        onBack = { screen = Screen.Home },
                        onAdd = { draft = null; screen = Screen.Edit(null) },
                        onEdit = { draft = it; screen = Screen.Edit(it.id) },
                        onDelete = { collectionViewModel.delete(it) },
                        onExportCsv = { exportCsv() },
                        onImportExcel = { screen = Screen.Import },
                        onShowMissing = { screen = Screen.Missing }
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
                            screen = Screen.Crop(path)
                        },
                        onClose = { screen = Screen.Edit(draft?.id) }
                    )
                }

                is Screen.Crop -> {
                    CropScreen(
                        path = current.photoPath,
                        onDone = { path ->
                            draft = (draft ?: Album()).copy(coverPhotoPath = path)
                            screen = Screen.Edit(draft?.id)
                        },
                        onCancel = {
                            screen = Screen.Edit(draft?.id)
                        }
                    )
                }

                Screen.Import -> {
                    ImportScreen(
                        onImport = { rows, done ->
                            collectionViewModel.importAlbums(rows, done)
                        },
                        onBack = { screen = Screen.Home }
                    )
                }

                Screen.Missing -> {
                    MissingVolumesScreen(
                        missing = computeMissing(albums),
                        onBack = { screen = Screen.List(activeCategory) }
                    )
                }

                is Screen.Edit -> {
                    val doLookup: (String, (LookupResult?) -> Unit) -> Unit =
                        { isbn, cb -> collectionViewModel.lookupIsbn(isbn, cb) }
                    LaunchedEffect(current.albumId) {
                        if (draft == null && current.albumId != null) {
                            screen = Screen.List(activeCategory)
                        }
                    }
                    AlbumEditScreen(
                        initial = draft,
                        category = activeCategory,
                        onCategoryChange = { activeCategory = it },
                        onSave = { album ->
                            collectionViewModel.save(album) {
                                screen = Screen.List(activeCategory)
                            }
                        },
                        onScanIsbn = { currentDraft -> draft = currentDraft; screen = Screen.ScanIsbn },
                        onTakePhoto = { currentDraft -> draft = currentDraft; screen = Screen.TakePhoto },
                        onLookupIsbn = doLookup,
                        onBack = { screen = Screen.List(activeCategory) }
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
