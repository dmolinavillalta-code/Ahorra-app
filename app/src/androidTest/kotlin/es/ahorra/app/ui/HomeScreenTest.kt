package es.ahorra.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.ahorra.app.R
import es.ahorra.app.ui.theme.AhorraTheme
import es.ahorra.core.AppState
import es.ahorra.core.Contribution
import es.ahorra.core.Goal
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Pruebas de interfaz (requieren emulador o dispositivo): `./gradlew :app:connectedDebugAndroidTest`. */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun text(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    @Test
    fun emptyStateInvitesToCreateAGoal() {
        var newGoalClicks = 0
        rule.setContent {
            AhorraTheme { HomeScreen(AppState(), onOpenGoal = {}, onNewGoal = { newGoalClicks++ }) }
        }
        rule.onNodeWithText(text(R.string.home_empty_title)).assertExists()
        rule.onNodeWithText("＋  " + text(R.string.home_new_goal)).performClick()
        assertTrue(newGoalClicks == 1)
    }

    @Test
    fun goalCardShowsOnlyConfirmedSavings() {
        val today = LocalDate.now()
        val goal = Goal(
            id = "g1", name = "Viaje", targetMinor = 300_000, currencyCode = "EUR",
            startDate = today, months = 6, paymentDay = 5,
        )
        val state = AppState(
            goals = listOf(goal),
            contributions = listOf(Contribution("c1", "g1", 50_000, today)),
        )
        rule.setContent {
            AhorraTheme { HomeScreen(state, onOpenGoal = {}, onNewGoal = {}) }
        }
        rule.onNodeWithText("Viaje").assertExists()
        // 500 de 3.000 => 16 % (enteros, sin redondeo hacia arriba).
        rule.onNodeWithText(text(R.string.home_progress, 16)).assertExists()
    }
}
