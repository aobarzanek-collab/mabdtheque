package com.example.mabdtheque.data

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class Album(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seriesTitle: String = "",
    val volumeNumber: Int? = null,
    val author: String = "",
    val artist: String = "",
    val colorist: String = "",
    val publicationYear: Int? = null,
    val isbn: String = "",
    val isSigned: Boolean = false,
    val coverPhotoPath: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val coverUri: Uri?
        get() = coverPhotoPath?.let { Uri.parse(it) }

    val displaySubtitle: String
        get() = buildList {
            volumeNumber?.let { add("Tome $it") }
            publicationYear?.let { add(it.toString()) }
            if (isSigned) add("Dédicacé \u2726")
        }.joinToString(" · ")
}
