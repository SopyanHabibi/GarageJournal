package com.ian.garagejournal.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTest {

    @Test
    fun `teks biasa tidak diberi tanda kutip`() = assertEquals("Ganti oli", Csv.escape("Ganti oli"))

    @Test
    fun `koma membuat field dikutip`() = assertEquals("\"a,b\"", Csv.escape("a,b"))

    @Test
    fun `tanda kutip digandakan`() = assertEquals("\"say \"\"hi\"\"\"", Csv.escape("say \"hi\""))

    @Test
    fun `baris baru membuat field dikutip`() = assertEquals("\"a\nb\"", Csv.escape("a\nb"))

    @Test
    fun `baris diakhiri CRLF dan null menjadi kosong`() =
        assertEquals("1,,x\r\n", Csv.row(listOf(1, null, "x")))
}