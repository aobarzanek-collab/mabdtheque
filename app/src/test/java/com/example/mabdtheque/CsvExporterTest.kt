package com.example.mabdtheque

import com.example.mabdtheque.data.Album
import com.example.mabdtheque.ui.computeMissing
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

    @Test
    fun `displaySubtitle marque les souhaits`() {
        val album = Album(seriesTitle = "Titre", volumeNumber = 5, owned = false)
        assertEquals("Tome 5 · Souhait ♡", album.displaySubtitle)
    }

    @Test
    fun `displayTitle combine serie et titre d'album`() {
        val album = Album(seriesTitle = "100 âmes", albumTitle = "Victimes")
        assertEquals("100 âmes — Victimes", album.displayTitle)
    }

    @Test
    fun `computeMissing detecte les trous d'une serie`() {
        val albums = listOf(
            Album(seriesTitle = "Astérix", volumeNumber = 1, owned = true),
            Album(seriesTitle = "Astérix", volumeNumber = 2, owned = true),
            Album(seriesTitle = "Astérix", volumeNumber = 5, owned = true)
        )
        val result = computeMissing(albums)
        assertEquals(1, result.size)
        assertEquals(listOf(3, 4), result[0].missingVolumes)
    }

    @Test
    fun `computeMissing ignore les tomes non possedes deja connus`() {
        // tome 3 marque comme souhait (Compte=0) : il n'est pas un "manquant"
        val albums = listOf(
            Album(seriesTitle = "Astérix", volumeNumber = 1, owned = true),
            Album(seriesTitle = "Astérix", volumeNumber = 2, owned = true),
            Album(seriesTitle = "Astérix", volumeNumber = 3, owned = false),
            Album(seriesTitle = "Astérix", volumeNumber = 5, owned = true)
        )
        val result = computeMissing(albums)
        assertEquals(1, result.size)
        assertEquals(listOf(4), result[0].missingVolumes)
    }

    @Test
    fun `computeMissing ne remonte pas les series completes`() {
        val albums = listOf(
            Album(seriesTitle = "Série complète", volumeNumber = 1, owned = true),
            Album(seriesTitle = "Série complète", volumeNumber = 2, owned = true)
        )
        assertTrue(computeMissing(albums).isEmpty())
    }
}
