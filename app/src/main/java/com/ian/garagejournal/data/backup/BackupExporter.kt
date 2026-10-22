package com.ian.garagejournal.data.backup

import androidx.room.withTransaction
import com.ian.garagejournal.data.local.FuelLog
import com.ian.garagejournal.data.local.GarageDatabase
import com.ian.garagejournal.data.local.ReceiptStorage
import com.ian.garagejournal.data.local.Reminder
import com.ian.garagejournal.data.local.ServiceLog
import com.ian.garagejournal.data.local.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

const val BACKUP_FORMAT_VERSION = 1

data class ExportSummary(
    val vehicles: Int,
    val serviceLogs: Int,
    val fuelLogs: Int,
    val reminders: Int,
    val receipts: Int,
    val missingReceipts: Int
)

fun defaultBackupFileName(date: LocalDate = LocalDate.now()): String = "garage-journal-backup-$date.zip"

class BackupExporter(
    private val database: GarageDatabase,
    private val receiptStorage: ReceiptStorage
) {
    private class Snapshot(
        val vehicles: List<Vehicle>,
        val serviceLogs: List<ServiceLog>,
        val fuelLogs: List<FuelLog>,
        val reminders: List<Reminder>
    )

    suspend fun export(output: OutputStream, nowMillis: Long = System.currentTimeMillis()): ExportSummary {
        // Satu transaksi: keempat tabel dibaca dari kondisi yang sama
        val snapshot = database.withTransaction {
            Snapshot(
                database.vehicleDao().getAll(),
                database.serviceLogDao().getAll(),
                database.fuelLogDao().getAll(),
                database.reminderDao().getAll()
            )
        }
        return withContext(Dispatchers.IO) { write(snapshot, output, nowMillis) }
    }

    private fun write(s: Snapshot, output: OutputStream, nowMillis: Long): ExportSummary {
        val names = s.vehicles.associate { it.id to it.name }
        var receipts = 0
        var missing = 0

        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            zip.putText("manifest.txt", manifest(s, nowMillis))
            zip.putText("vehicles.csv", vehiclesCsv(s.vehicles))
            zip.putText("service_logs.csv", serviceLogsCsv(s.serviceLogs, names))
            zip.putText("fuel_logs.csv", fuelLogsCsv(s.fuelLogs, names))
            zip.putText("reminders.csv", remindersCsv(s.reminders, names))

            s.serviceLogs.mapNotNull { it.receiptPath }.distinct().forEach { name ->
                val file = receiptStorage.resolve(name)
                if (file.exists()) {
                    zip.putNextEntry(ZipEntry("receipts/$name"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    receipts++
                } else {
                    missing++
                }
            }
        }

        return ExportSummary(
            vehicles = s.vehicles.size,
            serviceLogs = s.serviceLogs.size,
            fuelLogs = s.fuelLogs.size,
            reminders = s.reminders.size,
            receipts = receipts,
            missingReceipts = missing
        )
    }

    private fun ZipOutputStream.putText(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    // Tanggal mesin (epoch millis) dipakai untuk impor; tanggal terbaca (yyyy-MM-dd) untuk dilihat di spreadsheet.
    private fun day(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    private fun manifest(s: Snapshot, nowMillis: Long) = buildString {
        appendLine("app=Garage Journal")
        appendLine("format=$BACKUP_FORMAT_VERSION")
        appendLine("exportedAt=${Instant.ofEpochMilli(nowMillis)}")
        appendLine("vehicles=${s.vehicles.size}")
        appendLine("serviceLogs=${s.serviceLogs.size}")
        appendLine("fuelLogs=${s.fuelLogs.size}")
        appendLine("reminders=${s.reminders.size}")
    }

    private fun vehiclesCsv(list: List<Vehicle>) = buildString {
        append(
            Csv.row(
                listOf(
                    "id", "name", "brand", "model", "year", "plateNumber",
                    "currentOdometer", "createdAt", "odometerUpdatedAt"
                )
            )
        )
        list.forEach { v ->
            append(
                Csv.row(
                    listOf(
                        v.id, v.name, v.brand, v.model, v.year, v.plateNumber,
                        v.currentOdometer, v.createdAt, v.odometerUpdatedAt
                    )
                )
            )
        }
    }

    private fun serviceLogsCsv(list: List<ServiceLog>, names: Map<Long, String>) = buildString {
        append(
            Csv.row(
                listOf(
                    "id", "vehicleId", "vehicle", "date", "dateReadable", "odometer",
                    "title", "description", "cost", "receiptFile"
                )
            )
        )
        list.forEach { l ->
            append(
                Csv.row(
                    listOf(
                        l.id, l.vehicleId, names[l.vehicleId], l.date, day(l.date), l.odometer,
                        l.title, l.description, l.cost, l.receiptPath
                    )
                )
            )
        }
    }

    private fun fuelLogsCsv(list: List<FuelLog>, names: Map<Long, String>) = buildString {
        append(
            Csv.row(
                listOf(
                    "id", "vehicleId", "vehicle", "date", "dateReadable", "odometer", "liters",
                    "pricePerLiter", "totalCost", "isFullTank", "kmPerLiter"
                )
            )
        )
        list.forEach { l ->
            append(
                Csv.row(
                    listOf(
                        l.id, l.vehicleId, names[l.vehicleId], l.date, day(l.date), l.odometer, l.liters,
                        l.pricePerLiter, l.totalCost, l.isFullTank, l.kmPerLiter
                    )
                )
            )
        }
    }

    private fun remindersCsv(list: List<Reminder>, names: Map<Long, String>) = buildString {
        append(
            Csv.row(
                listOf(
                    "id", "vehicleId", "vehicle", "name", "intervalKm", "intervalMonths",
                    "lastDoneOdometer", "lastDoneDate", "lastDoneDateReadable"
                )
            )
        )
        list.forEach { r ->
            append(
                Csv.row(
                    listOf(
                        r.id, r.vehicleId, names[r.vehicleId], r.name, r.intervalKm, r.intervalMonths,
                        r.lastDoneOdometer, r.lastDoneDate, day(r.lastDoneDate)
                    )
                )
            )
        }
    }
}