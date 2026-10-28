package com.example.mabdtheque.lookup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

data class LookupResult(
    val title: String,
    val year: Int? = null,
    val publisher: String? = null
)

object IsbnLookup {

    suspend fun lookup(isbn: String): LookupResult? = withContext(Dispatchers.IO) {
        runCatching {
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
}
