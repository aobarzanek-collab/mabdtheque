package com.example.mabdtheque.sync

import android.content.Context
import com.example.mabdtheque.data.Album
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Moteur de synchronisation via Firebase Realtime Database (API REST).
 * Chaque famille partage une "base" identifiée par un code d'accès ;
 * les albums sont synchronisés à la clé <baseUrl>/<code>/albums/<syncId>.
 */
object FirebaseSync {

    private const val PREFS = "sync_prefs"
    private const val KEY_URL = "base_url"
    private const val KEY_CODE = "access_code"

    fun getUrl(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_URL, "") ?: ""

    fun getCode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CODE, "") ?: ""

    fun isConfigured(context: Context): Boolean =
        getUrl(context).isNotBlank() && getCode(context).isNotBlank()

    fun saveConfig(context: Context, baseUrl: String, code: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_URL, baseUrl.trim().trimEnd('/'))
            .putString(KEY_CODE, code.trim())
            .apply()
    }

    // ---- JSON ----

    fun albumToJson(album: Album): JSONObject = JSONObject().apply {
        put("syncId", album.syncId)
        put("seriesTitle", album.seriesTitle)
        put("albumTitle", album.albumTitle)
        put("volumeNumber", album.volumeNumber ?: JSONObject.NULL)
        put("volumeEnd", album.volumeEnd ?: JSONObject.NULL)
        put("volumeSuffix", album.volumeSuffix)
        put("author", album.author)
        put("artist", album.artist)
        put("colorist", album.colorist)
        put("publisher", album.publisher)
        put("publicationYear", album.publicationYear ?: JSONObject.NULL)
        put("isbn", album.isbn)
        put("isSigned", album.isSigned)
        put("owned", album.owned)
        put("category", album.category)
        put("coverPhotoPath", album.coverPhotoPath ?: "")
        put("notes", album.notes)
        put("updatedAt", System.currentTimeMillis())
    }

    fun jsonToAlbum(json: JSONObject): Album = Album(
        syncId = json.optString("syncId", ""),
        seriesTitle = json.optString("seriesTitle", ""),
        albumTitle = json.optString("albumTitle", ""),
        volumeNumber = if (json.isNull("volumeNumber")) null
            else json.optInt("volumeNumber"),
        volumeEnd = if (json.isNull("volumeEnd")) null else json.optInt("volumeEnd"),
        volumeSuffix = json.optString("volumeSuffix", ""),
        author = json.optString("author", ""),
        artist = json.optString("artist", ""),
        colorist = json.optString("colorist", ""),
        publisher = json.optString("publisher", ""),
        publicationYear = if (json.isNull("publicationYear")) null
            else json.optInt("publicationYear"),
        isbn = json.optString("isbn", ""),
        isSigned = json.optBoolean("isSigned", false),
        owned = json.optBoolean("owned", true),
        category = json.optString("category", "BD"),
        coverPhotoPath = json.optString("coverPhotoPath").ifBlank { null },
        notes = json.optString("notes", "")
    )

    // ---- HTTP REST ----

    private fun request(
        url: String, method: String, body: String? = null
    ): String? {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = method
            conn.connectTimeout = 10000
            conn.readTimeout = 15000
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                OutputStreamWriter(conn.outputStream).use { it.write(body) }
            }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream?.bufferedReader()?.use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    /** Pousse (ou remplace) un album dans le cloud. */
    fun pushAlbum(context: Context, album: Album): Boolean {
        if (!isConfigured(context) || album.syncId.isBlank()) return false
        val url = "${getUrl(context)}/${getCode(context)}/albums/${album.syncId}.json"
        return request(url, "PUT", albumToJson(album).toString()) != null
    }

    /** Signale la suppression d'un album (pierre tombale) dans le cloud. */
    fun pushTombstone(context: Context, syncId: String): Boolean {
        if (!isConfigured(context) || syncId.isBlank()) return false
        val url = "${getUrl(context)}/${getCode(context)}/deletions/$syncId.json"
        return request(
            url, "PUT",
            JSONObject().put("deletedAt", System.currentTimeMillis()).toString()
        ) != null
    }

    data class PullResult(
        val albums: List<Album>,
        val deletedSyncIds: List<String>
    )

    /** Récupère tous les albums du cloud + les suppressions. */
    fun pullAll(context: Context): PullResult? {
        if (!isConfigured(context)) return null
        val base = "${getUrl(context)}/${getCode(context)}"
        val albumsJson = request("$base/albums.json", "GET") ?: return null
        val albums = ArrayList<Album>()
        if (albumsJson != "null") {
            val root = JSONObject(albumsJson)
            root.keys().forEach { key ->
                runCatching { albums.add(jsonToAlbum(root.getJSONObject(key))) }
            }
        }
        val deleted = ArrayList<String>()
        val delJson = request("$base/deletions.json", "GET")
        if (delJson != null && delJson != "null") {
            val root = JSONObject(delJson)
            root.keys().forEach { deleted.add(it) }
        }
        return PullResult(albums, deleted)
    }

    fun newSyncId(): String = UUID.randomUUID().toString()
}
