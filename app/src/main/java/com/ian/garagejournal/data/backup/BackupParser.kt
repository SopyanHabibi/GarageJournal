package com.ian.garagejournal.data.backup

import com.ian.garagejournal.data.local.FuelLog
import com.ian.garagejournal.data.local.Reminder
import com.ian.garagejournal.data.local.ServiceLog
import com.ian.garagejournal.data.local.Vehicle

class BackupFormatException(message: String) : Exception(message)

/** Isi teks berkas-berkas di dalam ZIP; null berarti berkasnya tidak ada. */
class BackupFiles(
    val manifest: String?,
    val vehicles: String?,
    val serviceLogs: String?,
    val fuelLogs: String?,
    val reminders: String?
)

/** Data hasil baca; id dan vehicleId masih id milik backup. */
class ParsedBackup(
    val vehicles: List<Vehicle>,
    val serviceLogs: List<ServiceLog>,
    val fuelLogs: List<FuelLog>,
    val reminders: List<Reminder>
)

object BackupParser {

    fun parse(files: BackupFiles): ParsedBackup {
        checkManifest(files.manifest)
        val vehicleText = files.vehicles
            ?: throw BackupFormatException("vehicles.csv tidak ada di dalam backup")

        val vehicles = parseVehicles(vehicleText)
        val vehicleIds = vehicles.map { it.id }.toSet()

        return ParsedBackup(
            vehicles = vehicles,
            serviceLogs = files.serviceLogs?.let { parseServiceLogs(it, vehicleIds) } ?: emptyList(),
            fuelLogs = files.fuelLogs?.let { parseFuelLogs(it, vehicleIds) } ?: emptyList(),
            reminders = files.reminders?.let { parseReminders(it, vehicleIds) } ?: emptyList()
        )
    }

    private fun checkManifest(text: String?) {
        if (text == null) throw BackupFormatException("Bukan backup Garage Journal (manifest.txt tidak ada)")
        val map = text.lines().mapNotNull { line ->
            val idx = line.indexOf('=')
            if (idx <= 0) null else line.substring(0, idx).trim() to line.substring(idx + 1).trim()
        }.toMap()

        if (map["app"] != "Garage Journal") throw BackupFormatException("Bukan backup Garage Journal")
        val format = map["format"]?.toIntOrNull()
            ?: throw BackupFormatException("Versi format backup tidak terbaca")
        if (format > BACKUP_FORMAT_VERSION) {
            throw BackupFormatException("Backup dibuat oleh versi app yang lebih baru (format $format). Perbarui app dulu.")
        }
        if (format < 1) throw BackupFormatException("Versi format backup tidak dikenal")
    }

    private fun parseVehicles(text: String): List<Vehicle> {
        val seen = mutableSetOf<Long>()
        return Csv.records(text).mapIndexed { i, rec ->
            val r = RecordReader("vehicles.csv", i + 1, rec)
            val id = r.long("id", min = 1)
            r.requireUnique(seen, id)
            Vehicle(
                id = id,
                name = r.text("name"),
                brand = r.text("brand"),
                model = r.text("model"),
                year = r.int("year", min = 1900, max = 2200),
                plateNumber = r.text("plateNumber"),
                currentOdometer = r.int("currentOdometer"),
                createdAt = r.long("createdAt"),
                odometerUpdatedAt = r.long("odometerUpdatedAt")
            )
        }
    }

    private fun parseServiceLogs(text: String, vehicleIds: Set<Long>): List<ServiceLog> {
        val seen = mutableSetOf<Long>()
        return Csv.records(text).mapIndexed { i, rec ->
            val r = RecordReader("service_logs.csv", i + 1, rec)
            val id = r.long("id", min = 1)
            r.requireUnique(seen, id)
            val vehicleId = r.long("vehicleId", min = 1)
            r.requireVehicle(vehicleId, vehicleIds)
            ServiceLog(
                id = id,
                vehicleId = vehicleId,
                date = r.long("date"),
                odometer = r.int("odometer"),
                title = r.text("title"),
                description = r.text("description", allowBlank = true),
                cost = r.long("cost"),
                receiptPath = r.optionalText("receiptFile")
            )
        }
    }

