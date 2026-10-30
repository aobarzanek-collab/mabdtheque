package com.example.mabdtheque

import com.example.mabdtheque.data.Album
import com.example.mabdtheque.data.ParsedVolume
import com.example.mabdtheque.data.VolumeParser
import com.example.mabdtheque.ui.computeMissing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumTest {

    // ---- VolumeParser ----

    @Test
    fun `parse tome simple`() {
        assertEquals(ParsedVolume(3, null, ""), VolumeParser.parse("3"))
        assertEquals(ParsedVolume(12, null, ""), VolumeParser.parse(" 12 "))
        assertEquals(null, VolumeParser.parse(""))
        assertEquals(null, VolumeParser.parse("abc"))
        assertEquals(null, VolumeParser.parse("-"))
    }

    @Test
    fun `parse integrale avec tiret`() {
        assertEquals(ParsedVolume(1, 3, ""), VolumeParser.parse("1-3"))
        assertEquals(ParsedVolume(1, 3, ""), VolumeParser.parse("1–3"))
        assertEquals(ParsedVolume(1, 3, ""), VolumeParser.parse("1 à 3"))
        assertEquals(ParsedVolume(2, 9, ""), VolumeParser.parse("2-9"))
        assertEquals(ParsedVolume(1, 3, ""), VolumeParser.parse("3-1"))
    }

    @Test
    fun `parse integrale avec separateurs`() {
        assertEquals(ParsedVolume(1, 3, ""), VolumeParser.parse("1,2,3"))
        assertEquals(ParsedVolume(1, 3, ""), VolumeParser.parse("1/2/3"))
    }

    @Test
    fun `parse integrale marquee Int`() {
        assertEquals(ParsedVolume(2, 2, ""), VolumeParser.parse("Int. 2"))
        assertEquals(ParsedVolume(1, 1, ""), VolumeParser.parse("int"))
        assertEquals(ParsedVolume(3, 3, ""), VolumeParser.parse("Intégrale3"))
    }

    @Test
    fun `parse tome decoupe 6-1 et 6-2`() {
        assertEquals(ParsedVolume(6, null, "1"), VolumeParser.parse("6.1"))
        assertEquals(ParsedVolume(6, null, "2"), VolumeParser.parse("6.2"))
        assertEquals(ParsedVolume(1, null, "1"), VolumeParser.parse("1.1"))
    }

    @Test
    fun `format tome decoupe et integrale`() {
        assertEquals("6.1", VolumeParser.format(6, null, "1"))
        assertEquals("1-3", VolumeParser.format(1, 3, ""))
        assertEquals("3", VolumeParser.format(3, null, ""))
        assertEquals(null, VolumeParser.format(null, null, ""))
    }

    // ---- Affichage ----

    @Test
    fun `displaySubtitle contient tome annee et dedicace`() {
        val album = Album(
            seriesTitle = "Astérix", volumeNumber = 1,
            publicationYear = 1961, isSigned = true
        )
        assertEquals("Tome 1 · 1961 · Dédicacé ✦", album.displaySubtitle)
    }

    @Test
    fun `displaySubtitle marque les souhaits`() {
        val album = Album(seriesTitle = "Titre", volumeNumber = 5, owned = false)
        assertEquals("Tome 5 · Souhait ♡", album.displaySubtitle)
    }

    @Test
    fun `displaySubtitle affiche tome decoupe`() {
        val album = Album(seriesTitle = "Travis", volumeNumber = 6, volumeSuffix = "1")
        assertEquals("Tome 6.1", album.displaySubtitle)
    }

    // ---- computeMissing ----

    @Test
    fun `computeMissing ne marque pas les tomes d'une integrale comme manquants`() {
        val albums = listOf(
            Album(seriesTitle = "Série", volumeNumber = 1, volumeEnd = 3, owned = true),
            Album(seriesTitle = "Série", volumeNumber = 4, owned = true)
        )
        assertTrue(computeMissing(albums).isEmpty())
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
    fun `computeMissing avec serie Travis tomes decoupes 6-1 6-2`() {
        val albums = listOf(
            Album(seriesTitle = "Travis", volumeNumber = 1, owned = true),
            Album(seriesTitle = "Travis", volumeNumber = 2, owned = true),
            Album(seriesTitle = "Travis", volumeNumber = 3, owned = true),
            Album(seriesTitle = "Travis", volumeNumber = 4, owned = true),
            Album(seriesTitle = "Travis", volumeNumber = 5, owned = true),
            Album(seriesTitle = "Travis", volumeNumber = 6, volumeSuffix = "1", owned = true),
            Album(seriesTitle = "Travis", volumeNumber = 6, volumeSuffix = "2", owned = true),
            Album(seriesTitle = "Travis", volumeNumber = 7, owned = true)
        )
        val result = computeMissing(albums)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `computeMissing ignore les tomes non possedes deja connus`() {
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
