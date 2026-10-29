package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.Difficulty
import com.example.game.MedalType
import com.example.game.calculateMedal
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
    assertEquals("Flappy Heroes", appName)
    val credit = context.getString(R.string.credit_by)
    assertEquals("Credit by Ghazia Abena", credit)
  }

  @Test
  fun `test medal calculation`() {
    assertEquals(MedalType.NONE, calculateMedal(0))
    assertEquals(MedalType.BRONZE, calculateMedal(5))
    assertEquals(MedalType.SILVER, calculateMedal(10))
    assertEquals(MedalType.GOLD, calculateMedal(20))
    assertEquals(MedalType.PLATINUM, calculateMedal(35))
  }

  @Test
  fun `test character skins`() {
    val names = com.example.game.CharacterSkin.values().map { it.displayName }
    assertEquals(listOf("Rocky Gibraltar", "Fred Flintstone", "Kamisato Ayaka", "Tanjiro Kamado"), names)
  }

  @Test
  fun `test sky themes`() {
    val themes = com.example.game.SkyTheme.values().map { it.name }
    assertEquals(listOf("RANDOM", "DAY", "SUNSET", "NIGHT"), themes)
  }
}
