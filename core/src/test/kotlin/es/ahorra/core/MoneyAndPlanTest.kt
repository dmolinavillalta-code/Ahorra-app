package es.ahorra.core

import java.time.LocalDate
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoneyAndPlanTest {

    // ---- Redondeos y reparto -------------------------------------------------------------

    @Test
    fun splitEvenlyDistributesRemainderToFirstInstallments() {
        assertEquals(listOf(34L, 33L, 33L), MoneyMath.splitEvenly(100, 3))
        assertEquals(listOf(10_000L, 10_000L), MoneyMath.splitEvenly(20_000, 2))
        assertEquals(listOf(1L, 1L, 0L), MoneyMath.splitEvenly(2, 3))
    }

    @Test
    fun splitEvenlyAlwaysSumsExactlyToTheTotal() {
        for (total in 0L..600L) {
            for (parts in 1..24) {
                val split = MoneyMath.splitEvenly(total, parts)
                assertEquals(total, split.sum(), "total=$total parts=$parts")
                assertTrue(split.max() - split.min() <= 1, "total=$total parts=$parts")
            }
        }
    }

    @Test
    fun parseToMinorUnderstandsSpanishAndEnglishInput() {
        assertEquals(150_000L, MoneyMath.parseToMinor("1.500", "EUR"))
        assertEquals(150_025L, MoneyMath.parseToMinor("1.500,25", "EUR"))
        assertEquals(150_050L, MoneyMath.parseToMinor("1,500.50", "EUR"))
        assertEquals(1_250L, MoneyMath.parseToMinor("12,5", "EUR"))
        assertEquals(10_000L, MoneyMath.parseToMinor("100", "EUR"))
        assertEquals(10_000L, MoneyMath.parseToMinor("100.00", "EUR"))
        assertEquals(150_000_000L, MoneyMath.parseToMinor("1.500.000", "EUR"))
        assertEquals(1_500L, MoneyMath.parseToMinor("1500", "JPY"))
    }

    @Test
    fun parseToMinorRejectsGarbageAndTooManyDecimals() {
        assertNull(MoneyMath.parseToMinor("", "EUR"))
        assertNull(MoneyMath.parseToMinor("abc", "EUR"))
        assertNull(MoneyMath.parseToMinor("12,345,6", "EUR"))
        assertNull(MoneyMath.parseToMinor("1,5555", "EUR"))
        assertNull(MoneyMath.parseToMinor("1.5", "JPY"))
        assertNull(MoneyMath.parseToMinor("-5", "EUR"))
        assertNull(MoneyMath.parseToMinor("5,", "EUR"))
        assertNull(MoneyMath.parseToMinor("10", "XXXX"))
        assertNull(MoneyMath.parseToMinor("99999999999999999999", "EUR"))
    }

    @Test
    fun formatUsesLocaleAndCurrencyDigits() {
        val es = MoneyMath.format(150_000, "EUR", Locale.forLanguageTag("es-ES"))
        assertTrue(es.contains("1.500,00") && es.contains("€"), es)
        assertEquals("150.00", MoneyMath.toPlain(15_000, "EUR"))
        assertEquals("1500", MoneyMath.toPlain(1_500, "JPY"))
    }

    // ---- Fechas de pago --------------------------------------------------------------------

    @Test
    fun dueDatesClampToShortMonths() {
        assertEquals(
            listOf(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31)),
            PlanCalculator.dueDates(LocalDate.of(2026, 1, 31), 3, 31),
        )
        assertEquals(
            listOf(LocalDate.of(2028, 1, 30), LocalDate.of(2028, 2, 29)),
            PlanCalculator.dueDates(LocalDate.of(2028, 1, 30), 2, 30),
        )
    }

    @Test
    fun dueDatesStartNextMonthWhenPaymentDayAlreadyPassed() {
        assertEquals(
            listOf(LocalDate.of(2026, 2, 10), LocalDate.of(2026, 3, 10)),
            PlanCalculator.dueDates(LocalDate.of(2026, 1, 15), 2, 10),
        )
        assertEquals(
            listOf(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 15)),
            PlanCalculator.dueDates(LocalDate.of(2026, 1, 15), 2, 15),
        )
    }

    @Test
    fun dueDatesRollOverTheYear() {
        assertEquals(
            listOf(LocalDate.of(2026, 12, 5), LocalDate.of(2027, 1, 5), LocalDate.of(2027, 2, 5)),
            PlanCalculator.dueDates(LocalDate.of(2026, 11, 20), 3, 5),
        )
    }

    // ---- Plan mensual ----------------------------------------------------------------------

    @Test
    fun twelveHundredEurosInTwelveMonthsIsOneHundredPerMonth() {
        val schedule = PlanCalculator.buildSchedule(120_000, LocalDate.of(2026, 1, 1), 12, 5)
        assertEquals(12, schedule.size)
        assertTrue(schedule.all { it.amountMinor == 10_000L })
        assertEquals(120_000L, schedule.last().cumulativeMinor)
        assertEquals(LocalDate.of(2026, 12, 5), schedule.last().dueDate)
    }

    @Test
    fun twelveHundredEurosInEightMonthsIsOneHundredFiftyPerMonth() {
        val schedule = PlanCalculator.buildSchedule(120_000, LocalDate.of(2026, 1, 1), 8, 5)
        assertTrue(schedule.all { it.amountMinor == 15_000L })
    }

    @Test
    fun tenEurosInThreeMonthsKeepsEveryCent() {
        val schedule = PlanCalculator.buildSchedule(1_000, LocalDate.of(2026, 1, 1), 3, 5)
        assertEquals(listOf(334L, 333L, 333L), schedule.map { it.amountMinor })
        assertEquals(1_000L, schedule.last().cumulativeMinor)
    }

    @Test
    fun nextPaymentTracksPartialExtraAndLatePayments() {
        val g = goal()
        val early = LocalDate.of(2026, 1, 3)

        val none = assertNotNull(PlanCalculator.nextPayment(g, 0, early))
        assertEquals(1, none.installment.index)
        assertEquals(10_000L, none.outstandingMinor)
        assertFalse(none.overdue)

        assertEquals(5_000L, PlanCalculator.nextPayment(g, 5_000, early)!!.outstandingMinor)

        val second = PlanCalculator.nextPayment(g, 10_000, early)!!
        assertEquals(2, second.installment.index)
        assertEquals(10_000L, second.outstandingMinor)

        // Aportación extra: se adelantan cuotas futuras.
        val prepaid = PlanCalculator.nextPayment(g, 25_000, early)!!
        assertEquals(3, prepaid.installment.index)
        assertEquals(5_000L, prepaid.outstandingMinor)

        // Pago atrasado.
        assertTrue(PlanCalculator.nextPayment(g, 0, LocalDate.of(2026, 1, 6))!!.overdue)

        // Objetivo cumplido: no hay próximo pago.
        assertNull(PlanCalculator.nextPayment(g, 120_000, early))
    }

    @Test
    fun progressIsComputedWithIntegersAndClamped() {
        assertEquals(0, PlanCalculator.progressBasisPoints(0, 100))
        assertEquals(3_333, PlanCalculator.progressBasisPoints(1, 3))
        assertEquals(5_000, PlanCalculator.progressBasisPoints(60_000, 120_000))
        assertEquals(10_000, PlanCalculator.progressBasisPoints(200, 100))
        assertEquals(0, PlanCalculator.progressBasisPoints(-5, 100))
    }

    @Test
    fun estimatedCompletionIsTheLastInstallmentDate() {
        assertEquals(LocalDate.of(2026, 12, 5), PlanCalculator.estimatedCompletion(goal()))
    }
}
