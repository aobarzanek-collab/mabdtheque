package com.example.mabdtheque.data

object VolumeParser {

    /**
     * Parse un champ "tome" souple. Formats reconnus :
     *  - "3"                -> tome 3 seul
     *  - "1-3" "1–3" "1 à 3"-> intégrale tomes 1 à 3
     *  - "1,2,3" "1/2/3"    -> intégrale tomes 1 à 3
     *  - "Int. 2" "int" "intégrale" -> intégrale (numéro d'intégrale dans les notes)
     *  - "1.1" "1.2"        -> hors-série après le tome 1 (1.1 -> tome 1, suffixe .1)
     *  - "A" "-"            -> pas de numéro (one-shot), null
     * Retourne (premier tome, dernier tome ou null si tome unique).
     */
    fun parse(raw: String): Pair<Int, Int?>? {
        var s = raw.trim().lowercase()
            .replace("–", "-").replace("—", "-")
            .replace("à", "-").replace(" a ", "-")
            .replace(" ", "")
        if (s.isEmpty() || s == "-") return null

        // Intégrales : "int", "int.3", "intégrale2" -> numéro d'intégrale seulement
        Regex("^int(?:é|e)grale?\\.?(\\d{0,3})$").matchEntire(s)?.let { m ->
            val n = m.groupValues[1].toIntOrNull()
            return n to n  // intégrale N couvre le tome N (numérotation approximative)
        }

        Regex("^(\\d{1,4})-(\\d{1,4})$").matchEntire(s)?.let { m ->
            val a = m.groupValues[1].toInt()
            val b = m.groupValues[2].toInt()
            return minOf(a, b) to maxOf(a, b)
        }

        // "1,2,3" ou "1/2/3"
        if (s.length > 1 && s.first().isDigit() && Regex("^[\\d,/.]+$").matches(s)) {
            val nums = s.split(Regex("[,/.]")).mapNotNull { it.toIntOrNull() }
            if (nums.size > 1) return minOf(*nums.toTypedArray()) to
                maxOf(*nums.toTypedArray())
        }

        // "1.1", "1.2" : hors-série du tome 1 -> traité comme tome 1
        Regex("^(\\d{1,4})\\.(\\d{1,3})$").matchEntire(s)?.let { m ->
            return m.groupValues[1].toInt() to null
        }

        val n = s.toIntOrNull() ?: return null
        return n to null
    }

    /** Représentation texte : "3" ou "1-3" */
    fun format(volumeNumber: Int?, volumeEnd: Int?): String? = when {
        volumeNumber == null -> null
        volumeEnd != null && volumeEnd != volumeNumber -> "$volumeNumber-$volumeEnd"
        else -> volumeNumber.toString()
    }
}
