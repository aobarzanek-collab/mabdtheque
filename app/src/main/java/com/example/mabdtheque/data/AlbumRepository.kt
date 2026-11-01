package com.example.mabdtheque.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class AlbumRepository(context: Context) {

    private val dao = AppDatabase.get(context).albumDao()

    fun inCategory(category: Category): Flow<List<Album>> =
        dao.getAllInCategory(category.name)

    fun searchInCategory(category: Category, query: String): Flow<List<Album>> =
        if (query.isBlank()) dao.getAllInCategory(category.name)
        else dao.searchInCategory(category.name, query.trim())

    fun countInCategory(category: Category): Flow<Int> =
        dao.countInCategory(category.name)

    suspend fun byId(id: Long): Album? = dao.getById(id)

    suspend fun allOnce(): List<Album> = dao.getAllOnce()

    suspend fun save(album: Album): Long =
        if (album.id == 0L) dao.insert(album) else { dao.update(album); album.id }

    suspend fun delete(album: Album) = dao.delete(album)
}
