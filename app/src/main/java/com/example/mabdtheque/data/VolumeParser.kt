package com.example.mabdtheque.data

data class ParsedVolume(
    val start: Int,
    val end: Int?,   // non-null si intégrale
    val suffix: String  // ex. "1" pour "6.1", "" sinon
) {
    val isIntegrale: Boolean get() = end != null && end != start
}

object VolumeParser {

    /**
     * Parse un champ "tome" souple. Formats reconnus :
     *  - "3"                -> tome 3 seul
     *  - "1-3" "1–3" "1 à 3"-> intégrale tomes 1 à 3
     *  - "1,2,3" "1/2/3"    -> intégrale tomes 1 à 3
     *  - "6.1" "6.2"        -> tomes découpés : tome 6, partie 1 / 2
     *  - "Int. 2" "int"     -> intégrale (numéro d'intégrale)
     *  - "A" "-"            -> pas de numéro, null
     */
    fun parse(raw: String): ParsedVolume? {
        var s = raw.trim().lowercase()
            .replace("–", "-").replace("—", "-")
            .replace("à", "-").replace(" a ", "-")
            .replace(" ", "")
        if (s.isEmpty() || s == "-") return null

        // Intégrales : "int", "int.3", "intégrale2"
        Regex("^int(?:é|e)grale?\\.?(\\d{0,3})$").matchEntire(s)?.let { m ->
            val n = m.groupValues[1].toIntOrNull() ?: 1
            return ParsedVolume(n, n, "")
        }

        // Découpage de tome : "6.1" ou "6.10" -> tome 6, partie 1
        Regex("^(\\d{1,4})\\.(\\d{1,3})$").matchEntire(s)?.let { m ->
            return ParsedVolume(m.groupValues[1].toInt(), null, m.groupValues[2])
        }

        Regex("^(\\d{1,4})-(\\d{1,4})$").matchEntire(s)?.let { m ->
            val a = m.groupValues[1].toInt()
            val b = m.groupValues[2].toInt()
            return ParsedVolume(minOf(a, b), maxOf(a, b), "")
        }

        // "1,2,3" ou "1/2/3" : plusieurs tomes séparés (hors décimales)
        if (s.length > 1 && s.first().isDigit() && Regex("^[\\d,/]+$").matches(s)) {
            val nums = s.split(Regex("[,/]")).mapNotNull { it.toIntOrNull() }
            if (nums.size > 1) return ParsedVolume(nums.min(), nums.max(), "")
        }

        val n = s.toIntOrNull() ?: return null
        return ParsedVolume(n, null, "")
    }

    /** Représentation texte : "3", "1-3" ou "6.1" */
    fun format(volumeNumber: Int?, volumeEnd: Int?, suffix: String): String? = when {
        volumeNumber == null -> null
        volumeEnd != null && volumeEnd != volumeNumber -> "$volumeNumber-$volumeEnd"
        suffix.isNotBlank() -> "$volumeNumber.$suffix"
        else -> volumeNumber.toString()
    }
}
