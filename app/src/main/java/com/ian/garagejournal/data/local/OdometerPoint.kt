package com.ian.garagejournal.data.local

/** Hasil query ringan: tanggal dan odometer sebuah catatan, untuk memperkirakan pemakaian harian. */
data class OdometerPoint(val date: Long, val odometer: Int)