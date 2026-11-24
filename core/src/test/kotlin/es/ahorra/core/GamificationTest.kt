package es.ahorra.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GamificationTest {

    private fun month(m: Int, day: Int = 10) = LocalDate.of(2026, m, day)

    private fun stateWithMonths(vararg months: Int, g: Goal = goal(target = 1_000_000)) =
        stateWith(g, *months.mapIndexed { i, m -> contribution("c$i", 1_000, date = month(m)) }.toTypedArray())

    @Test
    fun levelDependsOnNumberOfContributionsNotOnAmounts() {
        assertEquals(Level(1, 1), Gamification.level(0))
        assertEquals(Level(2, 2), Gamification.level(1))
        assertEquals(Level(3, 3), Gamification.level(3))
        assertEquals(Level(8, null), Gamification.level(96))
        assertEquals(Level(8, null), Gamification.level(500))
    }

    @Test
    fun streakCountsConsecutiveMonthsWithAContribution() {
        val s = stateWithMonths(1, 2, 3)
        assertEquals(StreakInfo(current = 3, best = 3), Gamification.streak(s, LocalDate.of(2026, 3, 15)))
    }

    @Test
    fun streakIsNotBrokenWhileTheCurrentMonthIsStillOpen() {
        val s = stateWithMonths(1, 2)
        assertEquals(2, Gamification.streak(s, LocalDate.of(2026, 3, 10)).current)
    }

    @Test
    fun missingAFullMonthResetsTheCurrentStreakButKeepsTheBest() {
        val s = stateWithMonths(1, 2, 4)
        val info = Gamification.streak(s, LocalDate.of(2026, 4, 20))
        assertEquals(1, info.current)
        assertEquals(2, info.best)
        assertEquals(0, Gamification.streak(stateWithMonths(1), LocalDate.of(2026, 3, 10)).current)
    }

    @Test
    fun pausedMonthsNeitherCountNorBreakTheStreak() {
        val paused = goal(target = 1_000_000).copy(pausedMonths = listOf("2026-02"))
        val s = stateWithMonths(1, 3, g = paused)
        assertEquals(StreakInfo(current = 2, best = 2), Gamification.streak(s, LocalDate.of(2026, 3, 20)))
    }

    @Test
    fun openEndedPauseDoesNotBreakTheStreak() {
        val paused = goal(target = 1_000_000).copy(status = GoalStatus.PAUSED, pausedSince = month(2, 1))
        val s = stateWithMonths(1, g = paused)
        assertEquals(1, Gamification.streak(s, LocalDate.of(2026, 3, 20)).current)
    }

    @Test
    fun achievementsAreUnlockedByProgressAndConsistency() {
        assertTrue(Gamification.achievements(AppState(), TODAY).isEmpty())

        val s = stateWithMonths(1, 2, 3, g = goal(target = 4_000))
        val a = Gamification.achievements(s, LocalDate.of(2026, 3, 15))
        assertTrue(Achievement.FIRST_GOAL in a)
        assertTrue(Achievement.FIRST_CONTRIBUTION in a)
        assertTrue(Achievement.STREAK_3 in a)
        assertFalse(Achievement.STREAK_6 in a)
        assertTrue(Achievement.HALFWAY in a) // 3.000 de 4.000
        assertFalse(Achievement.GOAL_COMPLETED in a)
    }

    @Test
    fun halfwayAndCompletedAreRecognised() {
        var s = stateWith(goal(target = 10_000, months = 2))
        s = Ledger.addContribution(s, contribution("a", 5_000), TODAY).okState()
        assertTrue(Achievement.HALFWAY in Gamification.achievements(s, TODAY))
        assertFalse(Achievement.GOAL_COMPLETED in Gamification.achievements(s, TODAY))
        s = Ledger.addContribution(s, contribution("b", 5_000, kind = ContributionKind.EXTRA), TODAY).okState()
        val a = Gamification.achievements(s, TODAY)
        assertTrue(Achievement.GOAL_COMPLETED in a)
        assertTrue(Achievement.EXTRA_CONTRIBUTION in a)
    }

    @Test
    fun accentsUnlockWithLevelAndBreakingAStreakNeverRemovesThem() {
        assertEquals(listOf(Accent.TEAL), Accent.unlockedAt(1))
        assertEquals(listOf(Accent.TEAL, Accent.INDIGO), Accent.unlockedAt(2))
        assertNull(Gamification.level(96).contributionsToNext)
    }
}
