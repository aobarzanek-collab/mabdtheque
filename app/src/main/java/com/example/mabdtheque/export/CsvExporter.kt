package com.example.mabdtheque.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.mabdtheque.data.Album
import java.io.File

object CsvExporter {

    private val HEADER = listOf(
        "Titre", "Tome", "Auteur", "Dessinateur", "Coloriste",
        "Annee", "ISBN", "Dedicace", "Notes"
    )

    fun export(context: Context, albums: List<Album>): Uri? {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "mabdtheque_${System.currentTimeMillis()}.csv")
        file.writeText(buildString {
            appendLine(HEADER.joinToString(";"))
            albums.forEach { a ->
                appendLine(
                    listOf(
                        a.seriesTitle.csv(),
                        a.volumeNumber?.toString()?.csv() ?: "",
                        a.author.csv(),
                        a.artist.csv(),
                        a.colorist.csv(),
                        a.publicationYear?.toString() ?: "",
                        a.isbn.csv(),
                        if (a.isSigned) "oui" else "non",
                        a.notes.csv()
                    ).joinToString(";")
                )
            }
        })
        return FileProvider.getUriForFile(
            context, context.packageName + ".fileprovider", file
        )
    }

    fun shareIntent(context: Context, uri: Uri): Intent =
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "Exporter ma collection"
        ).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
}

private fun String.csv(): String =
    if (contains(';') || contains('"') || contains('\n')) {
        "\"" + replace("\"", "\"\"") + "\""
    } else this
