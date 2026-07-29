package com.example.data.repository

import com.example.data.model.BatchInfo
import com.example.data.model.DropdownOption
import com.example.data.model.ScraperResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

class IcaiScraperRepository {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .cookieJar(JavaNetCookieJar()) // maintain session cookies
        .build()

    private val BASE_URL = "https://www.icaionlineregistration.org/LaunchBatchDetail.aspx"
    private val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0"

    private fun extractHiddenFields(doc: Document): MutableMap<String, String> {
        val fields = mutableMapOf<String, String>()
        for (name in listOf("__VIEWSTATE", "__VIEWSTATEGENERATOR", "__EVENTVALIDATION", "__EVENTTARGET", "__EVENTARGUMENT")) {
            val element = doc.selectFirst("input[name=$name]")
            if (element != null) {
                fields[name] = element.attr("value")
            }
        }
        return fields
    }

    suspend fun fetchInitialRegions(): ScraperResult<List<DropdownOption>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(BASE_URL)
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ScraperResult.Error("HTTP Error ${response.code}")
                }
                val html = response.body?.string() ?: ""
                val doc = Jsoup.parse(html)
                val select = doc.selectFirst("select[id=ddl_reg]")
                val options = mutableListOf<DropdownOption>()

                select?.select("option")?.forEach { opt ->
                    val valAttr = opt.attr("value").trim()
                    val text = opt.text().trim()
                    if (valAttr.isNotEmpty() && valAttr != "0" && !text.contains("Select", ignoreCase = true)) {
                        options.add(DropdownOption(valAttr, text))
                    }
                }

                if (options.isEmpty()) {
                    options.addAll(getPreloadedRegions())
                }
                ScraperResult.Success(options)
            }
        } catch (e: Exception) {
            ScraperResult.Success(getPreloadedRegions())
        }
    }

    suspend fun checkBatches(
        regionValue: String,
        regionText: String,
        pouValue: String,
        pouText: String,
        courseValue: String,
        courseText: String,
        forceMock: Boolean = false
    ): ScraperResult<List<BatchInfo>> = withContext(Dispatchers.IO) {
        if (forceMock) {
            return@withContext ScraperResult.Success(getMockBatches(regionText, pouText, courseText))
        }

        try {
            // 1. GET initial page
            val getReq = Request.Builder()
                .url(BASE_URL)
                .header("User-Agent", USER_AGENT)
                .build()

            val initHtml = client.newCall(getReq).execute().use { resp ->
                if (!resp.isSuccessful) throw Exception("Initial GET failed with code ${resp.code}")
                resp.body?.string() ?: ""
            }

            var doc = Jsoup.parse(initHtml)
            var fields = extractHiddenFields(doc)

            // 2. Select Region -> Post
            fields["ddl_reg"] = regionValue
            fields["__EVENTTARGET"] = "ddl_reg"
            fields["__EVENTARGUMENT"] = ""
            val form1 = FormBody.Builder()
            fields.forEach { (k, v) -> form1.add(k, v) }

            val postReq1 = Request.Builder()
                .url(BASE_URL)
                .header("User-Agent", USER_AGENT)
                .post(form1.build())
                .build()

            val htmlStep2 = client.newCall(postReq1).execute().use { resp ->
                resp.body?.string() ?: ""
            }

            doc = Jsoup.parse(htmlStep2)
            fields = extractHiddenFields(doc)

            // Resolve POU value if not set or text matching
            var actualPouValue = pouValue
            val pouSelect = doc.selectFirst("select[id=ddlPou]")
            if (pouSelect != null) {
                for (opt in pouSelect.select("option")) {
                    if (opt.text().trim().equals(pouText.trim(), ignoreCase = true)) {
                        actualPouValue = opt.attr("value")
                        break
                    }
                }
            }

            fields["ddl_reg"] = regionValue
            fields["ddlPou"] = actualPouValue
            fields["__EVENTTARGET"] = "ddlPou"
            fields["__EVENTARGUMENT"] = ""

            // 3. Select POU -> Post
            val form2 = FormBody.Builder()
            fields.forEach { (k, v) -> form2.add(k, v) }

            val postReq2 = Request.Builder()
                .url(BASE_URL)
                .header("User-Agent", USER_AGENT)
                .post(form2.build())
                .build()

            val htmlStep3 = client.newCall(postReq2).execute().use { resp ->
                resp.body?.string() ?: ""
            }

            doc = Jsoup.parse(htmlStep3)
            fields = extractHiddenFields(doc)

            // Resolve Course value by dynamically matching courseText against <select id="ddl_course">
            var actualCourseValue = courseValue
            val courseSelect = doc.selectFirst("select[id=ddl_course]")
            if (courseSelect != null) {
                val cleanTarget = courseText.trim()
                for (opt in courseSelect.select("option")) {
                    val optText = opt.text().trim()
                    val optVal = opt.attr("value").trim()
                    if (optVal.isNotEmpty() && optVal != "0") {
                        if (optText.equals(cleanTarget, ignoreCase = true) ||
                            optText.contains(cleanTarget, ignoreCase = true) ||
                            cleanTarget.contains(optText, ignoreCase = true)) {
                            actualCourseValue = optVal
                            break
                        }
                    }
                }
            }

            // 4. Get batch list -> Post
            fields["ddl_reg"] = regionValue
            fields["ddlPou"] = actualPouValue
            fields["ddl_course"] = actualCourseValue
            fields["btn_getlist"] = "Get List"
            fields["__EVENTTARGET"] = ""
            fields["__EVENTARGUMENT"] = ""

            val form3 = FormBody.Builder()
            fields.forEach { (k, v) -> form3.add(k, v) }

            val postReq3 = Request.Builder()
                .url(BASE_URL)
                .header("User-Agent", USER_AGENT)
                .post(form3.build())
                .build()

            val batchListHtml = client.newCall(postReq3).execute().use { resp ->
                resp.body?.string() ?: ""
            }

            doc = Jsoup.parse(batchListHtml)

            var rows = doc.select("#upd_grid table tbody tr")
            if (rows.isEmpty()) {
                rows = doc.select("#upd_grid table tr")
            }

            val batches = mutableListOf<BatchInfo>()

            for (i in 1 until rows.size) { // skip header row
                val cells = rows[i].select("td")
                if (cells.size >= 2) {
                    val batchName = cells[0].text().trim()
                    var availSeats = 0
                    var totSeats = 0

                    try {
                        availSeats = cells[1].text().trim().replace(",", "").toInt()
                    } catch (_: Exception) {}

                    if (cells.size >= 3) {
                        try {
                            totSeats = cells[2].text().trim().replace(",", "").toInt()
                        } catch (_: Exception) {}
                    }

                    val startDate = if (cells.size >= 4) cells[3].text().trim() else ""
                    val endDate = if (cells.size >= 5) cells[4].text().trim() else ""
                    val dates = when {
                        startDate.isNotBlank() && endDate.isNotBlank() && !startDate.contains(" to ", ignoreCase = true) -> "$startDate to $endDate"
                        startDate.isNotBlank() -> startDate
                        else -> endDate
                    }
                    val timings = if (cells.size >= 6) cells[5].text().trim() else ""
                    val venue = if (cells.size >= 7) cells[6].text().trim() else ""
                    val fee = if (cells.size >= 8) cells[7].text().trim() else ""

                    batches.add(
                        BatchInfo(
                            batchName = batchName,
                            totalSeats = if (totSeats > 0) totSeats else (availSeats + 40),
                            availableSeats = availSeats,
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
            }

            ScraperResult.Success(batches)
        } catch (e: Exception) {
            ScraperResult.Error("Network error: ${e.message ?: "Failed to connect to ICAI portal"}", e)
        }
    }

    private fun getPreloadedRegions(): List<DropdownOption> {
        return listOf(
            DropdownOption("1", "Central"),
            DropdownOption("2", "Eastern"),
            DropdownOption("3", "Northern"),
            DropdownOption("4", "Southern"),
            DropdownOption("5", "Western")
        )
    }

    private fun getMockBatches(regionText: String, pouText: String, courseText: String): List<BatchInfo> {
        val now = System.currentTimeMillis()
        val seatRoll = (now / 10000) % 3

        return listOf(
            BatchInfo(
                batchName = "$pouText $courseText BATCH #102",
                totalSeats = 45,
                availableSeats = if (seatRoll == 0L) 3 else 0,
                dates = "01-Aug-2026 to 15-Aug-2026",
                timings = "09:30 AM - 04:30 PM",
                venue = "$pouText ICAI Bhawan, Main Auditorium",
                fee = "₹ 7,000/-",
                statusText = if (seatRoll == 0L) "3 Seats OPEN" else "Full",
                regionName = regionText,
                pouName = pouText,
                courseName = courseText
            ),
            BatchInfo(
                batchName = "$pouText $courseText BATCH #103",
                totalSeats = 50,
                availableSeats = if (seatRoll == 1L) 1 else 0,
                dates = "16-Aug-2026 to 30-Aug-2026",
                timings = "10:00 AM - 05:00 PM",
                venue = "$pouText IT Center, ICAI Annex",
                fee = "₹ 7,000/-",
                statusText = if (seatRoll == 1L) "1 Seat OPEN" else "Full",
                regionName = regionText,
                pouName = pouText,
                courseName = courseText
            )
        )
    }
}

// Simple CookieJar implementation for OkHttp
class JavaNetCookieJar : okhttp3.CookieJar {
    private val cookieStore = java.util.concurrent.CopyOnWriteArrayList<okhttp3.Cookie>()

    override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<okhttp3.Cookie>) {
        cookieStore.removeAll { c -> cookies.any { it.name == c.name } }
        cookieStore.addAll(cookies)
    }

    override fun loadForRequest(url: okhttp3.HttpUrl): List<okhttp3.Cookie> {
        return cookieStore.filter { it.matches(url) }
    }
}
