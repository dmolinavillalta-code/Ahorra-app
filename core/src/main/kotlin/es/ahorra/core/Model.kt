package es.ahorra.core

import java.time.LocalDate

enum class GoalStatus { ACTIVE, PAUSED, COMPLETED }

enum class ContributionKind { REGULAR, EXTRA }

/** Colores de acento. Los desbloqueables son solo cosméticos y se obtienen por constancia (nivel). */
enum class Accent(val unlockLevel: Int) {
    TEAL(1), INDIGO(2), AMBER(3), ROSE(4);

    companion object {
        fun unlockedAt(level: Int): List<Accent> = entries.filter { it.unlockLevel <= level }
    }
}

object Limits {
    const val MAX_TARGET_MINOR = 100_000_000_000L
    const val MAX_CONTRIBUTION_MINOR = 100_000_000_000L
    const val MAX_MONTHS = 600
    const val MAX_NAME = 60
    const val MAX_NOTE = 140
    const val MAX_GOALS = 100
    const val MAX_CONTRIBUTIONS = 20_000
    val MIN_DATE: LocalDate = LocalDate.of(2000, 1, 1)
}

/**
 * Un objetivo de ahorro. El plan es "(objetivo - ahorrado al empezar el plan)" repartido en [months] cuotas.
 * [planBaseSavedMinor] es lo ahorrado cuando se (re)calculó el plan.
 */
data class Goal(
    val id: String,
    val name: String,
    val targetMinor: Long,
    val currencyCode: String,
    val startDate: LocalDate,
    val months: Int,
    val paymentDay: Int,
    val planBaseSavedMinor: Long = 0,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val pausedSince: LocalDate? = null,
    /** Meses (formato yyyy-MM) en los que el objetivo estuvo en pausa; no rompen la racha. */
    val pausedMonths: List<String> = emptyList(),
)

/** Solo existen aportaciones CONFIRMADAS por la persona: lo pendiente o simulado nunca se guarda como ahorro. */
data class Contribution(
    val id: String,
    val goalId: String,
    val amountMinor: Long,
    val date: LocalDate,
    val kind: ContributionKind = ContributionKind.REGULAR,
    val note: String = "",
)

data class Settings(
    val remindersEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val accent: Accent = Accent.TEAL,
)

data class AppState(
    val version: Int = CURRENT_VERSION,
    val goals: List<Goal> = emptyList(),
    val contributions: List<Contribution> = emptyList(),
    val settings: Settings = Settings(),
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}
