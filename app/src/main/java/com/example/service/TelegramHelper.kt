package com.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object TelegramHelper {

    suspend fun sendMessage(
        botToken: String,
        chatId: String,
        text: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (botToken.isBlank() || chatId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Bot Token or Chat ID is empty."))
            }

            val cleanToken = botToken.trim()
            val cleanChatId = chatId.trim()

            val postData = "chat_id=" + URLEncoder.encode(cleanChatId, "UTF-8") +
                    "&text=" + URLEncoder.encode(text, "UTF-8") +
                    "&parse_mode=" + URLEncoder.encode("HTML", "UTF-8")
            val postDataBytes = postData.toByteArray(Charsets.UTF_8)

            val urlString = "https://api.telegram.org/bot$cleanToken/sendMessage"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("Content-Length", postDataBytes.size.toString())
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            connection.outputStream.use { os ->
                os.write(postDataBytes)
                os.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                Result.success("Telegram notification sent successfully!")
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Result.failure(Exception("Telegram API Error (HTTP $responseCode): $errorStream"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
