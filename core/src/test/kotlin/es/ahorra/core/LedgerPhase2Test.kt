package es.ahorra.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LedgerPhase2Test {

    private fun rejected(r: LedgerResult): Reason = (r as LedgerResult.Rejected).reason

    @Test
    fun goalsCarryAnIconThatMustBeValid() {
        val ok = Ledger.createGoal(AppState(), goal().copy(icon = "plane")).okState()
        assertEquals("plane", ok.goals.single().icon)
        assertEquals("target", Ledger.createGoal(AppState(), goal()).okState().goals.single().icon)
        assertEquals(Reason.INVALID_ICON, rejected(Ledger.createGoal(AppState(), goal().copy(icon = "<script>"))))
    }

    @Test
    fun updatePlanCanChangeOrKeepTheIcon() {
        val s = Ledger.createGoal(AppState(), goal().copy(icon = "car")).okState()
        assertEquals("car", Ledger.updatePlan(s, "g1", "Móvil", 120_000, 6, 5, TODAY).okState().goals.single().icon)
        assertEquals("gift", Ledger.updatePlan(s, "g1", "Móvil", 120_000, 6, 5, TODAY, icon = "gift").okState().goals.single().icon)
        assertEquals(Reason.INVALID_ICON, rejected(Ledger.updatePlan(s, "g1", "Móvil", 120_000, 6, 5, TODAY, icon = "nope")))
    }

    @Test
    fun displayNameIsTrimmedAndLimited() {
        val s = AppState()
        val saved = Ledger.updateSettings(s, Settings(displayName = "  David  ", welcomeSeen = true)).okState()
        assertEquals("David", saved.settings.displayName)
        assertEquals(true, saved.settings.welcomeSeen)
        assertEquals(
            Reason.INVALID_SETTINGS,
            rejected(Ledger.updateSettings(s, Settings(displayName = "x".repeat(Limits.MAX_DISPLAY_NAME + 1)))),
        )
    }

    @Test
    fun newAccentsUnlockAtLevelFiveAndSix() {
        val twelve = AppState(contributions = List(12) { contribution("c$it", 100) })
        assertEquals(Reason.INVALID_SETTINGS, rejected(Ledger.updateSettings(AppState(), Settings(accent = Accent.EMERALD))))
        assertEquals(Accent.EMERALD, Ledger.updateSettings(twelve, Settings(accent = Accent.EMERALD)).okState().settings.accent)
        assertEquals(Reason.INVALID_SETTINGS, rejected(Ledger.updateSettings(twelve, Settings(accent = Accent.VIOLET))))
        val twentyFour = AppState(contributions = List(24) { contribution("c$it", 100) })
        assertEquals(Accent.VIOLET, Ledger.updateSettings(twentyFour, Settings(accent = Accent.VIOLET)).okState().settings.accent)
    }

    @Test
    fun importValidatesIconsNamesAndChallenges() {
        val base = stateWith(goal())
        assertNotNull(Ledger.sanitizeImport(base, TODAY))
        assertNull(Ledger.sanitizeImport(stateWith(goal().copy(icon = "hack")), TODAY))
        assertNull(Ledger.sanitizeImport(base.copy(settings = Settings(displayName = "x".repeat(31))), TODAY))
        assertNull(Ledger.sanitizeImport(base.copy(challenges = listOf(ActiveChallenge("casino", "2026-03"))), TODAY))
        assertNull(Ledger.sanitizeImport(base.copy(challenges = listOf(ActiveChallenge("three", "marzo"))), TODAY))
        val dup = ActiveChallenge("three", "2026-03")
        assertNull(Ledger.sanitizeImport(base.copy(challenges = listOf(dup, dup)), TODAY))
        assertNotNull(Ledger.sanitizeImport(base.copy(challenges = listOf(dup)), TODAY))
    }
}
