package es.ahorra.core

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.IsoFields

/**
 * Retos personales VOLUNTARIOS, de un mes de duración. Sin dinero, sin azar, sin castigos:
 * si no se cumplen, no pasa nada; si se cumplen, suman un logro.
 */
enum class ChallengeType(val id: String, val target: Int) {
    EXTRA("extra", 1),
    THREE("three", 3),
    EARLY("early", 1),
    WEEKS("weeks", 2);

    companion object {
        fun fromId(id: String): ChallengeType? = entries.firstOrNull { it.id == id }
    }
}

data class ChallengeProgress(val type: ChallengeType, val month: YearMonth, val current: Int) {
    val target: Int get() = type.target
    val completed: Boolean get() = current >= type.target
    val fraction: Float get() = (current.toFloat() / type.target).coerceIn(0f, 1f)
}

object Challenges {

    fun progress(state: AppState, type: ChallengeType, month: YearMonth): ChallengeProgress {
        val inMonth = state.contributions.filter { YearMonth.from(it.date) == month }
        val current = when (type) {
            ChallengeType.EXTRA -> inMonth.count { it.kind == ContributionKind.EXTRA }
            ChallengeType.THREE -> inMonth.size
            ChallengeType.EARLY -> inMonth.count { it.date.dayOfMonth <= 10 }
            ChallengeType.WEEKS -> inMonth.map { weekKey(it.date) }.toSet().size
        }
        return ChallengeProgress(type, month, current)
    }

    /** Retos aceptados para el mes de [today], con su progreso. */
    fun active(state: AppState, today: LocalDate): List<ChallengeProgress> {
        val month = YearMonth.from(today)
        return state.challenges
            .filter { it.month == month.toString() }
            .mapNotNull { entry -> ChallengeType.fromId(entry.challengeId)?.let { progress(state, it, month) } }
    }

    /** Retos que aún no se han aceptado este mes. */
    fun available(state: AppState, today: LocalDate): List<ChallengeType> {
        val accepted = active(state, today).map { it.type }.toSet()
        return ChallengeType.entries.filter { it !in accepted }
    }

    /** Retos aceptados (de cualquier mes) que se cumplieron. */
    fun completedCount(state: AppState): Int = state.challenges.count { entry ->
        val type = ChallengeType.fromId(entry.challengeId)
        val month = runCatching { YearMonth.parse(entry.month) }.getOrNull()
        type != null && month != null && progress(state, type, month).completed
    }

    private fun weekKey(date: LocalDate): Pair<Int, Int> =
        date.get(IsoFields.WEEK_BASED_YEAR) to date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
}
