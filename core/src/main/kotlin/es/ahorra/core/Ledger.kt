package es.ahorra.core

import java.time.LocalDate
import java.time.YearMonth

enum class Reason {
    INVALID_NAME, INVALID_TARGET, INVALID_MONTHS, INVALID_DAY, INVALID_CURRENCY,
    INVALID_AMOUNT, INVALID_DATE, INVALID_SETTINGS, UNKNOWN_GOAL, GOAL_COMPLETED,
    LIMIT_REACHED, NOT_READY, STORAGE_ERROR,
}

sealed interface LedgerResult {
    /** [changed] = false cuando la operación ya estaba aplicada (idempotencia: doble toque, reintentos). */
    data class Ok(val state: AppState, val changed: Boolean = true) : LedgerResult
    data class Rejected(val reason: Reason) : LedgerResult
}

/**
 * Reglas de negocio puras (sin Android, sin E/S): cada operación recibe un estado y devuelve otro.
 * Nada aquí mueve dinero; solo registra lo que la persona confirma.
 */
object Ledger {

    fun savedMinor(state: AppState, goalId: String): Long =
        state.contributions.asSequence().filter { it.goalId == goalId }.sumOf { it.amountMinor }

    fun createGoal(state: AppState, goal: Goal): LedgerResult {
        if (state.goals.any { it.id == goal.id }) return LedgerResult.Ok(state, changed = false)
        if (state.goals.size >= Limits.MAX_GOALS) return LedgerResult.Rejected(Reason.LIMIT_REACHED)
        validateGoalFields(goal.name, goal.targetMinor, goal.currencyCode, goal.months, goal.paymentDay)
            ?.let { return LedgerResult.Rejected(it) }
        val clean = goal.copy(
            name = goal.name.trim(),
            planBaseSavedMinor = 0,
            status = GoalStatus.ACTIVE,
            pausedSince = null,
            pausedMonths = emptyList(),
        )
        return LedgerResult.Ok(state.copy(goals = state.goals + clean))
    }

    fun addContribution(state: AppState, contribution: Contribution, today: LocalDate): LedgerResult {
        if (state.contributions.any { it.id == contribution.id }) return LedgerResult.Ok(state, changed = false)
        val goal = state.goals.firstOrNull { it.id == contribution.goalId }
            ?: return LedgerResult.Rejected(Reason.UNKNOWN_GOAL)
        if (goal.status == GoalStatus.COMPLETED) return LedgerResult.Rejected(Reason.GOAL_COMPLETED)
        if (contribution.amountMinor <= 0 || contribution.amountMinor > Limits.MAX_CONTRIBUTION_MINOR) {
            return LedgerResult.Rejected(Reason.INVALID_AMOUNT)
        }
        // Dinero "ahorrado" = ya ocurrido: no se admiten fechas futuras.
        if (contribution.date.isAfter(today) || contribution.date.isBefore(Limits.MIN_DATE)) {
            return LedgerResult.Rejected(Reason.INVALID_DATE)
        }
        if (state.contributions.size >= Limits.MAX_CONTRIBUTIONS) return LedgerResult.Rejected(Reason.LIMIT_REACHED)
        val stored = contribution.copy(note = contribution.note.trim().take(Limits.MAX_NOTE))
        return LedgerResult.Ok(refreshStatus(state.copy(contributions = state.contributions + stored), goal.id))
    }

    fun removeContribution(state: AppState, contributionId: String): LedgerResult {
        val existing = state.contributions.firstOrNull { it.id == contributionId }
            ?: return LedgerResult.Ok(state, changed = false)
        val next = state.copy(contributions = state.contributions.filterNot { it.id == contributionId })
        return LedgerResult.Ok(refreshStatus(next, existing.goalId))
    }

    /** Cambia objetivo, plazo o día: el plan se recalcula desde hoy sobre lo que falta. */
    fun updatePlan(
        state: AppState,
        goalId: String,
        name: String,
        targetMinor: Long,
        months: Int,
        paymentDay: Int,
        today: LocalDate,
    ): LedgerResult {
        val goal = state.goals.firstOrNull { it.id == goalId } ?: return LedgerResult.Rejected(Reason.UNKNOWN_GOAL)
        validateGoalFields(name, targetMinor, goal.currencyCode, months, paymentDay)
            ?.let { return LedgerResult.Rejected(it) }
        val saved = savedMinor(state, goalId)
        val updated = goal.copy(
            name = name.trim(),
            targetMinor = targetMinor,
            months = months,
            paymentDay = paymentDay,
            startDate = today,
            planBaseSavedMinor = saved,
            status = when {
                saved >= targetMinor -> GoalStatus.COMPLETED
                goal.status == GoalStatus.PAUSED -> GoalStatus.PAUSED
                else -> GoalStatus.ACTIVE
            },
        )
        return LedgerResult.Ok(replaceGoal(state, updated))
    }

    fun pauseGoal(state: AppState, goalId: String, today: LocalDate): LedgerResult {
        val goal = state.goals.firstOrNull { it.id == goalId } ?: return LedgerResult.Rejected(Reason.UNKNOWN_GOAL)
        return when (goal.status) {
            GoalStatus.PAUSED -> LedgerResult.Ok(state, changed = false)
            GoalStatus.COMPLETED -> LedgerResult.Rejected(Reason.GOAL_COMPLETED)
            GoalStatus.ACTIVE -> LedgerResult.Ok(
                replaceGoal(state, goal.copy(status = GoalStatus.PAUSED, pausedSince = today)),
            )
        }
    }

