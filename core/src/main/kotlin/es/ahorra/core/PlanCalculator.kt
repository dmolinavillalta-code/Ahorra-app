package es.ahorra.core

import java.time.LocalDate
import java.time.YearMonth

data class PlanInstallment(
    val index: Int,
    val dueDate: LocalDate,
    val amountMinor: Long,
    /** Importe acumulado que debe estar ahorrado (desde el inicio del plan) al cumplirse esta cuota. */
    val cumulativeMinor: Long,
)

data class NextPayment(
    val installment: PlanInstallment,
    /** Lo que falta para dar esta cuota por cubierta (tiene en cuenta aportaciones extra y parciales). */
    val outstandingMinor: Long,
    val overdue: Boolean,
)

object PlanCalculator {

    /**
     * Fechas de pago mensuales. El día se ajusta al último día de los meses cortos (31 -> 28/29/30).
     * Si el día de pago de este mes ya pasó respecto a [startDate], el primer pago es el mes siguiente.
     */
    fun dueDates(startDate: LocalDate, months: Int, paymentDay: Int): List<LocalDate> {
        require(months >= 1) { "months >= 1" }
        require(paymentDay in 1..31) { "paymentDay en 1..31" }
        fun dueIn(ym: YearMonth): LocalDate = ym.atDay(minOf(paymentDay, ym.lengthOfMonth()))
        val startMonth = YearMonth.from(startDate)
        val first = if (dueIn(startMonth).isBefore(startDate)) startMonth.plusMonths(1) else startMonth
        return List(months) { dueIn(first.plusMonths(it.toLong())) }
    }

    fun buildSchedule(remainingMinor: Long, startDate: LocalDate, months: Int, paymentDay: Int): List<PlanInstallment> {
        if (remainingMinor <= 0) return emptyList()
        val dates = dueDates(startDate, months, paymentDay)
        val amounts = MoneyMath.splitEvenly(remainingMinor, months)
        var cumulative = 0L
        return amounts.mapIndexed { i, amount ->
            cumulative += amount
            PlanInstallment(index = i + 1, dueDate = dates[i], amountMinor = amount, cumulativeMinor = cumulative)
        }
    }

    fun schedule(goal: Goal): List<PlanInstallment> =
        buildSchedule(goal.targetMinor - goal.planBaseSavedMinor, goal.startDate, goal.months, goal.paymentDay)

    /** Una cuota está cubierta cuando lo ahorrado desde el inicio del plan alcanza su importe acumulado. */
    fun isPaid(installment: PlanInstallment, goal: Goal, savedMinor: Long): Boolean =
        savedMinor - goal.planBaseSavedMinor >= installment.cumulativeMinor

    fun unpaidInstallments(goal: Goal, savedMinor: Long): Int =
        schedule(goal).count { !isPaid(it, goal, savedMinor) }

    fun nextPayment(goal: Goal, savedMinor: Long, today: LocalDate): NextPayment? {
        if (savedMinor >= goal.targetMinor) return null
        val progress = savedMinor - goal.planBaseSavedMinor
        val installment = schedule(goal).firstOrNull { it.cumulativeMinor > progress } ?: return null
        return NextPayment(
            installment = installment,
            outstandingMinor = installment.cumulativeMinor - progress,
            overdue = installment.dueDate.isBefore(today),
        )
    }

    fun estimatedCompletion(goal: Goal): LocalDate? = schedule(goal).lastOrNull()?.dueDate

    /** Progreso en centésimas de porcentaje (0..10000), calculado con enteros. */
    fun progressBasisPoints(savedMinor: Long, targetMinor: Long): Int {
        if (targetMinor <= 0 || savedMinor <= 0) return 0
        return (savedMinor * 10_000L / targetMinor).coerceIn(0L, 10_000L).toInt()
    }
}
