package com.example.mabdtheque.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mabdtheque.data.Album
import com.example.mabdtheque.data.AlbumRepository
import com.example.mabdtheque.data.Category
import com.example.mabdtheque.lookup.IsbnLookup
import com.example.mabdtheque.lookup.LookupResult
import com.example.mabdtheque.sync.FirebaseSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = AlbumRepository(app)
    private val appContext: Context = app.applicationContext

    private val query = MutableStateFlow("")

    val searchQuery: StateFlow<String> = query

    fun onSearchQueryChange(q: String) { query.value = q }

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

    // ---------- Synchronisation ----------

    /** Tire le cloud au démarrage si la synchro est configurée. */
    init {
        if (FirebaseSync.isConfigured(appContext)) {
            sync { }
        }
    }

    fun save(album: Album, onSaved: () -> Unit = {}) = viewModelScope.launch {
        // Attribue un identifiant de synchro au premier enregistrement
        val toSave = if (album.syncId.isBlank()) {
            album.copy(syncId = FirebaseSync.newSyncId())
        } else album
        val id = repository.save(toSave)
        // Pousse dans le cloud (silencieusement)
        if (FirebaseSync.isConfigured(appContext)) {
            val pushed = repository.byId(id)
            if (pushed != null) {
                withContext(Dispatchers.IO) {
                    runCatching { FirebaseSync.pushAlbum(appContext, pushed) }
                }
            }
        }
        onSaved()
    }

    fun delete(album: Album) = viewModelScope.launch {
        repository.delete(album)
        if (FirebaseSync.isConfigured(appContext) && album.syncId.isNotBlank()) {
            withContext(Dispatchers.IO) {
                runCatching { FirebaseSync.pushTombstone(appContext, album.syncId) }
            }
        }
    }

    /** Synchronisation complète : tire le cloud, fusionne, pousse les albums locaux sans syncId. */
    fun sync(onDone: (String) -> Unit) = viewModelScope.launch {
        if (!FirebaseSync.isConfigured(appContext)) {
            onDone("Synchro non configurée.")
            return@launch
        }
        val message = withContext(Dispatchers.IO) {
            runCatching {
                val pull = FirebaseSync.pullAll(appContext)
                    ?: return@runCatching "Impossible de contacter la base. Vérifiez l'URL et les règles d'accès."
                val local = repository.allOnce()
                val bySyncId = local.filter { it.syncId.isNotBlank() }
                    .associateBy { it.syncId }.toMutableMap()

                // 1. Applique les suppressions distantes
                pull.deletedSyncIds.forEach { deletedId ->
                    bySyncId[deletedId]?.let { repository.delete(it) }
                }

                // 2. Intègre les albums distants (nouveaux ou mis à jour)
                var added = 0
                var updated = 0
                pull.albums.forEach { remote ->
                    if (remote.syncId.isBlank()) return@forEach
                    val existing = bySyncId[remote.syncId]
                    if (existing == null) {
                        repository.save(remote.copy(id = 0))
                        added++
                    } else if (existing != remote.copy(id = existing.id, createdAt = existing.createdAt)) {
                        repository.save(remote.copy(id = existing.id))
                        updated++
                    }
                }

                // 3. Pousse les albums locaux qui n'existent pas encore dans le cloud
                var pushed = 0
                val afterPull = repository.allOnce()
                val remoteIds = pull.albums.map { it.syncId }.toSet()
                afterPull.filter { it.syncId.isNotBlank() && it.syncId !in remoteIds }
                    .forEach { album ->
                        if (FirebaseSync.pushAlbum(appContext, album)) pushed++
                    }

                // 4. Supprime du cloud les albums supprimés localement
                val localIds = afterPull.map { it.syncId }.toSet()
                pull.albums
                    .filter { it.syncId.isNotBlank() && it.syncId !in localIds }
                    .forEach { FirebaseSync.pushTombstone(appContext, it.syncId) }

                "Sync terminée : +$added ajoutés, ~$updated mis à jour, $pushed envoyés."
            }.getOrElse { "Erreur de synchronisation : ${it.message}" }
        }
        onDone(message)
    }

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
            val importedAlbums = ArrayList<Album>()
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
                val withSyncId = row.copy(
                    id = 0, syncId = FirebaseSync.newSyncId()
                )
                repository.save(withSyncId)
                importedAlbums.add(withSyncId)
                existingKeys.add(key)
                imported++
            }
            // Pousse les albums importés dans le cloud
            if (FirebaseSync.isConfigured(appContext)) {
                withContext(Dispatchers.IO) {
                    importedAlbums.forEach {
                        runCatching { FirebaseSync.pushAlbum(appContext, it) }
                    }
                }
            }
            onDone(imported, duplicates)
        }

    fun lookupIsbn(isbn: String, onResult: (LookupResult?) -> Unit) =
        viewModelScope.launch {
            onResult(IsbnLookup.lookup(isbn))
        }
}
