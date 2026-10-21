package com.ian.garagejournal.domain

fun ReminderStatus.summaryText(): String {
    val parts = mutableListOf<String>()
    kmRemaining?.let {
        parts += when {
            it > 0 -> "${"%,d".format(it)} km lagi"
            it == 0 -> "tepat di batas km"
            else -> "lewat ${"%,d".format(-it)} km"
        }
    }
    daysRemaining?.let {
        parts += when {
            it > 0 -> "$it hari lagi"
            it == 0 -> "jatuh tempo hari ini"
            else -> "lewat ${-it} hari"
        }
    }
    return parts.joinToString(" • ")
}