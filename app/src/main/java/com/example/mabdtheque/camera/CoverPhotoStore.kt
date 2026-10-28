package com.example.mabdtheque.camera

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object CoverPhotoStore {

    fun newPhotoFile(context: Context): File =
        File(context.filesDir, "covers").apply { mkdirs() }
            .let { dir -> File(dir, "cover_${System.currentTimeMillis()}.jpg") }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )

    fun delete(path: String?) {
        path ?: return
        runCatching { File(path).delete() }
    }
}
