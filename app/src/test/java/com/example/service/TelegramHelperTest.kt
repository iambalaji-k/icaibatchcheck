package com.example.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramHelperTest {

    @Test
    fun `accepts well-formed bot tokens`() {
        assertTrue(TelegramHelper.isValidToken("123456789:ABCdefGhIJKlm-no_OP12345"))
        assertTrue(TelegramHelper.isValidToken(" 123456789:ABCdef "))
    }

    @Test
    fun `rejects malformed bot tokens that could alter the request path`() {
        assertFalse(TelegramHelper.isValidToken("123456789:abc/def"))
        assertFalse(TelegramHelper.isValidToken("no-colon-token"))
        assertFalse(TelegramHelper.isValidToken("123456789:"))
        assertFalse(TelegramHelper.isValidToken(""))
    }

    @Test
    fun `accepts numeric user, group and channel chat ids and public usernames`() {
        assertTrue(TelegramHelper.isValidChatId("123456789"))
        assertTrue(TelegramHelper.isValidChatId("-1001234567890"))
        assertTrue(TelegramHelper.isValidChatId("@my_icai_alerts"))
    }

    @Test
    fun `rejects invalid chat ids`() {
        assertFalse(TelegramHelper.isValidChatId("abc"))
        assertFalse(TelegramHelper.isValidChatId("@ab"))
        assertFalse(TelegramHelper.isValidChatId("12 34"))
        assertFalse(TelegramHelper.isValidChatId(""))
    }

    @Test
    fun `html escape neutralizes markup from scraped content`() {
        assertEquals(
            "&lt;script&gt;alert(&amp;x)&lt;/script&gt;",
            TelegramHelper.escapeHtml("<script>alert(&x)</script>")
        )
    }

    @Test
    fun `alert message escapes interpolated fields and marks simulation`() {
        val msg = TelegramHelper.buildAlertMessage(
            course = "A<B>&C",
            pou = "Chennai",
            region = "Southern",
            batchName = "BATCH #1",
            availableSeats = 3,
            dates = "01-Aug to 15-Aug",
            simulated = true
        )
        assertTrue(msg.contains("SIMULATION"))
        assertTrue(msg.contains("A&lt;B&gt;&amp;C"))
        assertFalse(msg.contains("A<B>"))
    }
}
