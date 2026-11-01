package com.example.mabdtheque.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mabdtheque.data.Album
import com.example.mabdtheque.data.AlbumRepository
import com.example.mabdtheque.data.Category
import com.example.mabdtheque.lookup.IsbnLookup
import com.example.mabdtheque.lookup.LookupResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = AlbumRepository(app)

    private val query = MutableStateFlow("")

    val searchQuery: StateFlow<String> = query

    fun onSearchQueryChange(q: String) { query.value = q }

    // Collection actuellement ouverte (BD, MANGA, JEUNESSE)
    private val currentCategory = MutableStateFlow(Category.BD)

    fun openCategory(category: Category) {
        currentCategory.value = category
        query.value = ""
    }

    val albums: StateFlow<List<Album>> = currentCategory
        .flatMapLatest { cat ->
            query.flatMapLatest { q -> repository.searchInCategory(cat, q) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = currentCategory
        .flatMapLatest { repository.countInCategory(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val homeCounts: StateFlow<Map<Category, Int>> = kotlinx.coroutines.flow.combine(
        repository.countInCategory(Category.BD),
        repository.countInCategory(Category.MANGA),
        repository.countInCategory(Category.JEUNESSE)
    ) { bd, manga, jeunesse ->
        mapOf(Category.BD to bd, Category.MANGA to manga, Category.JEUNESSE to jeunesse)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun save(album: Album, onSaved: () -> Unit = {}) = viewModelScope.launch {
        repository.save(album)
        onSaved()
    }

    fun delete(album: Album) = viewModelScope.launch { repository.delete(album) }

    suspend fun repositoryAll(): List<Album> = repository.allOnce()

    fun importAlbums(rows: List<Album>, onDone: (Int, Int) -> Unit) =
        viewModelScope.launch {
            var imported = 0
            var duplicates = 0
            val existing = repository.allOnce()
            val existingKeys = existing.map {
                Triple(
                    Triple(it.seriesTitle.trim().lowercase(), it.volumeNumber, it.volumeEnd),
                    it.volumeSuffix,
                    it.isbn.trim()
                )
            }.toMutableSet()
            for (row in rows) {
                val key = Triple(
                    Triple(row.seriesTitle.trim().lowercase(), row.volumeNumber, row.volumeEnd),
                    row.volumeSuffix,
                    row.isbn.trim()
                )
                if (key in existingKeys) {
                    duplicates++
                    continue
                }
                repository.save(row.copy(id = 0))
                existingKeys.add(key)
                imported++
            }
            onDone(imported, duplicates)
        }

    fun lookupIsbn(isbn: String, onResult: (LookupResult?) -> Unit) =
        viewModelScope.launch {
            onResult(IsbnLookup.lookup(isbn))
        }
}
