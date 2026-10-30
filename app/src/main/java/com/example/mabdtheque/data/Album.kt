package com.example.mabdtheque.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class Album(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seriesTitle: String = "",
    val albumTitle: String = "",
    val volumeNumber: Int? = null,
    val author: String = "",
    val artist: String = "",
    val colorist: String = "",
    val publisher: String = "",
    val publicationYear: Int? = null,
    val isbn: String = "",
    val isSigned: Boolean = false,
    val owned: Boolean = true,
    val coverPhotoPath: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val displaySubtitle: String
        get() = buildList {
            volumeNumber?.let { add("Tome $it") }
            publicationYear?.let { add(it.toString()) }
            if (isSigned) add("Dédicacé \u2726")
            if (!owned) add("Souhait \u2661")
        }.joinToString(" · ")

    val displayTitle: String
        get() = when {
            seriesTitle.isBlank() && albumTitle.isBlank() -> "(Sans titre)"
            seriesTitle.isBlank() -> albumTitle
            albumTitle.isBlank() -> seriesTitle
            else -> "$seriesTitle — $albumTitle"
        }
}
