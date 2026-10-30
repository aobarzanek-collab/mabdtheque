package com.example.mabdtheque.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Query("SELECT * FROM albums ORDER BY seriesTitle COLLATE NOCASE, volumeNumber")
    fun getAll(): Flow<List<Album>>

    @Query(
        "SELECT * FROM albums WHERE seriesTitle LIKE '%' || :q || '%' " +
        "OR albumTitle LIKE '%' || :q || '%' " +
        "OR author LIKE '%' || :q || '%' " +
        "OR artist LIKE '%' || :q || '%' " +
        "OR colorist LIKE '%' || :q || '%' " +
        "OR publisher LIKE '%' || :q || '%' " +
        "OR isbn = :q ORDER BY seriesTitle COLLATE NOCASE, volumeNumber"
    )
    fun search(q: String): Flow<List<Album>>

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun getById(id: Long): Album?

    @Query("SELECT * FROM albums WHERE isbn = :isbn LIMIT 1")
    suspend fun findByIsbn(isbn: String): Album?

    @Query("SELECT * FROM albums ORDER BY seriesTitle COLLATE NOCASE, volumeNumber")
    suspend fun getAllOnce(): List<Album>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: Album): Long

    @Update
    suspend fun update(album: Album)

    @Delete
    suspend fun delete(album: Album)

    @Query("SELECT COUNT(*) FROM albums")
    fun count(): Flow<Int>
}
