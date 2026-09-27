package com.example.ui.screens

import android.content.Context
import android.text.format.DateFormat
import java.util.Date

// Thread-safe by construction: formatters are created per call (cheap) instead of
// sharing a mutable SimpleDateFormat across threads. Honors the 12/24-hour system setting.
fun formatTimestamp(context: Context, timestamp: Long): String =
    DateFormat.getTimeFormat(context).format(Date(timestamp))