    /** Al reanudar, las cuotas aún sin cubrir se reparten desde hoy: el plazo se desplaza, no se castiga. */
    fun resumeGoal(state: AppState, goalId: String, today: LocalDate): LedgerResult {
        val goal = state.goals.firstOrNull { it.id == goalId } ?: return LedgerResult.Rejected(Reason.UNKNOWN_GOAL)
        if (goal.status == GoalStatus.COMPLETED) return LedgerResult.Rejected(Reason.GOAL_COMPLETED)
        if (goal.status == GoalStatus.ACTIVE) return LedgerResult.Ok(state, changed = false)
        val saved = savedMinor(state, goalId)
        val since = goal.pausedSince ?: today
        val newPaused = (goal.pausedMonths + monthsBetween(YearMonth.from(since), YearMonth.from(today)))
            .distinct().sorted()
        val remainingInstallments = PlanCalculator.unpaidInstallments(goal, saved).coerceAtLeast(1)
        val resumed = goal.copy(
            status = GoalStatus.ACTIVE,
            pausedSince = null,
            pausedMonths = newPaused,
            startDate = today,
            planBaseSavedMinor = saved,
            months = remainingInstallments,
        )
        return LedgerResult.Ok(replaceGoal(state, resumed))
    }

    fun deleteGoal(state: AppState, goalId: String): LedgerResult {
        if (state.goals.none { it.id == goalId }) return LedgerResult.Ok(state, changed = false)
        return LedgerResult.Ok(
            state.copy(
                goals = state.goals.filterNot { it.id == goalId },
                contributions = state.contributions.filterNot { it.goalId == goalId },
            ),
        )
    }

    fun updateSettings(state: AppState, settings: Settings): LedgerResult {
        if (settings == state.settings) return LedgerResult.Ok(state, changed = false)
        val level = Gamification.level(state.contributions.size).number
        if (settings.reminderHour !in 0..23 || settings.accent.unlockLevel > level) {
            return LedgerResult.Rejected(Reason.INVALID_SETTINGS)
        }
        return LedgerResult.Ok(state.copy(settings = settings))
    }

    /**
     * Valida un estado importado desde un archivo (que puede venir de otra app, estar corrupto o manipulado).
     * Devuelve null si algo no cuadra; nunca "arregla" datos en silencio.
     */
    fun sanitizeImport(candidate: AppState, today: LocalDate): AppState? {
        if (candidate.version != AppState.CURRENT_VERSION) return null
        if (candidate.goals.size > Limits.MAX_GOALS || candidate.contributions.size > Limits.MAX_CONTRIBUTIONS) return null
        if (candidate.goals.map { it.id }.toSet().size != candidate.goals.size) return null
        if (candidate.contributions.map { it.id }.toSet().size != candidate.contributions.size) return null
        for (g in candidate.goals) {
            if (g.id.isBlank() || g.id.length > 64) return null
            if (validateGoalFields(g.name, g.targetMinor, g.currencyCode, g.months, g.paymentDay) != null) return null
            if (g.planBaseSavedMinor < 0 || g.planBaseSavedMinor > Limits.MAX_TARGET_MINOR) return null
        }
        val goalIds = candidate.goals.map { it.id }.toSet()
        for (c in candidate.contributions) {
            if (c.id.isBlank() || c.id.length > 64) return null
            if (c.goalId !in goalIds) return null
            if (c.amountMinor <= 0 || c.amountMinor > Limits.MAX_CONTRIBUTION_MINOR) return null
            if (c.date.isAfter(today) || c.date.isBefore(Limits.MIN_DATE)) return null
            if (c.note.length > Limits.MAX_NOTE) return null
        }
        if (candidate.settings.reminderHour !in 0..23) return null
        // El estado de cada objetivo se recalcula a partir de sus aportaciones, no se confía en el archivo.
        return candidate.goals.fold(candidate) { acc, g -> refreshStatus(acc, g.id) }
    }

    private fun validateGoalFields(name: String, targetMinor: Long, currency: String, months: Int, day: Int): Reason? =
        when {
            name.trim().isEmpty() || name.trim().length > Limits.MAX_NAME -> Reason.INVALID_NAME
            targetMinor <= 0 || targetMinor > Limits.MAX_TARGET_MINOR -> Reason.INVALID_TARGET
            !MoneyMath.isValidCurrency(currency) -> Reason.INVALID_CURRENCY
            months !in 1..Limits.MAX_MONTHS -> Reason.INVALID_MONTHS
            day !in 1..31 -> Reason.INVALID_DAY
            else -> null
        }

    private fun replaceGoal(state: AppState, goal: Goal): AppState =
        state.copy(goals = state.goals.map { if (it.id == goal.id) goal else it })

    private fun refreshStatus(state: AppState, goalId: String): AppState {
        val goal = state.goals.firstOrNull { it.id == goalId } ?: return state
        val saved = savedMinor(state, goalId)
        val status = when {
            saved >= goal.targetMinor -> GoalStatus.COMPLETED
            goal.status == GoalStatus.COMPLETED -> GoalStatus.ACTIVE
            else -> goal.status
        }
        return if (status == goal.status) state else replaceGoal(state, goal.copy(status = status))
    }

    private fun monthsBetween(from: YearMonth, to: YearMonth): List<String> {
        val result = mutableListOf<String>()
        var m = from
        while (!m.isAfter(to)) {
            result.add(m.toString())
            m = m.plusMonths(1)
        }
        return result
    }
}
