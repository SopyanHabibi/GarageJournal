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
}