package com.ian.garagejournal.util

import com.ian.garagejournal.data.local.Vehicle

/**
 * Nama tampilan kendaraan. Kalau nickname kosong, fallback ke "Merek Model".
 */
fun Vehicle.displayName(): String =
    name.ifBlank { "$brand $model" }