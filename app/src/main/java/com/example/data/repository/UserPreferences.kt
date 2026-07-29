package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.BatchTarget
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("icai_checker_prefs", Context.MODE_PRIVATE)

    var regionValue: String
        get() = prefs.getString(KEY_REGION_VAL, "4") ?: "4" // 4 = Southern
        set(value) = prefs.edit().putString(KEY_REGION_VAL, value).apply()

    var regionText: String
        get() = prefs.getString(KEY_REGION_TXT, "Southern") ?: "Southern"
        set(value) = prefs.edit().putString(KEY_REGION_TXT, value).apply()

    var pouValue: String
        get() = prefs.getString(KEY_POU_VAL, "3") ?: "3" // Default value for Chennai
        set(value) = prefs.edit().putString(KEY_POU_VAL, value).apply()

    var pouText: String
        get() = prefs.getString(KEY_POU_TXT, "Chennai") ?: "Chennai"
        set(value) = prefs.edit().putString(KEY_POU_TXT, value).apply()

    var courseValue: String
        get() = prefs.getString(KEY_COURSE_VAL, "48") ?: "48" // 48 = AICITSS - Advanced Information Technology
        set(value) = prefs.edit().putString(KEY_COURSE_VAL, value).apply()

    var courseText: String
        get() = prefs.getString(KEY_COURSE_TXT, "AICITSS - Advanced Information Technology") ?: "AICITSS - Advanced Information Technology"
        set(value) = prefs.edit().putString(KEY_COURSE_TXT, value).apply()

    var intervalMinutes: Int
        get() = prefs.getInt(KEY_INTERVAL_MINS, 5)
        set(value) = prefs.edit().putInt(KEY_INTERVAL_MINS, value).apply()

    var isMonitoringActive: Boolean
        get() = prefs.getBoolean(KEY_IS_MONITORING, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_MONITORING, value).apply()

    var notifyOnlyNewSeats: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_NEW, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_NEW, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var lastCheckTime: Long
        get() = prefs.getLong(KEY_LAST_CHECK, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CHECK, value).apply()

    var mockModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_MOCK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_MOCK_MODE, value).apply()

    var telegramBotToken: String
        get() = prefs.getString(KEY_TELEGRAM_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TELEGRAM_TOKEN, value).apply()

    var telegramChatId: String
        get() = prefs.getString(KEY_TELEGRAM_CHAT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TELEGRAM_CHAT_ID, value).apply()

    var telegramEnabled: Boolean
        get() = prefs.getBoolean(KEY_TELEGRAM_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_TELEGRAM_ENABLED, value).apply()

    fun getTargets(): List<BatchTarget> {
        val jsonStr = prefs.getString(KEY_TARGETS_JSON, null)
        if (jsonStr.isNull_or_blank()) {
            return emptyList()
        }
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<BatchTarget>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    BatchTarget(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        regionValue = obj.optString("regionValue", "4"),
                        regionText = obj.optString("regionText", "Southern"),
                        pouValue = obj.optString("pouValue", "3"),
                        pouText = obj.optString("pouText", "Chennai"),
                        courseValue = obj.optString("courseValue", "48"),
                        courseText = obj.optString("courseText", "AICITSS - Advanced Information Technology"),
                        isEnabled = obj.optBoolean("isEnabled", true)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveTargets(targets: List<BatchTarget>) {
        val array = JSONArray()
        for (t in targets) {
            val obj = JSONObject().apply {
                put("id", t.id)
                put("regionValue", t.regionValue)
                put("regionText", t.regionText)
                put("pouValue", t.pouValue)
                put("pouText", t.pouText)
                put("courseValue", t.courseValue)
                put("courseText", t.courseText)
                put("isEnabled", t.isEnabled)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_TARGETS_JSON, array.toString()).apply()

        // Sync first target to primary properties for legacy fallback
        if (targets.isNotEmpty()) {
            val first = targets.first()
            regionValue = first.regionValue
            regionText = first.regionText
            pouValue = first.pouValue
            pouText = first.pouText
            courseValue = first.courseValue
            courseText = first.courseText
        }
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty()
    }

    companion object {
        private const val KEY_REGION_VAL = "region_val"
        private const val KEY_REGION_TXT = "region_txt"
        private const val KEY_POU_VAL = "pou_val"
        private const val KEY_POU_TXT = "pou_txt"
        private const val KEY_COURSE_VAL = "course_val"
        private const val KEY_COURSE_TXT = "course_txt"
        private const val KEY_INTERVAL_MINS = "interval_mins"
        private const val KEY_IS_MONITORING = "is_monitoring"
        private const val KEY_NOTIFY_NEW = "notify_new"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_LAST_CHECK = "last_check_time"
        private const val KEY_MOCK_MODE = "mock_mode"
        private const val KEY_TELEGRAM_TOKEN = "telegram_token"
        private const val KEY_TELEGRAM_CHAT_ID = "telegram_chat_id"
        private const val KEY_TELEGRAM_ENABLED = "telegram_enabled"
        private const val KEY_TARGETS_JSON = "targets_json"
    }
}

