package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ICAI Batch Checker", appName)
  }

  @Test
  fun `verify theme preferences default and persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = com.example.data.repository.UserPreferences(context)
    assertEquals(com.example.ui.theme.AppThemeMode.SYSTEM.name, prefs.themeMode)

    prefs.themeMode = com.example.ui.theme.AppThemeMode.AMOLED.name
    assertEquals(com.example.ui.theme.AppThemeMode.AMOLED.name, prefs.themeMode)

    prefs.themeMode = com.example.ui.theme.AppThemeMode.DARK.name
    assertEquals(com.example.ui.theme.AppThemeMode.DARK.name, prefs.themeMode)

    prefs.themeMode = com.example.ui.theme.AppThemeMode.LIGHT.name
    assertEquals(com.example.ui.theme.AppThemeMode.LIGHT.name, prefs.themeMode)
  }
}
