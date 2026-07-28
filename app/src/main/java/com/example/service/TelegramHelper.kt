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
            val encodedText = URLEncoder.encode(text, "UTF-8")

            val urlString = "https://api.telegram.org/bot$cleanToken/sendMessage?chat_id=$cleanChatId&text=$encodedText&parse_mode=HTML"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

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
