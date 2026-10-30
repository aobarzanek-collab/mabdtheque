package com.example.mabdtheque

import com.example.mabdtheque.data.Album
import com.example.mabdtheque.data.VolumeParser
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
    fun `parse tome simple`() {
        assertEquals(3 to null, VolumeParser.parse("3"))
        assertEquals(12 to null, VolumeParser.parse(" 12 "))
        assertEquals(null, VolumeParser.parse(""))
        assertEquals(null, VolumeParser.parse("abc"))
    }

    @Test
    fun `parse integrale avec tiret`() {
        assertEquals(1 to 3, VolumeParser.parse("1-3"))
        assertEquals(1 to 3, VolumeParser.parse("1–3"))
        assertEquals(1 to 3, VolumeParser.parse("1 à 3"))
        assertEquals(2 to 9, VolumeParser.parse("2-9"))
        assertEquals(1 to 3, VolumeParser.parse("3-1"))
    }

    @Test
    fun `parse integrale avec separateurs`() {
        assertEquals(1 to 3, VolumeParser.parse("1,2,3"))
        assertEquals(1 to 3, VolumeParser.parse("1/2/3"))
    }

    @Test
    fun `computeMissing ne marque pas les tomes d'une integrale comme manquants`() {
        // Intégrale tomes 1-3 + tome 4 : aucun manquant
        val albums = listOf(
            Album(seriesTitle = "Série", volumeNumber = 1, volumeEnd = 3, owned = true),
            Album(seriesTitle = "Série", volumeNumber = 4, owned = true)
        )
        val result = computeMissing(albums)
        assertTrue(result.isEmpty())
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
