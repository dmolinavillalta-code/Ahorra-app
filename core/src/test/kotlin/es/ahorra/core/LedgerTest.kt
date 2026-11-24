package es.ahorra.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LedgerTest {

    private fun rejected(r: LedgerResult): Reason = (r as LedgerResult.Rejected).reason

    // ---- Crear objetivos ---------------------------------------------------------------------

    @Test
    fun createGoalTrimsNameAndIsIdempotentById() {
        val first = Ledger.createGoal(AppState(), goal().copy(name = "  Móvil  "))
        val state = first.okState()
        assertEquals("Móvil", state.goals.single().name)

        val again = Ledger.createGoal(state, goal()) as LedgerResult.Ok
        assertFalse(again.changed)
        assertEquals(1, again.state.goals.size)
    }

    @Test
    fun createGoalValidatesEveryField() {
        assertEquals(Reason.INVALID_NAME, rejected(Ledger.createGoal(AppState(), goal().copy(name = "   "))))
        assertEquals(Reason.INVALID_TARGET, rejected(Ledger.createGoal(AppState(), goal(target = 0))))
        assertEquals(Reason.INVALID_TARGET, rejected(Ledger.createGoal(AppState(), goal(target = Limits.MAX_TARGET_MINOR + 1))))
        assertEquals(Reason.INVALID_MONTHS, rejected(Ledger.createGoal(AppState(), goal(months = 0))))
        assertEquals(Reason.INVALID_MONTHS, rejected(Ledger.createGoal(AppState(), goal(months = 601))))
        assertEquals(Reason.INVALID_DAY, rejected(Ledger.createGoal(AppState(), goal(day = 32))))
        assertEquals(Reason.INVALID_CURRENCY, rejected(Ledger.createGoal(AppState(), goal(currency = "ZZZ"))))
    }

    // ---- Aportaciones -----------------------------------------------------------------------------

    @Test
    fun duplicateContributionIdIsAppliedOnlyOnce() {
        val s0 = stateWith()
        val c = contribution("c1", 10_000)
        val s1 = Ledger.addContribution(s0, c, TODAY).okState()
        val retry = Ledger.addContribution(s1, c, TODAY) as LedgerResult.Ok
        assertFalse(retry.changed)
        assertEquals(10_000L, Ledger.savedMinor(retry.state, "g1"))
        assertEquals(1, retry.state.contributions.size)
    }

    @Test
    fun contributionsMustBePositiveAlreadyHappenedAndForAnExistingGoal() {
        val s = stateWith()
        assertEquals(Reason.INVALID_AMOUNT, rejected(Ledger.addContribution(s, contribution("a", 0), TODAY)))
        assertEquals(Reason.INVALID_AMOUNT, rejected(Ledger.addContribution(s, contribution("a", -5), TODAY)))
        assertEquals(
            Reason.INVALID_DATE,
            rejected(Ledger.addContribution(s, contribution("a", 100, date = TODAY.plusDays(1)), TODAY)),
        )
        assertEquals(
            Reason.UNKNOWN_GOAL,
            rejected(Ledger.addContribution(s, contribution("a", 100, goalId = "nope"), TODAY)),
        )
    }

    @Test
    fun goalCompletesWhenSavedReachesTargetAndReopensIfAContributionIsRemoved() {
        val g = goal(target = 10_000, months = 2)
        var s = stateWith(g)
        s = Ledger.addContribution(s, contribution("a", 6_000), TODAY).okState()
        assertEquals(GoalStatus.ACTIVE, s.goals.single().status)
        s = Ledger.addContribution(s, contribution("b", 4_000), TODAY).okState()
        assertEquals(GoalStatus.COMPLETED, s.goals.single().status)

        assertEquals(Reason.GOAL_COMPLETED, rejected(Ledger.addContribution(s, contribution("c", 1), TODAY)))

        s = Ledger.removeContribution(s, "b").okState()
        assertEquals(GoalStatus.ACTIVE, s.goals.single().status)
        assertEquals(6_000L, Ledger.savedMinor(s, "g1"))
    }

    @Test
    fun removingUnknownContributionChangesNothing() {
        val r = Ledger.removeContribution(stateWith(), "nope") as LedgerResult.Ok
        assertFalse(r.changed)
    }

    // ---- Cambios de plan, pausas --------------------------------------------------------------------

    @Test
    fun updatePlanRecalculatesFromTodayOverWhatIsMissing() {
        var s = stateWith(goal())
        s = Ledger.addContribution(s, contribution("a", 30_000), TODAY).okState()
        s = Ledger.updatePlan(s, "g1", "Móvil", 120_000, 6, 15, TODAY).okState()
        val g = s.goals.single()
        assertEquals(30_000L, g.planBaseSavedMinor)
        assertEquals(TODAY, g.startDate)
        val schedule = PlanCalculator.schedule(g)
        assertEquals(6, schedule.size)
        assertTrue(schedule.all { it.amountMinor == 15_000L })
        assertEquals(LocalDate.of(2026, 3, 15), schedule.first().dueDate)
    }

    @Test
    fun loweringTheTargetBelowWhatIsSavedCompletesTheGoal() {
        var s = stateWith(goal())
        s = Ledger.addContribution(s, contribution("a", 50_000), TODAY).okState()
        s = Ledger.updatePlan(s, "g1", "Móvil", 40_000, 4, 5, TODAY).okState()
        assertEquals(GoalStatus.COMPLETED, s.goals.single().status)
    }

    @Test
    fun pauseAndResumeShiftTheScheduleWithoutLosingProgress() {
        var s = stateWith(goal())
        s = Ledger.addContribution(s, contribution("a", 10_000, date = LocalDate.of(2026, 1, 20)), TODAY).okState()
        s = Ledger.pauseGoal(s, "g1", TODAY).okState()
        assertEquals(GoalStatus.PAUSED, s.goals.single().status)
        assertFalse((Ledger.pauseGoal(s, "g1", TODAY) as LedgerResult.Ok).changed)

        val resumeDay = LocalDate.of(2026, 5, 2)
        s = Ledger.resumeGoal(s, "g1", resumeDay).okState()
        val g = s.goals.single()
        assertEquals(GoalStatus.ACTIVE, g.status)
        assertNull(g.pausedSince)
        assertEquals(listOf("2026-03", "2026-04", "2026-05"), g.pausedMonths)
        assertEquals(10_000L, g.planBaseSavedMinor)
        assertEquals(11, g.months) // 12 cuotas - 1 cubierta
        assertEquals(10_000L, Ledger.savedMinor(s, "g1")) // no se pierde nada
    }

    @Test
    fun deletingAGoalDeletesItsContributionsOnly() {
        val g2 = goal(id = "g2")
        var s = AppState(goals = listOf(goal(), g2))
        s = Ledger.addContribution(s, contribution("a", 100), TODAY).okState()
        s = Ledger.addContribution(s, contribution("b", 200, goalId = "g2"), TODAY).okState()
        s = Ledger.deleteGoal(s, "g1").okState()
        assertEquals(listOf("g2"), s.goals.map { it.id })
        assertEquals(listOf("b"), s.contributions.map { it.id })
    }

    // ---- Ajustes ------------------------------------------------------------------------------------

    @Test
    fun cosmeticAccentsOnlyUnlockWithConsistency() {
        val s = stateWith()
        assertEquals(
            Reason.INVALID_SETTINGS,
            rejected(Ledger.updateSettings(s, Settings(accent = Accent.ROSE))),
        )
        assertEquals(
            Reason.INVALID_SETTINGS,
            rejected(Ledger.updateSettings(s, Settings(reminderHour = 24))),
        )
        assertTrue(Ledger.updateSettings(s, Settings(remindersEnabled = true, reminderHour = 8)) is LedgerResult.Ok)
    }

    // ---- Importación (archivos no fiables) ------------------------------------------------------------

    @Test
    fun importAcceptsConsistentDataAndRecomputesStatus() {
        val dirty = stateWith(goal(target = 10_000), contribution("a", 10_000)) // estado ACTIVE declarado, pero ya cumplido
        val clean = assertNotNull(Ledger.sanitizeImport(dirty, TODAY))
        assertEquals(GoalStatus.COMPLETED, clean.goals.single().status)
    }

    @Test
    fun importRejectsOrphansDuplicatesAndInvalidAmounts() {
        assertNull(Ledger.sanitizeImport(stateWith(goal(), contribution("a", 100, goalId = "nope")), TODAY))
        assertNull(Ledger.sanitizeImport(stateWith(goal(), contribution("a", 100), contribution("a", 200)), TODAY))
        assertNull(Ledger.sanitizeImport(stateWith(goal(), contribution("a", -100)), TODAY))
        assertNull(Ledger.sanitizeImport(stateWith(goal(), contribution("a", 100, date = TODAY.plusDays(2))), TODAY))
        assertNull(Ledger.sanitizeImport(stateWith(goal(currency = "ZZZ")), TODAY))
        assertNull(Ledger.sanitizeImport(AppState(version = 99), TODAY))
    }
}
