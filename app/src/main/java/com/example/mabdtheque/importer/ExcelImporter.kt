package com.example.mabdtheque.importer

import android.content.Context
import android.net.Uri
import com.example.mabdtheque.data.Album
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row

data class ImportResult(
    val albums: List<Album>,
    val errors: List<String>
)

object ExcelImporter {

    fun parse(context: Context, uri: Uri): ImportResult {
        val errors = ArrayList<String>()
        val albums = ArrayList<Album>()
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val workbook = HSSFWorkbook(input)
                val sheet = workbook.getSheetAt(0)
                val header = sheet.getRow(sheet.firstRowNum) ?: return ImportResult(
                    emptyList(), listOf("Feuille vide ou en-tête introuvable")
                )

                val colMap = buildColumnMap(header)

                for (rowIndex in header.rowNum + 1..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex) ?: continue
                    val album = parseRow(row, colMap, rowIndex + 1, errors)
                    if (album != null) albums.add(album)
                }
            }
        } catch (e: Exception) {
            errors.add("Lecture impossible : ${e.message}")
        }
        return ImportResult(albums, errors)
    }

    private fun buildColumnMap(header: Row): Map<String, Int> {
        val map = HashMap<String, Int>()
        for (cellIndex in header.firstCellNum until header.lastCellNum) {
            val cell = header.getCell(cellIndex) ?: continue
            val name = cell.toString().trim().lowercase()
            map[name] = cellIndex
        }
        return map
    }

    private fun parseRow(
        row: Row, colMap: Map<String, Int>, rowNum: Int, errors: List<String>
    ): Album? {
        val title = getCellString(row, colMap, listOf("titre", "serie", "titre de la serie", "title"))
        if (title.isBlank()) return null
        val album = Album(
            seriesTitle = title,
            volumeNumber = getCellInt(row, colMap, listOf("tome", "volume", "numéro", "numero")),
            author = getCellString(row, colMap, listOf("auteur", "scenariste", "scénariste", "author")),
            artist = getCellString(row, colMap, listOf("dessinateur", "dessin", "artist")),
            colorist = getCellString(row, colMap, listOf("coloriste", "couleur", "colors")),
            publicationYear = getCellInt(row, colMap, listOf("annee", "année", "year", "parution")),
            isbn = getCellString(row, colMap, listOf("isbn", "code-barres", "code barres", "ean")),
            notes = getCellString(row, colMap, listOf("notes", "remarques", "commentaire")),
            isSigned = getCellString(row, colMap, listOf("dédicace", "dedicace", "signé", "signe"))
                .let { it == "oui" || it == "x" || it == "true" || it == "vrai" }
        )
        return album
    }

    private fun getCellString(row: Row, colMap: Map<String, Int>, keys: List<String>): String {
        for (key in keys) {
            val col = colMap[key] ?: continue
            val cell = row.getCell(col) ?: continue
            return when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue.trim()
                CellType.NUMERIC -> {
                    val value = cell.numericCellValue
                    if (value == value.toLong().toDouble()) {
                        value.toLong().toString()
                    } else value.toString()
                }
                else -> ""
            }
        }
        return ""
    }

    private fun getCellInt(row: Row, colMap: Map<String, Int>, keys: List<String>): Int? {
        val str = getCellString(row, colMap, keys)
        return str.filter { it.isDigit() }.take(4).toIntOrNull()
    }
}
