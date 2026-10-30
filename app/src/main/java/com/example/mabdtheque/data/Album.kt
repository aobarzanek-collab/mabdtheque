package com.example.mabdtheque.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class Album(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seriesTitle: String = "",
    val albumTitle: String = "",
    val volumeNumber: Int? = null,
    val volumeEnd: Int? = null,
    val volumeSuffix: String = "",
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
    val volumeSubtitle: String
        get() = when {
            volumeNumber == null -> ""
            volumeEnd != null && volumeEnd != volumeNumber ->
                "Tomes $volumeNumber à $volumeEnd"
            volumeSuffix.isNotBlank() -> "Tome $volumeNumber.$volumeSuffix"
            else -> "Tome $volumeNumber"
        }

    val displaySubtitle: String
        get() = buildList {
            if (volumeSubtitle.isNotBlank()) add(volumeSubtitle)
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
