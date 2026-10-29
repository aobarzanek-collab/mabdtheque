package com.example.mabdtheque.lookup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

data class LookupResult(
    val title: String = "",
    val authors: List<String> = emptyList(),
    val year: Int? = null,
    val publisher: String? = null
)

object IsbnLookup {

    suspend fun lookup(isbn: String): LookupResult? = withContext(Dispatchers.IO) {
        googleBooks(isbn) ?: openLibrary(isbn)
    }

    private fun googleBooks(isbn: String): LookupResult? = runCatching {
        val url = URL(
            "https://www.googleapis.com/books/v1/volumes?q=isbn:" +
                URLEncoder.encode(isbn, "UTF-8")
        )
        val root = JSONObject(url.readText())
        val info = root.optJSONArray("items")
            ?.optJSONObject(0)
            ?.optJSONObject("volumeInfo")
        val title = info?.optString("title", "") ?: ""
        if (info == null || title.isBlank()) null else {
            val authors = ArrayList<String>()
            info.optJSONArray("authors")?.let { arr ->
                for (i in 0 until arr.length()) authors.add(arr.optString(i))
            }
            LookupResult(
                title = title,
                authors = authors,
                year = info.optString("publishedDate").take(4).toIntOrNull(),
                publisher = info.optString("publisher").takeIf { it.isNotBlank() }
            )
        }
    }.getOrNull()

    private fun openLibrary(isbn: String): LookupResult? = runCatching {
        val url = URL(
            "https://openlibrary.org/isbn/" +
                URLEncoder.encode(isbn, "UTF-8") + ".json"
        )
        val json = JSONObject(url.readText())
        val title = json.optString("title", "")
        if (title.isBlank()) null else LookupResult(
            title = title,
            year = json.optString("publish_date").take(4).toIntOrNull(),
            publisher = json.optJSONArray("publishers")?.optString(0)
        )
    }.getOrNull()
}
