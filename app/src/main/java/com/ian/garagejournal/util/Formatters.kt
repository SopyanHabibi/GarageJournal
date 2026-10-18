package com.ian.garagejournal.util

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val localeId: Locale = Locale.forLanguageTag("id-ID")
private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", localeId)

fun formatRupiah(value: Long): String =
    "Rp " + NumberFormat.getInstance(localeId).format(value)

fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)

// DatePicker Material3 bekerja dalam "tengah malam UTC". Dua fungsi ini
// menerjemahkannya ke/dari awal hari di zona waktu lokal, supaya tanggal tidak geser sehari.
fun pickerMillisToLocalMillis(utcMillis: Long): Long =
    Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun localMillisToPickerMillis(localMillis: Long): Long =
    Instant.ofEpochMilli(localMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()