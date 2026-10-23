package com.ian.garagejournal.data.backup

object Csv {
    /** Aturan CSV standar: field yang berisi koma, kutip, atau baris baru dibungkus kutip; kutip digandakan. */
    fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }

    /** Satu baris CSV. null menjadi kosong, baris diakhiri CRLF. */
    fun row(fields: List<Any?>): String =
        fields.joinToString(",") { escape(it?.toString() ?: "") } + "\r\n"

    /** Membaca teks CSV menjadi baris berisi field. Mendukung koma, kutip, dan baris baru di dalam tanda kutip. */
    fun parse(text: String): List<List<String>> {
        val s = text.removePrefix("\uFEFF") // BOM, kalau berkas pernah disimpan ulang oleh spreadsheet
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < s.length) {
            val c = s[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < s.length && s[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        row.add(field.toString())
                        field.setLength(0)
                    }
                    '\n' -> {
                        row.add(field.toString())
                        field.setLength(0)
                        rows.add(row)
                        row = mutableListOf()
                    }
                    '\r' -> Unit // CRLF: yang menutup baris adalah \n
                    else -> field.append(c)
                }
            }
            i++
        }

        if (inQuotes) throw BackupFormatException("CSV rusak: ada tanda kutip yang tidak ditutup")
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows.filterNot { it.size == 1 && it[0].isEmpty() } // abaikan baris kosong
    }

    /** Baris pertama jadi nama kolom. Urutan kolom dan kolom tambahan tidak masalah. */
    fun records(text: String): List<Map<String, String>> {
        val rows = parse(text)
        if (rows.isEmpty()) return emptyList()
        val header = rows.first().map { it.trim() }
        return rows.drop(1).map { cells ->
            header.indices.associate { i -> header[i] to (cells.getOrNull(i) ?: "") }
        }
    }
}