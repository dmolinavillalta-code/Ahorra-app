package es.ahorra.core

import java.time.LocalDate

data class CurrencySummary(
    val currencyCode: String,
    val savedMinor: Long,
    val targetMinor: Long,
    /** Suma de la cuota mensual de los objetivos activos (no se mezclan monedas). */
    val monthlyMinor: Long,
    val goals: Int,
    val activeGoals: Int,
)

data class SeriesPoint(val date: LocalDate, val savedMinor: Long)

/** Evolución real frente al plan de un objetivo, lista para dibujar. */
data class GoalSeries(val actual: List<SeriesPoint>, val plan: List<SeriesPoint>)

object Summary {

    /** Totales por moneda, en el orden en que aparece cada moneda. Nunca se suman monedas distintas. */
    fun byCurrency(state: AppState): List<CurrencySummary> =
        state.goals.groupBy { it.currencyCode }.map { (code, goals) ->
            CurrencySummary(
                currencyCode = code,
                savedMinor = goals.sumOf { Ledger.savedMinor(state, it.id) },
                targetMinor = goals.sumOf { it.targetMinor },
                monthlyMinor = goals
                    .filter { it.status == GoalStatus.ACTIVE }
                    .sumOf { goal ->
                        val saved = Ledger.savedMinor(state, goal.id)
                        PlanCalculator.schedule(goal)
                            .firstOrNull { !PlanCalculator.isPaid(it, goal, saved) }
                            ?.amountMinor ?: 0L
                    },
                goals = goals.size,
                activeGoals = goals.count { it.status == GoalStatus.ACTIVE },
            )
        }
}

object ProgressSeries {

    /**
     * "actual": ahorro acumulado tras cada día con aportaciones (prolongado hasta hoy).
     * "plan": línea recta por las cuotas previstas, partiendo de lo ahorrado al empezar el plan.
     */
    fun build(goal: Goal, contributions: List<Contribution>, today: LocalDate): GoalSeries {
        val mine = contributions.filter { it.goalId == goal.id }.sortedBy { it.date }
        val origin = listOfNotNull(goal.startDate, mine.firstOrNull()?.date).min()

        val actual = mutableListOf(SeriesPoint(origin, 0L))
        var running = 0L
        for ((date, group) in mine.groupBy { it.date }.toSortedMap()) {
            running += group.sumOf { it.amountMinor }
            actual.add(SeriesPoint(date, running))
        }
        if (today.isAfter(actual.last().date)) actual.add(SeriesPoint(today, running))

        val schedule = PlanCalculator.schedule(goal)
        val plan = if (schedule.isEmpty()) {
            emptyList()
        } else {
            listOf(SeriesPoint(goal.startDate, goal.planBaseSavedMinor)) +
                schedule.map { SeriesPoint(it.dueDate, goal.planBaseSavedMinor + it.cumulativeMinor) }
        }
        return GoalSeries(actual, plan)
    }
}
