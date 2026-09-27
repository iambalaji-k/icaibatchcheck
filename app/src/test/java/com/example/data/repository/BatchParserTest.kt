package com.example.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.jsoup.Jsoup

class BatchParserTest {

    private val html = """
        <html><body>
        <div id="upd_grid">
          <table>
            <thead><tr><th>Batch</th><th>Seats</th></tr></thead>
            <tbody>
              <tr><td>Chennai AICITSS BATCH #101</td><td>0</td><td>45</td><td>01-Aug-2026</td><td>15-Aug-2026</td><td>09:30 AM - 04:30 PM</td><td>ICAI Bhawan, Chennai</td><td>7000</td></tr>
              <tr><td>Chennai AICITSS BATCH #102</td><td>1,234</td><td>2000</td><td>16-Aug-2026</td><td>30-Aug-2026</td><td>10:00 AM - 05:00 PM</td><td>IT Center, Chennai</td><td>7000</td></tr>
              <tr><td>Chennai AICITSS BATCH #103</td><td>N.A.</td><td></td><td>01-Sep-2026</td><td>05-Sep-2026</td><td>Full Day</td><td>T Nagar</td><td>7000</td></tr>
            </tbody>
          </table>
        </div>
        </body></html>
    """.trimIndent()

    private fun rows() = Jsoup.parse(html).select("#upd_grid table tbody tr")

    @Test
    fun `parses batch name and available seats`() {
        val batches = BatchParser.parseBatchRows(rows(), "Southern", "Chennai", "AICITSS")
        assertEquals(3, batches.size)
        assertEquals("Chennai AICITSS BATCH #101", batches[0].batchName)
        assertEquals(0, batches[0].availableSeats)
        assertFalse(batches[0].isOpen)
    }

    @Test
    fun `parses thousands separators in seat counts`() {
        val batches = BatchParser.parseBatchRows(rows(), "Southern", "Chennai", "AICITSS")
        assertEquals(1234, batches[1].availableSeats)
        assertEquals(2000, batches[1].totalSeats)
        assertTrue(batches[1].isOpen)
    }

    @Test
    fun `parses date range timings venue and fee`() {
        val batches = BatchParser.parseBatchRows(rows(), "Southern", "Chennai", "AICITSS")
        val b = batches[1]
        assertEquals("16-Aug-2026 to 30-Aug-2026", b.dates)
        assertEquals("10:00 AM - 05:00 PM", b.timings)
        assertEquals("IT Center, Chennai", b.venue)
        assertEquals("7000", b.fee)
    }

    @Test
    fun `non numeric seat cell is treated as zero not a crash`() {
        val batches = BatchParser.parseBatchRows(rows(), "Southern", "Chennai", "AICITSS")
        assertEquals(0, batches[2].availableSeats)
    }

    @Test
    fun `unknown total seats falls back to available seats instead of a fabricated capacity`() {
        val htmlNoTotal = """
            <html><body><div id="upd_grid"><table>
              <tr><th>Batch</th><th>Seats</th><th>Start</th><th>End</th></tr>
              <tr><td>B1</td><td>3</td><td>01-Sep</td><td>05-Sep</td></tr>
            </table></div></body></html>
        """.trimIndent()
        val rows = Jsoup.parse(htmlNoTotal).select("#upd_grid table tbody tr")
        val batches = BatchParser.parseBatchRows(rows, "r", "p", "c")
        assertEquals(3, batches[0].totalSeats)
    }

    @Test
    fun `parses dropdown options skipping placeholder entries`() {
        val doc = Jsoup.parse(
            """
            <select id="ddl_reg">
              <option value="0">--Select Region--</option>
              <option value="1"> Central </option>
              <option value="2">Eastern</option>
            </select>
            """.trimIndent()
        )
        val options = BatchParser.parseDropdownOptions(doc.selectFirst("select"))
        assertEquals(listOf("Central", "Eastern"), options.map { it.text })
        assertEquals(listOf("1", "2"), options.map { it.value })
    }

    @Test
    fun `seat count parsing is strict`() {
        assertEquals(40, BatchParser.parseSeatCount(" 40 "))
        assertEquals(1234, BatchParser.parseSeatCount("1,234"))
        assertNull(BatchParser.parseSeatCount("N.A."))
    }

    @Test
    fun `batch ids are deterministic and target-scoped`() {
        val id = BatchParser.buildBatchId("Southern", "Chennai", "AICITSS", "BATCH #102")
        assertEquals("southern_chennai_aicitss_batch #102", id)
    }

    @Test
    fun `missing total column does not claim a fake 100 percent capacity`() {
        val htmlNoTotal = """
            <html><body><div id="upd_grid"><table>
              <tr><th>Batch</th><th>Seats</th><th>Start</th></tr>
              <tr><td>B1</td><td>1</td><td>01-Sep</td></tr>
            </table></div></body></html>
        """.trimIndent()
        val rows = Jsoup.parse(htmlNoTotal).select("#upd_grid table tbody tr")
        val b = BatchParser.parseBatchRows(rows, "r", "p", "c")[0]
        // Capacity is unknown, so the UI must suppress the percentage bar rather
        // than render "1 of 1 (100%)".
        assertFalse(b.knownCapacity)
    }

    @Test
    fun `present total column marks capacity known`() {
        val batches = BatchParser.parseBatchRows(rows(), "Southern", "Chennai", "AICITSS")
        // Fixture row #102 has an explicit total of 2000.
        assertTrue(batches[1].knownCapacity)
        assertEquals(2000, batches[1].totalSeats)
    }

    @Test
    fun `ICITSS target does not match AICITSS batches and vice versa`() {
        // Word-token matching, not substring: "AICITSS".contains("ICITSS") would
        // wrongly be true under a naive substring check.
        assertFalse(BatchParser.courseMatches("AICITSS - Advanced Information Technology", "ICITSS"))
        assertFalse(BatchParser.courseMatches("ICITSS - Orientation Course", "AICITSS"))
        assertTrue(BatchParser.courseMatches("ICITSS - Information Technology", "ICITSS"))
        assertTrue(
            BatchParser.courseMatches(
                "AICITSS - Advanced Information Technology",
                "AICITSS - Advanced Information Technology (Adv ITT)"
            )
        )
    }

    @Test
    fun `empty course name never matches everything`() {
        assertFalse(BatchParser.courseMatches("", "ICITSS"))
        assertFalse(BatchParser.courseMatches("ICITSS", ""))
    }
}
