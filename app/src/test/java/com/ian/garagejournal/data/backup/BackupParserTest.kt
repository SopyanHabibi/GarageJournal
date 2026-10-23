package com.ian.garagejournal.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MANIFEST = "app=Garage Journal\nformat=1\n"
private const val VEHICLES =
    "id,name,brand,model,year,plateNumber,currentOdometer,createdAt,odometerUpdatedAt\r\n" +
            "1,Si Hitam,Honda,Beat,2020,BK 1 XX,10000,0,0\r\n"
private const val SERVICE_HEADER =
    "id,vehicleId,vehicle,date,dateReadable,odometer,title,description,cost,receiptFile\r\n"
private const val FUEL_HEADER =
    "id,vehicleId,vehicle,date,dateReadable,odometer,liters,pricePerLiter,totalCost,isFullTank,kmPerLiter\r\n"
private const val REMINDER_HEADER =
    "id,vehicleId,vehicle,name,intervalKm,intervalMonths,lastDoneOdometer,lastDoneDate,lastDoneDateReadable\r\n"

class BackupParserTest {

    private fun files(
        manifest: String? = MANIFEST,
        vehicles: String? = VEHICLES,
        service: String? = null,
        fuel: String? = null,
        reminders: String? = null
    ) = BackupFiles(manifest, vehicles, service, fuel, reminders)

    private fun errorOf(block: () -> Unit): BackupFormatException =
        assertThrows(BackupFormatException::class.java) { block() }

    @Test
    fun `backup minimal dengan satu motor terbaca`() {
        val parsed = BackupParser.parse(files())
        val v = parsed.vehicles.single()
        assertEquals(1L, v.id)
        assertEquals("Si Hitam", v.name)
        assertEquals(10_000, v.currentOdometer)
        assertTrue(parsed.serviceLogs.isEmpty())
    }

    @Test
    fun `urutan kolom dan kolom tambahan tidak masalah`() {
        val vehicles =
            "plateNumber,id,name,brand,model,year,currentOdometer,createdAt,odometerUpdatedAt,extra\r\n" +
                    "BK 1 XX,1,Si Hitam,Honda,Beat,2020,10000,0,0,xyz\r\n"
        val v = BackupParser.parse(files(vehicles = vehicles)).vehicles.single()
        assertEquals("BK 1 XX", v.plateNumber)
        assertEquals("Si Hitam", v.name)
    }

    @Test
    fun `tanpa manifest ditolak`() {
        val e = errorOf { BackupParser.parse(files(manifest = null)) }
        assertTrue(e.message!!.contains("manifest"))
    }

    @Test
    fun `bukan backup Garage Journal ditolak`() {
        errorOf { BackupParser.parse(files(manifest = "app=Lain\nformat=1\n")) }
    }

    @Test
    fun `format yang lebih baru ditolak`() {
        val e = errorOf { BackupParser.parse(files(manifest = "app=Garage Journal\nformat=99\n")) }
        assertTrue(e.message!!.contains("lebih baru"))
    }

    @Test
    fun `vehicles csv wajib ada`() {
        errorOf { BackupParser.parse(files(vehicles = null)) }
    }

    @Test
    fun `id motor ganda ditolak`() {
        val dup = VEHICLES + "1,Lain,Yamaha,NMAX,2021,B 1 X,500,0,0\r\n"
        errorOf { BackupParser.parse(files(vehicles = dup)) }
    }

    @Test
    fun `angka tidak valid menyebut berkas nomor data dan kolom`() {
        val service = SERVICE_HEADER +
                "1,1,Si Hitam,0,1970-01-01,9500,Ganti oli,,50000,\r\n" +
                "2,1,Si Hitam,0,1970-01-01,abc,Ganti oli,,50000,\r\n"
        val e = errorOf { BackupParser.parse(files(service = service)) }
        assertTrue(e.message!!.contains("service_logs.csv"))
        assertTrue(e.message!!.contains("data ke-2"))
        assertTrue(e.message!!.contains("odometer"))
    }

    @Test
    fun `vehicleId yang tidak ada ditolak`() {
        val service = SERVICE_HEADER + "1,9,Hantu,0,1970-01-01,9500,Ganti oli,,50000,\r\n"
        val e = errorOf { BackupParser.parse(files(service = service)) }
        assertTrue(e.message!!.contains("vehicleId 9"))
    }

    @Test
    fun `BBM membaca boolean dan desimal`() {
        val fuel = FUEL_HEADER + "1,1,Si Hitam,0,1970-01-01,10000,3.5,10000,35000,true,\r\n"
        val log = BackupParser.parse(files(fuel = fuel)).fuelLogs.single()
        assertEquals(3.5, log.liters, 0.0)
        assertTrue(log.isFullTank)
    }

    @Test
    fun `liter nol ditolak`() {
        val fuel = FUEL_HEADER + "1,1,Si Hitam,0,1970-01-01,10000,0,10000,0,true,\r\n"
        errorOf { BackupParser.parse(files(fuel = fuel)) }
    }

    @Test
    fun `pengingat tanpa interval ditolak`() {
        val reminders = REMINDER_HEADER + "1,1,Si Hitam,Ganti oli,,,9500,0,1970-01-01\r\n"
        errorOf { BackupParser.parse(files(reminders = reminders)) }
    }

    @Test
    fun `pengingat dengan salah satu interval terbaca`() {
        val reminders = REMINDER_HEADER + "1,1,Si Hitam,Ganti oli,2000,,9500,0,1970-01-01\r\n"
        val r = BackupParser.parse(files(reminders = reminders)).reminders.single()
        assertEquals(2_000, r.intervalKm)
        assertEquals(null, r.intervalMonths)
    }
}