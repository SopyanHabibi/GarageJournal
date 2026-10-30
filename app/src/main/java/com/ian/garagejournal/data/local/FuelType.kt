package com.ian.garagejournal.data.local

data class FuelType(
    val name: String,
    val pricePerLiter: Long
)

// Harga per Oktober 2026 — update sesuai harga terbaru jika berubah
val PERTAMINA_FUEL_TYPES = listOf(
    FuelType("Pertalite",        10_000),
    FuelType("Pertamax",         16_000),
    FuelType("Pertamax Turbo",   19_600),
    FuelType("Pertamax Green 95",19_000),
    FuelType("Dexlite",          23_700),
    FuelType("Pertamina Dex",    25_200),
    FuelType("Solar (Subsidi)",   6_800),
)