package com.ian.garagejournal.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CsvParseTest {

    @Test
    fun `baris sederhana dengan CRLF`() =
        assertEquals(listOf(listOf("a", "b", "c")), Csv.parse("a,b,c\r\n"))

    @Test
    fun `tanpa baris baru di akhir tetap terbaca`() =
        assertEquals(listOf(listOf("a", "b")), Csv.parse("a,b"))

    @Test
    fun `koma dan kutip ganda di dalam tanda kutip`() =
        assertEquals(listOf(listOf("x, y", "say \"hi\"")), Csv.parse("\"x, y\",\"say \"\"hi\"\"\"\r\n"))

    @Test
    fun `baris baru di dalam tanda kutip`() =
        assertEquals(listOf(listOf("a\nb", "c")), Csv.parse("\"a\nb\",c\n"))

    @Test
    fun `field kosong dipertahankan`() =
        assertEquals(listOf(listOf("1", "", "x")), Csv.parse("1,,x\r\n"))

    @Test
    fun `BOM diabaikan dan baris kosong dilewati`() =
        assertEquals(listOf(listOf("a"), listOf("b")), Csv.parse("\uFEFFa\n\nb\n"))

    @Test
    fun `hasil Csv row bisa dibaca kembali tanpa berubah`() {
        val fields = listOf("1", "Pakai oli, merek \"X\"\nDi bengkel A", "", "x")
        assertEquals(listOf(fields), Csv.parse(Csv.row(fields)))
    }

    @Test
    fun `records memetakan nama kolom ke nilai`() {
        val recs = Csv.records("name,id\r\nSi Hitam,1\r\nX,2\r\n")
        assertEquals(
            listOf(mapOf("name" to "Si Hitam", "id" to "1"), mapOf("name" to "X", "id" to "2")),
            recs
        )
    }

    @Test
    fun `tanda kutip yang tidak ditutup ditolak`() {
        assertThrows(BackupFormatException::class.java) { Csv.parse("\"abc") }
    }
}