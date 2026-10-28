package com.example.mabdtheque.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class AlbumRepository(context: Context) {

    private val dao = AppDatabase.get(context).albumDao()

    fun all(): Flow<List<Album>> = dao.getAll()

    fun search(query: String): Flow<List<Album>> =
        if (query.isBlank()) dao.getAll() else dao.search(query.trim())

    fun count(): Flow<Int> = dao.count()

    suspend fun byId(id: Long): Album? = dao.getById(id)

    suspend fun byIsbn(isbn: String): Album? = dao.findByIsbn(isbn)

    suspend fun allOnce(): List<Album> = dao.getAllOnce()

    suspend fun save(album: Album): Long =
        if (album.id == 0L) dao.insert(album) else { dao.update(album); album.id }

    suspend fun delete(album: Album) = dao.delete(album)
}
