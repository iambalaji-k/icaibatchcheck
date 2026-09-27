package com.example.service

/**
 * Pure alert-dedup rules shared by the monitoring loop, kept side-effect free
 * so they can be unit tested.
 */
object AlertPolicy {

    fun shouldAlert(previousSeats: Int, currentSeats: Int): Boolean {
        if (currentSeats <= 0) return false
        return previousSeats == 0 || currentSeats > previousSeats
    }

    /**
     * dataSync foreground services have a ~6h/24h runtime quota on Android 14+.
     * Stop monitoring gracefully before the system enforces the limit with a
     * ForegroundService$TimeLimitExceededException.
     */
    fun exceededDataSyncQuota(monitoringSinceMillis: Long, nowMillis: Long): Boolean {
        if (monitoringSinceMillis <= 0L) return false
        return nowMillis - monitoringSinceMillis >= DATA_SYNC_QUOTA_MILLIS
    }

    const val DATA_SYNC_QUOTA_MILLIS = 5 * 60 * 60 * 1000L
}
