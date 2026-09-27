package com.example.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringModeTest {

    @Test
    fun `intervals below 15 minutes use fast looping service`() {
        assertTrue(MonitoringScheduler.isFastInterval(2))
        assertTrue(MonitoringScheduler.isFastInterval(5))
        assertTrue(MonitoringScheduler.isFastInterval(10))
    }

    @Test
    fun `intervals at or above 15 minutes use WorkManager periodic schedule`() {
        assertFalse(MonitoringScheduler.isFastInterval(15))
        assertFalse(MonitoringScheduler.isFastInterval(30))
    }
}