    private fun parseFuelLogs(text: String, vehicleIds: Set<Long>): List<FuelLog> {
        val seen = mutableSetOf<Long>()
        return Csv.records(text).mapIndexed { i, rec ->
            val r = RecordReader("fuel_logs.csv", i + 1, rec)
            val id = r.long("id", min = 1)
            r.requireUnique(seen, id)
            val vehicleId = r.long("vehicleId", min = 1)
            r.requireVehicle(vehicleId, vehicleIds)
            FuelLog(
                id = id,
                vehicleId = vehicleId,
                date = r.long("date"),
                odometer = r.int("odometer"),
                liters = r.positiveDouble("liters"),
                pricePerLiter = r.long("pricePerLiter"),
                totalCost = r.long("totalCost"),
                isFullTank = r.boolean("isFullTank"),
                kmPerLiter = null // dihitung ulang saat impor
            )
        }
    }

    private fun parseReminders(text: String, vehicleIds: Set<Long>): List<Reminder> {
        val seen = mutableSetOf<Long>()
        return Csv.records(text).mapIndexed { i, rec ->
            val r = RecordReader("reminders.csv", i + 1, rec)
            val id = r.long("id", min = 1)
            r.requireUnique(seen, id)
            val vehicleId = r.long("vehicleId", min = 1)
            r.requireVehicle(vehicleId, vehicleIds)

            val km = r.optionalInt("intervalKm", min = 1)
            val months = r.optionalInt("intervalMonths", min = 1, max = 120)
            if (km == null && months == null) r.fail("isi intervalKm, intervalMonths, atau keduanya")

            Reminder(
                id = id,
                vehicleId = vehicleId,
                name = r.text("name"),
                intervalKm = km,
                intervalMonths = months,
                lastDoneOdometer = r.int("lastDoneOdometer"),
                lastDoneDate = r.long("lastDoneDate")
            )
        }
    }
}

/** Membaca satu baris data; setiap kegagalan menyebut berkas, nomor data, dan kolom. */
private class RecordReader(
    private val file: String,
    private val number: Int,
    private val values: Map<String, String>
) {
    fun fail(message: String): Nothing =
        throw BackupFormatException("$file, data ke-$number: $message")

    private fun raw(column: String): String = values[column] ?: fail("kolom '$column' tidak ada")

    fun text(column: String, allowBlank: Boolean = false): String {
        val v = raw(column)
        if (!allowBlank && v.isBlank()) fail("kolom '$column' kosong")
        return v
    }

    fun optionalText(column: String): String? = values[column]?.takeIf { it.isNotBlank() }

    fun long(column: String, min: Long = 0): Long {
        val v = raw(column).trim().toLongOrNull() ?: fail("kolom '$column' bukan angka yang valid")
        if (v < min) fail("kolom '$column' tidak boleh kurang dari $min")
        return v
    }

    fun int(column: String, min: Int = 0, max: Int = Int.MAX_VALUE): Int {
        val v = raw(column).trim().toIntOrNull() ?: fail("kolom '$column' bukan angka yang valid")
        if (v < min || v > max) fail("kolom '$column' di luar batas yang wajar")
        return v
    }

    fun optionalInt(column: String, min: Int, max: Int = Int.MAX_VALUE): Int? {
        val v = values[column]?.trim().orEmpty()
        if (v.isEmpty()) return null
        val n = v.toIntOrNull() ?: fail("kolom '$column' bukan angka yang valid")
        if (n < min || n > max) fail("kolom '$column' di luar batas yang wajar")
        return n
    }

    fun positiveDouble(column: String): Double {
        val v = raw(column).trim().toDoubleOrNull()
        if (v == null || !v.isFinite() || v <= 0.0) fail("kolom '$column' harus angka lebih dari 0")
        return v
    }

    fun boolean(column: String): Boolean = when (raw(column).trim().lowercase()) {
        "true", "1" -> true
        "false", "0" -> false
        else -> fail("kolom '$column' harus true atau false")
    }

    fun requireVehicle(vehicleId: Long, known: Set<Long>) {
        if (vehicleId !in known) fail("vehicleId $vehicleId tidak ada di vehicles.csv")
    }

    fun requireUnique(seen: MutableSet<Long>, id: Long) {
        if (!seen.add(id)) fail("id $id ganda")
    }
}