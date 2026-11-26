package es.ahorra.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class SummaryAndSeriesTest {

    @Test
    fun summaryGroupsByCurrencyAndNeverMixesThem() {
        val g1 = goal(id = "g1", target = 120_000, months = 12)
        val g2 = goal(id = "g2", target = 60_000, months = 6, currency = "USD")
        val g3 = goal(id = "g3", target = 30_000, months = 3).copy(status = GoalStatus.PAUSED)
        var s = AppState(goals = listOf(g1, g2, g3))
        s = Ledger.addContribution(s, contribution("a", 25_000, goalId = "g1"), TODAY).okState()

        val summary = Summary.byCurrency(s)
        assertEquals(listOf("EUR", "USD"), summary.map { it.currencyCode })

        val eur = summary[0]
        assertEquals(25_000L, eur.savedMinor)
        assertEquals(150_000L, eur.targetMinor)
        assertEquals(10_000L, eur.monthlyMinor) // g1: ya cubiertas 2 cuotas; la 3.ª es de 100 €. g3 está en pausa.
        assertEquals(2, eur.goals)
        assertEquals(1, eur.activeGoals)

        assertEquals(10_000L, summary[1].monthlyMinor)
        assertEquals(0L, summary[1].savedMinor)
    }

    @Test
    fun summaryOfNothingIsEmpty() {
        assertEquals(emptyList(), Summary.byCurrency(AppState()))
    }

    @Test
    fun seriesAccumulatesPerDayAndExtendsToToday() {
        val g = goal(target = 30_000, months = 3, day = 5, start = LocalDate.of(2026, 1, 1))
        val contributions = listOf(
            contribution("c1", 10_000, date = LocalDate.of(2026, 1, 5)),
            contribution("c2", 5_000, date = LocalDate.of(2026, 1, 5)),
            contribution("c3", 10_000, date = LocalDate.of(2026, 2, 20)),
        )
        val series = ProgressSeries.build(g, contributions, LocalDate.of(2026, 3, 1))
        assertEquals(
            listOf(
                SeriesPoint(LocalDate.of(2026, 1, 1), 0),
                SeriesPoint(LocalDate.of(2026, 1, 5), 15_000),
                SeriesPoint(LocalDate.of(2026, 2, 20), 25_000),
                SeriesPoint(LocalDate.of(2026, 3, 1), 25_000),
            ),
            series.actual,
        )
        assertEquals(
            listOf(
                SeriesPoint(LocalDate.of(2026, 1, 1), 0),
                SeriesPoint(LocalDate.of(2026, 1, 5), 10_000),
                SeriesPoint(LocalDate.of(2026, 2, 5), 20_000),
                SeriesPoint(LocalDate.of(2026, 3, 5), 30_000),
            ),
            series.plan,
        )
    }

    @Test
    fun seriesWithoutContributionsIsAFlatLine() {
        val g = goal(start = LocalDate.of(2026, 1, 1))
        val series = ProgressSeries.build(g, emptyList(), LocalDate.of(2026, 2, 1))
        assertEquals(listOf(SeriesPoint(LocalDate.of(2026, 1, 1), 0), SeriesPoint(LocalDate.of(2026, 2, 1), 0)), series.actual)
        assertEquals(listOf(SeriesPoint(LocalDate.of(2026, 1, 1), 0)), ProgressSeries.build(g, emptyList(), g.startDate).actual)
    }

    @Test
    fun seriesIgnoresOtherGoalsAndStartsFromThePlanBase() {
        var s = AppState(goals = listOf(goal(id = "g1"), goal(id = "g2")))
        s = Ledger.addContribution(s, contribution("a", 40_000, date = LocalDate.of(2026, 3, 1), goalId = "g1"), TODAY).okState()
        s = Ledger.addContribution(s, contribution("b", 999, date = LocalDate.of(2026, 3, 2), goalId = "g2"), TODAY).okState()
        s = Ledger.updatePlan(s, "g1", "Móvil", 120_000, 4, 5, TODAY).okState()

        val series = ProgressSeries.build(s.goals.first { it.id == "g1" }, s.contributions, TODAY)
        assertEquals(40_000L, series.actual.last().savedMinor)
        assertEquals(40_000L, series.plan.first().savedMinor) // el plan parte de lo ya ahorrado
        assertEquals(120_000L, series.plan.last().savedMinor)
    }
}
