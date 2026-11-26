package es.ahorra.core

import java.time.LocalDate
import java.time.YearMonth

data class StreakInfo(val current: Int, val best: Int)

/** [contributionsToNext] es null en el nivel máximo. */
data class Level(val number: Int, val contributionsToNext: Int?)

enum class Achievement {
    FIRST_GOAL, FIRST_CONTRIBUTION, EXTRA_CONTRIBUTION, STREAK_3, STREAK_6, STREAK_12,
    PERCENT_25, HALFWAY, PERCENT_75, GOAL_COMPLETED, TEN_CONTRIBUTIONS, TWO_GOALS, CHALLENGE_DONE
}

/**
 * Motivación por progreso y constancia, sin presión: la racha cuenta meses con al menos una aportación,
 * el nivel depende del número de aportaciones (no de su importe) y romper una racha no quita nada.
 */
object Gamification {

    private val levelThresholds = listOf(0, 1, 3, 6, 12, 24, 48, 96)

    /** Avance (0..1) dentro del nivel actual hacia el siguiente; 1 en el nivel máximo. */
    fun levelFraction(confirmedContributions: Int): Float {
        val index = levelThresholds.indexOfLast { confirmedContributions >= it }
        val next = levelThresholds.getOrNull(index + 1) ?: return 1f
        return (confirmedContributions - levelThresholds[index]).toFloat() / (next - levelThresholds[index])
    }

    fun level(confirmedContributions: Int): Level {
        val number = levelThresholds.indexOfLast { confirmedContributions >= it } + 1
        val next = levelThresholds.getOrNull(number)?.minus(confirmedContributions)
        return Level(number, next)
    }

    fun streak(state: AppState, today: LocalDate): StreakInfo {
        val done: Set<YearMonth> = state.contributions.map { YearMonth.from(it.date) }.toSet()
        val neutral = neutralMonths(state, today)
        val thisMonth = YearMonth.from(today)

        // El mes en curso aún está abierto: si todavía no hay aportación, la racha se cuenta desde el mes anterior.
        var cursor = if (thisMonth in done || thisMonth in neutral) thisMonth else thisMonth.minusMonths(1)
        var current = 0
        while (cursor in done || cursor in neutral) {
            if (cursor in done) current++
            cursor = cursor.minusMonths(1)
        }

        var best = 0
        var run = 0
        var previous: YearMonth? = null
        for (month in (done + neutral).toSortedSet()) {
            val p = previous
            if (p == null || p.plusMonths(1) != month) run = 0
            if (month in done) run++
            if (run > best) best = run
            previous = month
        }
        return StreakInfo(current = current, best = best)
    }

    fun achievements(state: AppState, today: LocalDate): Set<Achievement> {
        val result = mutableSetOf<Achievement>()
        if (state.goals.isNotEmpty()) result.add(Achievement.FIRST_GOAL)
        if (state.contributions.isNotEmpty()) result.add(Achievement.FIRST_CONTRIBUTION)
        if (state.contributions.any { it.kind == ContributionKind.EXTRA }) result.add(Achievement.EXTRA_CONTRIBUTION)
        val best = streak(state, today).best
        if (best >= 3) result.add(Achievement.STREAK_3)
        if (best >= 6) result.add(Achievement.STREAK_6)
        if (best >= 12) result.add(Achievement.STREAK_12)
        fun reached(numerator: Long, denominator: Long) =
            state.goals.any { it.targetMinor > 0 && Ledger.savedMinor(state, it.id) * denominator >= it.targetMinor * numerator }
        if (reached(1, 4)) result.add(Achievement.PERCENT_25)
        if (reached(1, 2)) result.add(Achievement.HALFWAY)
        if (reached(3, 4)) result.add(Achievement.PERCENT_75)
        if (state.contributions.size >= 10) result.add(Achievement.TEN_CONTRIBUTIONS)
        if (state.goals.size >= 2) result.add(Achievement.TWO_GOALS)
        if (Challenges.completedCount(state) > 0) result.add(Achievement.CHALLENGE_DONE)
        if (state.goals.any { it.status == GoalStatus.COMPLETED }) result.add(Achievement.GOAL_COMPLETED)
        return result
    }

    /** Meses "neutros": todos los objetivos abiertos estaban en pausa. No suman ni rompen la racha. */
    private fun neutralMonths(state: AppState, today: LocalDate): Set<YearMonth> {
        val open = state.goals.filter { it.status != GoalStatus.COMPLETED }
        if (open.isEmpty()) return emptySet()
        val perGoal: List<Set<YearMonth>> = open.map { goal ->
            val months = goal.pausedMonths
                .mapNotNull { runCatching { YearMonth.parse(it) }.getOrNull() }
                .toMutableSet()
            goal.pausedSince?.let { since ->
                var m = YearMonth.from(since)
                val end = YearMonth.from(today)
                while (!m.isAfter(end)) {
                    months.add(m)
                    m = m.plusMonths(1)
                }
            }
            months
        }
        return perGoal.reduce { a, b -> a intersect b }
    }
}
