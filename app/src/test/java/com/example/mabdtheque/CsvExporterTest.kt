package com.example.mabdtheque

import com.example.mabdtheque.data.Album
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumTest {

    @Test
    fun `displaySubtitle contient tome annee et dedicace`() {
        val album = Album(
            seriesTitle = "Astérix",
            volumeNumber = 1,
            publicationYear = 1961,
            isSigned = true
        )
        assertEquals("Tome 1 · 1961 · Dédicacé ✦", album.displaySubtitle)
    }

    @Test
    fun `displaySubtitle vide quand album minimal`() {
        val album = Album(seriesTitle = "Titre")
        assertEquals("", album.displaySubtitle)
    }
}
