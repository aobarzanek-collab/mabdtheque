package com.example.mabdtheque.importer

import android.content.Context
import android.net.Uri
import com.example.mabdtheque.data.Album
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory

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
                val workbook: Workbook = WorkbookFactory.create(input)
                for (sheetIndex in 0 until workbook.numberOfSheets) {
                    val sheet = workbook.getSheetAt(sheetIndex)
                    parseSheet(sheet, albums, errors)
                }
            }
        } catch (e: Exception) {
            errors.add("Lecture impossible : ${e.message}")
        }
        return ImportResult(albums, errors)
    }

    private fun parseSheet(sheet: Sheet, albums: MutableList<Album>, errors: MutableList<String>) {
        val headerRow = sheet.getRow(sheet.firstRowNum) ?: return
        val colMap = buildColumnMap(headerRow)
        if (!colMap.containsKeyAny(listOf("serie", "titre", "title"))) {
            errors.add("Feuille '${sheet.sheetName}' ignorée : colonne série/titre introuvable")
            return
        }
        for (rowIndex in headerRow.rowNum + 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            val album = parseRow(row, colMap)
            if (album != null) albums.add(album)
        }
    }

    private fun Map<String, Int>.containsKeyAny(keys: List<String>): Boolean =
        keys.any { containsKey(it) }

    private fun buildColumnMap(header: Row): Map<String, Int> {
        val map = HashMap<String, Int>()
        for (cellIndex in header.firstCellNum until header.lastCellNum) {
            val cell = header.getCell(cellIndex) ?: continue
            val name = cell.toString().trim().lowercase()
            if (name.isNotBlank()) map[name] = cellIndex
        }
        return map
    }

    private fun parseRow(row: Row, colMap: Map<String, Int>): Album? {
        val series = getCellString(row, colMap, listOf("serie", "série"))
        val title = getCellString(row, colMap, listOf("titre", "title"))
        if (series.isBlank() && title.isBlank()) return null

        val ownedRaw = getCellString(row, colMap, listOf("compte", "possede", "possédé"))
        val owned = when {
            ownedRaw.isBlank() -> true
            else -> {
                val n = ownedRaw.filter { it.isDigit() }.toDoubleOrNull()
                n == null || n > 0
            }
        }

        return Album(
            seriesTitle = series,
            albumTitle = title,
            volumeNumber = getCellInt(row, colMap, listOf("tome", "volume", "numero", "numéro")),
            author = getCellString(row, colMap, listOf("scenariste", "scénariste", "auteur", "author")),
            artist = getCellString(row, colMap, listOf("dessinateur", "dessin")),
            colorist = getCellString(row, colMap, listOf("coloriste", "couleur")),
            publisher = getCellString(row, colMap, listOf("editeur", "éditeur", "publisher")),
            publicationYear = getCellInt(row, colMap, listOf("annee", "année", "year", "parution")),
            isbn = getCellString(row, colMap, listOf("isbn", "code-barres", "code barres", "ean")),
            isSigned = getCellString(row, colMap, listOf("dedicace", "dédicace", "signe", "signé"))
                .let { it == "oui" || it == "x" || it == "true" || it == "vrai" },
            owned = owned,
            notes = getCellString(row, colMap, listOf("notes", "remarques", "commentaire"))
        )
    }

    private fun getCellString(row: Row, colMap: Map<String, Int>, keys: List<String>): String {
        for (key in keys) {
            val col = colMap[key] ?: continue
            val cell: Cell? = row.getCell(col)
            if (cell == null) continue
            return when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue.trim()
                CellType.NUMERIC -> {
                    val value = cell.numericCellValue
                    if (value == value.toLong().toDouble()) {
                        value.toLong().toString()
                    } else value.toString()
                }
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
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
