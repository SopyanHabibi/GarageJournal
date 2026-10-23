package com.ian.garagejournal.data.backup

import androidx.room.withTransaction
import com.ian.garagejournal.data.local.GarageDatabase
import com.ian.garagejournal.data.local.ReceiptStorage
import com.ian.garagejournal.domain.FuelCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

data class ImportSummary(
    val vehicles: Int,
    val skippedVehicles: Int,
    val serviceLogs: Int,
    val fuelLogs: Int,
    val reminders: Int,
    val receipts: Int,
    val missingReceipts: Int
)

class BackupImporter(
    private val database: GarageDatabase,
    private val receiptStorage: ReceiptStorage
) {
    suspend fun import(input: InputStream): ImportSummary = withContext(Dispatchers.IO) {
        val copied = mutableMapOf<String, String>() // nama di backup -> nama baru di penyimpanan app
        try {
            val files = try {
                readZip(input, copied)
            } catch (e: ZipException) {
                throw BackupFormatException("Berkas ZIP rusak atau tidak bisa dibaca")
            }
            val parsed = BackupParser.parse(files) // validasi penuh sebelum menyentuh database
            merge(parsed, copied)
        } catch (e: Throwable) {
            copied.values.forEach { receiptStorage.delete(it) } // batal: buang foto yang sempat disalin
            throw e
        }
    }

    private fun readZip(input: InputStream, copied: MutableMap<String, String>): BackupFiles {
        val texts = mutableMapOf<String, String>()
        var entryCount = 0

        ZipInputStream(BufferedInputStream(input)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (++entryCount > MAX_ENTRIES) {
                    throw BackupFormatException("Terlalu banyak berkas di dalam backup")
                }
                val name = entry.name
                if (!entry.isDirectory) {
                    when {
                        name in TEXT_FILES ->
                            texts[name] = zip.readCapped(MAX_TEXT_BYTES).toString(Charsets.UTF_8)

                        name.startsWith(RECEIPT_PREFIX) -> {
                            // Nama dari ZIP hanya dipakai sebagai kunci. Berkas disimpan dengan nama baru buatan kita.
                            val file = name.removePrefix(RECEIPT_PREFIX)
                            if (file.isNotEmpty() && '/' !in file && '\\' !in file && file !in copied) {
                                copied[file] = receiptStorage.importStream(LimitedInputStream(zip, MAX_RECEIPT_BYTES))
                            }
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        return BackupFiles(
            manifest = texts["manifest.txt"],
            vehicles = texts["vehicles.csv"],
            serviceLogs = texts["service_logs.csv"],
            fuelLogs = texts["fuel_logs.csv"],
            reminders = texts["reminders.csv"]
        )
    }

    private suspend fun merge(parsed: ParsedBackup, copied: Map<String, String>): ImportSummary {
        val now = System.currentTimeMillis()
        var vehicles = 0
        var skipped = 0
        var services = 0
        var fuels = 0
        var reminders = 0
        var missing = 0
        val usedReceipts = mutableSetOf<String>()

        // KM/L dihitung ulang per motor dengan rumus yang sama seperti di app
        val efficiencyByLog: Map<Long, Double> = parsed.fuelLogs
            .groupBy { it.vehicleId }.values
            .flatMap { FuelCalculator.efficiencies(it).entries }
            .associate { it.key to it.value }

        database.withTransaction {
            val vehicleDao = database.vehicleDao()
            val knownPlates = vehicleDao.getAll().map { plateKey(it.plateNumber) }.toMutableSet()
            val idMap = mutableMapOf<Long, Long>() // id di backup -> id baru

            for (v in parsed.vehicles) {
                if (!knownPlates.add(plateKey(v.plateNumber))) {
                    skipped++
                    continue
                }
                // odometerUpdatedAt = sekarang: restore dihitung sebagai interaksi, bukan "data basi"
                idMap[v.id] = vehicleDao.insert(
                    v.copy(id = 0, odometerUpdatedAt = now, staleNotifiedAt = 0, staleNotifyCount = 0)
                )
                vehicles++
            }

            for (log in parsed.serviceLogs) {
                val newVehicleId = idMap[log.vehicleId] ?: continue
                val backupName = log.receiptPath
                val newName = backupName?.let { copied[it] }
                if (backupName != null && newName == null) missing++
                if (newName != null) usedReceipts += newName
                database.serviceLogDao().insert(log.copy(id = 0, vehicleId = newVehicleId, receiptPath = newName))
                services++
            }

            for (log in parsed.fuelLogs) {
                val newVehicleId = idMap[log.vehicleId] ?: continue
                database.fuelLogDao().insert(
                    log.copy(id = 0, vehicleId = newVehicleId, kmPerLiter = efficiencyByLog[log.id])
                )
                fuels++
            }

            for (r in parsed.reminders) {
                val newVehicleId = idMap[r.vehicleId] ?: continue
                database.reminderDao().insert(r.copy(id = 0, vehicleId = newVehicleId))
                reminders++
            }
        }

        // Foto yang tidak terpakai (motornya dilewati atau tidak dirujuk) dibuang
        copied.values.filter { it !in usedReceipts }.forEach { receiptStorage.delete(it) }

        return ImportSummary(
            vehicles = vehicles,
            skippedVehicles = skipped,
            serviceLogs = services,
            fuelLogs = fuels,
            reminders = reminders,
            receipts = usedReceipts.size,
            missingReceipts = missing
        )
    }

    /** Plat dibandingkan tanpa spasi/tanda baca dan tanpa membedakan huruf besar-kecil. */
    private fun plateKey(plate: String): String =
        plate.filter { it.isLetterOrDigit() }.uppercase().ifEmpty { plate.trim().uppercase() }

    private fun InputStream.readCapped(maxBytes: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val n = read(buffer)
            if (n == -1) break
            total += n
            if (total > maxBytes) throw BackupFormatException("Berkas CSV di dalam backup terlalu besar")
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private companion object {
        const val RECEIPT_PREFIX = "receipts/"
        const val MAX_ENTRIES = 20_000
        const val MAX_TEXT_BYTES = 20 * 1024 * 1024
        const val MAX_RECEIPT_BYTES = 30L * 1024 * 1024
        val TEXT_FILES = setOf("manifest.txt", "vehicles.csv", "service_logs.csv", "fuel_logs.csv", "reminders.csv")
    }
}

/** Membatasi jumlah byte yang boleh dibaca dari satu entri ZIP (perlindungan dari berkas raksasa). */
private class LimitedInputStream(
    private val source: InputStream,
    private var remaining: Long
) : InputStream() {
    override fun read(): Int {
        val b = source.read()
        if (b != -1 && --remaining < 0) throw BackupFormatException("Foto nota di dalam backup terlalu besar")
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = source.read(b, off, len)
        if (n > 0) {
            remaining -= n
            if (remaining < 0) throw BackupFormatException("Foto nota di dalam backup terlalu besar")
        }
        return n
    }
}