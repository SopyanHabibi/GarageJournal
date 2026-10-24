package com.ian.garagejournal.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun `rupiah ringkas untuk label grafik`() {
        assertEquals("Rp 500", formatRupiahCompact(500))
        assertEquals("Rp 45 rb", formatRupiahCompact(45_000))
        assertEquals("Rp 1,5 jt", formatRupiahCompact(1_500_000))
        assertEquals("Rp 2 jt", formatRupiahCompact(2_000_000))
    }
}