package com.example.mabdtheque.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mabdtheque.data.Album
import com.example.mabdtheque.data.AlbumRepository
import com.example.mabdtheque.lookup.IsbnLookup
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = AlbumRepository(app)

    private val query = MutableStateFlow("")

    val searchQuery: StateFlow<String> = query

    val albums: StateFlow<List<Album>> = query
        .flatMapLatest { repository.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = repository.count()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun onSearchQueryChange(q: String) { query.value = q }

    fun save(album: Album, onSaved: () -> Unit = {}) = viewModelScope.launch {
        repository.save(album)
        onSaved()
    }

    fun delete(album: Album) = viewModelScope.launch { repository.delete(album) }

    suspend fun repositoryAll(): List<Album> = repository.allOnce()

    fun lookupIsbn(isbn: String, onResult: (String, Int?) -> Unit) =
        viewModelScope.launch {
            val result = IsbnLookup.lookup(isbn)
            onResult(result?.title ?: "", result?.year)
        }
}
