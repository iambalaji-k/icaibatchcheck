package com.example.data.repository

import android.util.Log
import com.example.data.IcaiUrls
import com.example.data.model.BatchInfo
import com.example.data.model.DropdownOption
import com.example.data.model.ScraperResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

object IcaiScraperRepository {

    private const val TAG = "IcaiScraper"

    private val cookieJar = AppCookieJar()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .cookieJar(cookieJar) // maintain session cookies
        .build()

    private const val BASE_URL = IcaiUrls.PORTAL
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0"

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
                val options = BatchParser.parseDropdownOptions(doc.selectFirst("select[id=ddl_reg]"))

                if (options.isEmpty()) {
                    Log.w(TAG, "Region dropdown parsed empty — portal layout may have changed")
                    ScraperResult.Error("ICAI portal did not return a region list (page layout changed?)")
                } else {
                    ScraperResult.Success(options)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Region fetch failed", e)
            ScraperResult.Error("ICAI Portal Error: ${e.localizedMessage ?: "Failed to connect"}", e)
        }
    }

    suspend fun fetchPousForRegion(regionValue: String): ScraperResult<List<DropdownOption>> = withContext(Dispatchers.IO) {
        try {
            // 1. GET initial page
            val getReq = Request.Builder()
                .url(BASE_URL)
                .header("User-Agent", USER_AGENT)
                .build()

            val initHtml = client.newCall(getReq).execute().use { resp ->
                if (!resp.isSuccessful) throw Exception("Initial GET failed with HTTP ${resp.code}")
                resp.body?.string() ?: ""
            }

            val doc1 = Jsoup.parse(initHtml)
            val fields = extractHiddenFields(doc1)

            // 2. Select Region -> PostBack
            fields["ddl_reg"] = regionValue
            fields["__EVENTTARGET"] = "ddl_reg"
            fields["__EVENTARGUMENT"] = ""
            val form = FormBody.Builder()
            fields.forEach { (k, v) -> form.add(k, v) }

            val postReq = Request.Builder()
                .url(BASE_URL)
                .header("User-Agent", USER_AGENT)
                .post(form.build())
                .build()

            val htmlStep2 = client.newCall(postReq).execute().use { resp ->
                if (!resp.isSuccessful) throw Exception("POU fetch failed with HTTP ${resp.code}")
                resp.body?.string() ?: ""
            }

            val doc2 = Jsoup.parse(htmlStep2)
            val pouSelect = doc2.selectFirst("select[id=ddlPou]") ?: doc2.selectFirst("select[name=ddlPou]")
            val options = BatchParser.parseDropdownOptions(pouSelect)

            if (options.isEmpty()) {
                Log.w(TAG, "No POU options parsed for region $regionValue")
                ScraperResult.Error("ICAI portal did not return centers for this region")
            } else {
                ScraperResult.Success(options)
            }
        } catch (e: Exception) {
            Log.w(TAG, "POU fetch failed for region $regionValue", e)
            ScraperResult.Error("ICAI Portal Error: ${e.localizedMessage ?: "Failed to fetch centers"}", e)
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
                if (!resp.isSuccessful) throw Exception("Region select POST failed with HTTP ${resp.code}")
                resp.body?.string() ?: ""
            }

            doc = Jsoup.parse(htmlStep2)
            fields = extractHiddenFields(doc)

            // Resolve POU value by matching displayed text against the live dropdown
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
                if (!resp.isSuccessful) throw Exception("POU select POST failed with HTTP ${resp.code}")
                resp.body?.string() ?: ""
            }

            doc = Jsoup.parse(htmlStep3)
            fields = extractHiddenFields(doc)

            // Resolve Course value against the live dropdown: exact label first,
            // then word-token matching (never loose substring, which would
            // confuse ICITSS with AICITSS).
            var actualCourseValue = courseValue
            val courseSelect = doc.selectFirst("select[id=ddl_course]")
            if (courseSelect != null) {
                val cleanTarget = courseText.trim()
                val targetTokens = BatchParser.courseTokens(cleanTarget)
                var tokenMatch: String? = null
                for (opt in courseSelect.select("option")) {
                    val optText = opt.text().trim()
                    val optVal = opt.attr("value").trim()
                    if (optVal.isNotEmpty() && optVal != "0") {
                        if (optText.equals(cleanTarget, ignoreCase = true)) {
                            actualCourseValue = optVal
                            tokenMatch = null
                            break
                        }
                        if (tokenMatch == null &&
                            BatchParser.courseTokens(optText).any { it in targetTokens }) {
                            tokenMatch = optVal
                        }
                    }
                }
                if (actualCourseValue == courseValue && tokenMatch != null) {
                    actualCourseValue = tokenMatch
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
                if (!resp.isSuccessful) throw Exception("Batch list request failed with HTTP ${resp.code}")
                resp.body?.string() ?: ""
            }

            doc = Jsoup.parse(batchListHtml)

            var rows = doc.select("#upd_grid table tbody tr")
            if (rows.isEmpty()) {
                rows = doc.select("#upd_grid table tr")
            }

            ScraperResult.Success(BatchParser.parseBatchRows(rows, regionText, pouText, courseText))
        } catch (e: Exception) {
            ScraperResult.Error("ICAI Portal Error: ${e.localizedMessage ?: "Failed to connect to ICAI portal"}", e)
        }
    }

    private fun getMockBatches(regionText: String, pouText: String, courseText: String): List<BatchInfo> {
        val now = System.currentTimeMillis()
        val seatRoll = (now / 10000) % 3

        return listOf(
            BatchInfo(
                batchName = "$pouText $courseText BATCH #102",
                totalSeats = 45,
                availableSeats = if (seatRoll == 0L) 3 else 0, knownCapacity = true,
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
                availableSeats = if (seatRoll == 1L) 1 else 0, knownCapacity = true,
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

// CookieJar shared by all scraper callers so the UI and the service keep one session.
private class AppCookieJar : CookieJar {
    private val cookieStore = CopyOnWriteArrayList<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore.removeAll { c -> cookies.any { it.name == c.name } }
        cookieStore.addAll(cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore.filter { it.matches(url) }
    }
}
