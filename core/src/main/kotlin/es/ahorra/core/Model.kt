package es.ahorra.core

import java.time.LocalDate

enum class GoalStatus { ACTIVE, PAUSED, COMPLETED }

enum class ContributionKind { REGULAR, EXTRA }

/** Colores de acento. Los desbloqueables son solo cosméticos y se obtienen por constancia (nivel). */
enum class Accent(val unlockLevel: Int) {
    TEAL(1), INDIGO(2), AMBER(3), ROSE(4), EMERALD(5), VIOLET(6);

    companion object {
        fun unlockedAt(level: Int): List<Accent> = entries.filter { it.unlockLevel <= level }
    }
}

/** Iconos disponibles para un objetivo. La interfaz traduce cada clave a un dibujo. */
object GoalIcons {
    const val DEFAULT = "target"
    val all: List<String> = listOf(
        "target", "phone", "plane", "car", "home", "laptop", "gift", "heart",
        "school", "ring", "bike", "shield", "music", "game", "sun",
    )

    fun isValid(key: String): Boolean = key in all
}

object Limits {
    const val MAX_TARGET_MINOR = 100_000_000_000L
    const val MAX_CONTRIBUTION_MINOR = 100_000_000_000L
    const val MAX_MONTHS = 600
    const val MAX_NAME = 60
    const val MAX_DISPLAY_NAME = 30
    const val MAX_NOTE = 140
    const val MAX_GOALS = 100
    const val MAX_CONTRIBUTIONS = 20_000
    const val MAX_CHALLENGES = 240
    const val MAX_PET_NAME = 20
    const val MAX_FRIENDS = 50
    const val MAX_PROFILE_ID = 40
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
    val icon: String = GoalIcons.DEFAULT,
    /** Nombre que la persona le pone a su mascota (opcional). */
    val petName: String = "",
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
    /** Cómo quiere la persona que la llame la app. Solo se guarda en el móvil. */
    val displayName: String = "",
    val welcomeSeen: Boolean = false,
    /** Identificador aleatorio para que un amigo pueda actualizar tu tarjeta sin duplicarla. No es una cuenta. */
    val profileId: String = "",
)

/** Reto personal voluntario aceptado para un mes concreto (formato yyyy-MM). */
data class ActiveChallenge(val challengeId: String, val month: String)

data class AppState(
    val version: Int = CURRENT_VERSION,
    val goals: List<Goal> = emptyList(),
    val contributions: List<Contribution> = emptyList(),
    val settings: Settings = Settings(),
    val challenges: List<ActiveChallenge> = emptyList(),
    /** Especies descubiertas (se conservan aunque se borre el objetivo que las dio). */
    val collection: List<String> = emptyList(),
    /** Tarjetas de amigos importadas a mano: no hay servidor ni cuentas. */
    val friends: List<FriendCard> = emptyList(),
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}
