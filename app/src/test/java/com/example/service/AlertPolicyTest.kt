package com.example.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertPolicyTest {

    @Test
    fun `alerts when a full batch opens`() {
        assertTrue(AlertPolicy.shouldAlert(previousSeats = 0, currentSeats = 3))
    }

    @Test
    fun `alerts when seat count increases`() {
        assertTrue(AlertPolicy.shouldAlert(previousSeats = 2, currentSeats = 5))
    }

    @Test
    fun `stays silent when seats unchanged or decreased`() {
        assertFalse(AlertPolicy.shouldAlert(previousSeats = 3, currentSeats = 3))
        assertFalse(AlertPolicy.shouldAlert(previousSeats = 5, currentSeats = 2))
    }

    @Test
    fun `never alerts for zero seats`() {
        assertFalse(AlertPolicy.shouldAlert(previousSeats = 0, currentSeats = 0))
        assertFalse(AlertPolicy.shouldAlert(previousSeats = 4, currentSeats = 0))
    }

    @Test
    fun `quota stops monitoring just before the 6h system limit`() {
        val start = 1_000_000L
        assertFalse(
            AlertPolicy.exceededDataSyncQuota(start, start + AlertPolicy.DATA_SYNC_QUOTA_MILLIS - 1)
        )
        assertTrue(
            AlertPolicy.exceededDataSyncQuota(start, start + AlertPolicy.DATA_SYNC_QUOTA_MILLIS)
        )
    }

    @Test
    fun `unset start time never trips the quota`() {
        assertFalse(AlertPolicy.exceededDataSyncQuota(0L, Long.MAX_VALUE))
    }
}
