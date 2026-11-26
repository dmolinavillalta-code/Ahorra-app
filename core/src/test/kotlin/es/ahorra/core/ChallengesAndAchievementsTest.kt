package es.ahorra.core

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChallengesAndAchievementsTest {

    private val march = YearMonth.of(2026, 3)
    private fun d(m: Int, day: Int) = LocalDate.of(2026, m, day)

    // 2 mar 2026 es lunes (semana ISO 10); 15 mar es domingo (semana ISO 11).
    private fun marchState(vararg c: Contribution) =
        stateWith(goal(target = 10_000_000), *c)

    private val full = marchState(
        contribution("a", 1_000, date = d(3, 2), kind = ContributionKind.EXTRA),
        contribution("b", 1_000, date = d(3, 4)),
        contribution("c", 1_000, date = d(3, 15)),
        contribution("d", 1_000, date = d(2, 28)), // fuera del mes
    )

    @Test
    fun everyChallengeCountsOnlyItsMonth() {
        assertEquals(1, Challenges.progress(full, ChallengeType.EXTRA, march).current)
        assertEquals(3, Challenges.progress(full, ChallengeType.THREE, march).current)
        assertEquals(2, Challenges.progress(full, ChallengeType.EARLY, march).current)
        assertEquals(2, Challenges.progress(full, ChallengeType.WEEKS, march).current)
        assertTrue(ChallengeType.entries.all { Challenges.progress(full, it, march).completed })
    }

    @Test
    fun partialProgressIsReportedAsAFraction() {
        val s = marchState(contribution("a", 1_000, date = d(3, 2)), contribution("b", 1_000, date = d(3, 4)))
        val weeks = Challenges.progress(s, ChallengeType.WEEKS, march)
        assertEquals(1, weeks.current)
        assertFalse(weeks.completed)
        assertEquals(0.5f, weeks.fraction)
        assertEquals(0f, Challenges.progress(s, ChallengeType.EXTRA, march).fraction)
    }

    @Test
    fun acceptingIsVoluntaryIdempotentAndValidated() {
        val s0 = AppState()
        val s1 = Ledger.acceptChallenge(s0, "three", d(3, 10)).okState()
        assertEquals(listOf(ActiveChallenge("three", "2026-03")), s1.challenges)
        assertFalse((Ledger.acceptChallenge(s1, "three", d(3, 20)) as LedgerResult.Ok).changed)
        assertEquals(Reason.INVALID_CHALLENGE, (Ledger.acceptChallenge(s0, "casino", d(3, 10)) as LedgerResult.Rejected).reason)

        assertEquals(listOf(ChallengeType.EXTRA, ChallengeType.EARLY, ChallengeType.WEEKS), Challenges.available(s1, d(3, 10)))
        assertEquals(ChallengeType.entries.toList(), Challenges.available(s1, d(4, 1)))

        val s2 = Ledger.dismissChallenge(s1, "three", "2026-03").okState()
        assertTrue(s2.challenges.isEmpty())
        assertFalse((Ledger.dismissChallenge(s2, "three", "2026-03") as LedgerResult.Ok).changed)
    }

    @Test
    fun activeChallengesShowProgressAndCompletedOnesUnlockAnAchievement() {
        var s = full
        s = Ledger.acceptChallenge(s, "three", d(3, 20)).okState()
        val active = Challenges.active(s, d(3, 20))
        assertEquals(1, active.size)
        assertTrue(active.single().completed)
        assertEquals(1, Challenges.completedCount(s))
        assertTrue(Achievement.CHALLENGE_DONE in Gamification.achievements(s, d(3, 20)))
        assertFalse(Achievement.CHALLENGE_DONE in Gamification.achievements(full, d(3, 20)))
    }

    @Test
    fun percentageAchievementsUseIntegerMath() {
        fun achievementsAt(saved: Long) = Gamification.achievements(
            stateWith(goal(target = 10_000), contribution("a", saved)), TODAY,
        )
        assertFalse(Achievement.PERCENT_25 in achievementsAt(2_499))
        assertTrue(Achievement.PERCENT_25 in achievementsAt(2_500))
        assertFalse(Achievement.HALFWAY in achievementsAt(4_999))
        assertTrue(Achievement.HALFWAY in achievementsAt(5_000))
        assertFalse(Achievement.PERCENT_75 in achievementsAt(7_499))
        assertTrue(Achievement.PERCENT_75 in achievementsAt(7_500))
    }

    @Test
    fun volumeAndMultiGoalAchievements() {
        val many = stateWith(goal(target = 100_000_000), *Array(10) { contribution("c$it", 100) })
        assertTrue(Achievement.TEN_CONTRIBUTIONS in Gamification.achievements(many, TODAY))
        assertFalse(Achievement.TEN_CONTRIBUTIONS in Gamification.achievements(stateWith(goal(), contribution("x", 1)), TODAY))
        val two = AppState(goals = listOf(goal(id = "g1"), goal(id = "g2")))
        assertTrue(Achievement.TWO_GOALS in Gamification.achievements(two, TODAY))
    }

    @Test
    fun levelFractionGrowsInsideALevelAndCapsAtTheTop() {
        assertEquals(0f, Gamification.levelFraction(0))
        assertEquals(0f, Gamification.levelFraction(1))
        assertEquals(0.5f, Gamification.levelFraction(2))
        assertEquals(1f, Gamification.levelFraction(96))
        assertEquals(1f, Gamification.levelFraction(1_000))
    }
}
