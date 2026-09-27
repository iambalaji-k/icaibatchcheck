package com.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object TelegramHelper {

    private val TOKEN_PATTERN = Regex("^[0-9]+:[A-Za-z0-9_-]+$")
    // Numeric IDs (users, plus -100-prefixed groups/channels) or public @usernames.
    private val CHAT_ID_PATTERN = Regex("^(-?[0-9]{1,32}|@[A-Za-z][A-Za-z0-9_]{3,31})$")

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun isValidToken(token: String): Boolean = token.trim().matches(TOKEN_PATTERN)

    fun isValidChatId(chatId: String): Boolean = chatId.trim().matches(CHAT_ID_PATTERN)

    /** Escapes text interpolated into HTML-mode Telegram messages. */
    fun escapeHtml(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    fun buildAlertMessage(
        course: String,
        pou: String,
        region: String,
        batchName: String,
        availableSeats: Int,
        dates: String,
        simulated: Boolean
    ): String {
        val header = if (simulated) "<b>🧪 SIMULATION: ICAI SLOT OPEN ALERT</b>" else "<b>🚨 ICAI SLOT OPEN ALERT</b>"
        return header + "\n\n" +
                "<b>Course:</b> " + escapeHtml(course) + "\n" +
                "<b>POU / City:</b> " + escapeHtml(pou) + " (" + escapeHtml(region) + ")\n" +
                "<b>Batch:</b> " + escapeHtml(batchName) + "\n" +
                "<b>Available Seats:</b> <b>" + availableSeats + "</b>\n" +
                "<b>Dates:</b> " + escapeHtml(dates) + "\n\n" +
                "🔗 <a href='" + com.example.data.IcaiUrls.PORTAL + "'>Register on ICAI Portal Now</a>"
    }

    /**
     * Deliberately NOT sharing the scraper's OkHttpClient: the ICAI session
     * cookies must never be sent to api.telegram.org.
     */
    suspend fun sendMessage(
        botToken: String,
        chatId: String,
        text: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanToken = botToken.trim()
        val cleanChatId = chatId.trim()

        if (cleanToken.isBlank() || cleanChatId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Bot Token or Chat ID is empty."))
        }
        if (!cleanToken.matches(TOKEN_PATTERN)) {
            return@withContext Result.failure(IllegalArgumentException("Bot Token format is invalid."))
        }
        if (!cleanChatId.matches(CHAT_ID_PATTERN)) {
            return@withContext Result.failure(IllegalArgumentException("Chat ID must be numeric or a public @username."))
        }

        val body = FormBody.Builder()
            .add("chat_id", cleanChatId)
            .add("text", text)
            .add("parse_mode", "HTML")
            .build()
        val request = Request.Builder()
            .url("https://api.telegram.org/bot$cleanToken/sendMessage")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success("Telegram notification sent.")
                } else {
                    Result.failure(Exception("Telegram API Error (HTTP ${response.code}): $responseBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
