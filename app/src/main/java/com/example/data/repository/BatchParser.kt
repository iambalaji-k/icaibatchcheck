package com.example.data.repository

import com.example.data.model.BatchInfo
import com.example.data.model.DropdownOption
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.util.Locale

/**
 * Pure, side-effect-free HTML parsing helpers extracted from the scraper so they
 * can be unit tested against golden HTML fixtures.
 */
object BatchParser {

    fun buildBatchId(regionText: String, pouText: String, courseText: String, batchName: String): String =
        "${regionText}_${pouText}_${courseText}_${batchName}".lowercase(Locale.ROOT)

    fun parseDropdownOptions(select: Element?): List<DropdownOption> {
        val options = mutableListOf<DropdownOption>()
        select?.select("option")?.forEach { opt ->
            val valAttr = opt.attr("value").trim()
            val text = opt.text().trim()
            if (valAttr.isNotEmpty() && valAttr != "0" && !text.contains("Select", ignoreCase = true)) {
                options.add(DropdownOption(valAttr, text))
            }
        }
        return options
    }

    fun parseSeatCount(cellText: String): Int? =
        cellText.trim().replace(",", "").toIntOrNull()

    /**
     * Word tokens (2+ chars, alphanumeric) of a course label. Token equality —
     * not substring `contains` — prevents "ICITSS" from matching "AICITSS"
     * (the latter has no ICITSS word token) while still tolerating minor
     * portal label variations like "AICITSS - Adv ITT".
     */
    fun courseTokens(text: String): Set<String> =
        text.lowercase(Locale.ROOT).split(Regex("[^a-z0-9]+")).filter { it.length >= 2 }.toSet()

    fun courseMatches(batchCourse: String, targetCourse: String): Boolean {
        if (batchCourse.isBlank() || targetCourse.isBlank()) return false
        val batchTokens = courseTokens(batchCourse)
        return courseTokens(targetCourse).any { it in batchTokens }
    }

    private val PURE_NUMBER = Regex("[0-9]+")

    /**
     * Parses batch grid rows, skipping any header row (rows whose first cell is a
     * `<th>`, or a caller-supplied first index). Expected columns:
     * Batch Name | Available Seats | [Total Seats] | Start Date | End Date | Timings | Venue | Fee
     * (the Total Seats column is optional; older portal layouts omit it).
     */
    fun parseBatchRows(
        rows: Elements,
        regionText: String,
        pouText: String,
        courseText: String
    ): List<BatchInfo> {
        val batches = mutableListOf<BatchInfo>()
        for (i in 0 until rows.size) {
            val cells = rows[i].select("td")
            if (cells.size < 2) continue
            // Header rows surface inside tbody after Jsoup normalisation; skip them.
            if (rows[i].selectFirst("th") != null) continue
            if (i == 0 && parseSeatCount(cells[1].text()) == null) continue

            val batchName = cells[0].text().trim()
            val availSeats = parseSeatCount(cells[1].text()) ?: 0

            // Detect whether a Total Seats column is present: a pure-number cell after
            // available seats that is >= the available count. (Dates never match.)
            var totalSeats = 0
            var dateOffset = 2
            if (cells.size >= 3) {
                val totalCell = cells[2].text().trim().replace(",", "")
                val maybeTotal = totalCell.toIntOrNull()
                if (maybeTotal != null && totalCell.matches(PURE_NUMBER) && maybeTotal >= availSeats) {
                    totalSeats = maybeTotal
                    dateOffset = 3
                }
            }

            val startDate = cellText(cells, dateOffset)
            val endDate = cellText(cells, dateOffset + 1)
            val dates = when {
                startDate.isNotBlank() && endDate.isNotBlank() -> "$startDate to $endDate"
                startDate.isNotBlank() -> startDate
                else -> endDate
            }
            val timings = cellText(cells, dateOffset + 2)
            val venue = cellText(cells, dateOffset + 3)
            val fee = cellText(cells, dateOffset + 4)

            val knownCapacity = totalSeats > 0
            val effectiveTotal = if (knownCapacity) totalSeats else availSeats

            batches.add(
                BatchInfo(
                    batchName = batchName,
                    totalSeats = effectiveTotal,
                    availableSeats = availSeats,
                    knownCapacity = knownCapacity,
                    dates = dates,
                    timings = timings,
                    venue = venue,
                    fee = fee,
                    statusText = if (availSeats > 0) "$availSeats seat(s) available!" else "Seats Full",
                    regionName = regionText,
                    pouName = pouText,
                    courseName = courseText
                )
            )
        }
        return batches
    }

    private fun cellText(cells: org.jsoup.select.Elements, index: Int): String =
        if (cells.size > index) cells[index].text().trim() else ""
}
