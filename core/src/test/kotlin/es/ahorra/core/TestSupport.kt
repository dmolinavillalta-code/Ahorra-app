package es.ahorra.core

import java.time.LocalDate

internal val TODAY: LocalDate = LocalDate.of(2026, 3, 10)

internal fun goal(
    id: String = "g1",
    target: Long = 120_000,
    months: Int = 12,
    day: Int = 5,
    start: LocalDate = LocalDate.of(2026, 1, 1),
    currency: String = "EUR",
) = Goal(
    id = id,
    name = "Móvil",
    targetMinor = target,
    currencyCode = currency,
    startDate = start,
    months = months,
    paymentDay = day,
)

internal fun contribution(
    id: String,
    amount: Long,
    date: LocalDate = TODAY,
    goalId: String = "g1",
    kind: ContributionKind = ContributionKind.REGULAR,
) = Contribution(id = id, goalId = goalId, amountMinor = amount, date = date, kind = kind)

internal fun stateWith(g: Goal = goal(), vararg c: Contribution) =
    AppState(goals = listOf(g), contributions = c.toList())

internal fun LedgerResult.okState(): AppState = (this as LedgerResult.Ok).state
