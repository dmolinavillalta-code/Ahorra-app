package es.ahorra.app.data

import es.ahorra.core.Accent
import es.ahorra.core.ActiveChallenge
import es.ahorra.core.GoalIcons
import es.ahorra.core.AppState
import es.ahorra.core.Contribution
import es.ahorra.core.ContributionKind
import es.ahorra.core.Goal
import es.ahorra.core.GoalStatus
import es.ahorra.core.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * Formato de almacenamiento y exportación (JSON). Es una capa aparte del dominio para poder
 * evolucionar el archivo (campo `version`) sin tocar las reglas de negocio.
 */
@Serializable
data class GoalDto(
    val id: String,
    val name: String,
    val targetMinor: Long,
    val currencyCode: String,
    val startDate: String,
    val months: Int,
    val paymentDay: Int,
    val planBaseSavedMinor: Long = 0,
    val status: String = "ACTIVE",
    val pausedSince: String? = null,
    val pausedMonths: List<String> = emptyList(),
    val icon: String = GoalIcons.DEFAULT,
)

@Serializable
data class ContributionDto(
    val id: String,
    val goalId: String,
    val amountMinor: Long,
    val date: String,
    val kind: String = "REGULAR",
    val note: String = "",
)

@Serializable
data class SettingsDto(
    val remindersEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val accent: String = "TEAL",
    val displayName: String = "",
    val welcomeSeen: Boolean = false,
)

@Serializable
data class ChallengeDto(val challengeId: String, val month: String)

@Serializable
data class StateDto(
    val version: Int = AppState.CURRENT_VERSION,
    val goals: List<GoalDto> = emptyList(),
    val contributions: List<ContributionDto> = emptyList(),
    val settings: SettingsDto = SettingsDto(),
    val challenges: List<ChallengeDto> = emptyList(),
)

object StorageFormat {
    private const val MAX_CHARS = 20_000_000

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(state: AppState): String = json.encodeToString(StateDto.serializer(), state.toDto())

    /** Devuelve null si el texto no es un estado válido (corrupto, truncado o de otra versión de formato). */
    fun decode(text: String): AppState? {
        if (text.length > MAX_CHARS) return null
        return try {
            json.decodeFromString(StateDto.serializer(), text).toDomain()
        } catch (e: Exception) {
            null
        }
    }
}

internal fun AppState.toDto() = StateDto(
    version = version,
    goals = goals.map {
        GoalDto(
            id = it.id, name = it.name, targetMinor = it.targetMinor, currencyCode = it.currencyCode,
            startDate = it.startDate.toString(), months = it.months, paymentDay = it.paymentDay,
            planBaseSavedMinor = it.planBaseSavedMinor, status = it.status.name,
            pausedSince = it.pausedSince?.toString(), pausedMonths = it.pausedMonths, icon = it.icon,
        )
    },
    contributions = contributions.map {
        ContributionDto(
            id = it.id, goalId = it.goalId, amountMinor = it.amountMinor, date = it.date.toString(),
            kind = it.kind.name, note = it.note,
        )
    },
    settings = SettingsDto(
        settings.remindersEnabled, settings.reminderHour, settings.accent.name,
        settings.displayName, settings.welcomeSeen,
    ),
    challenges = challenges.map { ChallengeDto(it.challengeId, it.month) },
)

internal fun StateDto.toDomain() = AppState(
    version = version,
    goals = goals.map {
        Goal(
            id = it.id, name = it.name, targetMinor = it.targetMinor, currencyCode = it.currencyCode,
            startDate = LocalDate.parse(it.startDate), months = it.months, paymentDay = it.paymentDay,
            planBaseSavedMinor = it.planBaseSavedMinor, status = GoalStatus.valueOf(it.status),
            pausedSince = it.pausedSince?.let(LocalDate::parse), pausedMonths = it.pausedMonths, icon = it.icon,
        )
    },
    contributions = contributions.map {
        Contribution(
            id = it.id, goalId = it.goalId, amountMinor = it.amountMinor, date = LocalDate.parse(it.date),
            kind = ContributionKind.valueOf(it.kind), note = it.note,
        )
    },
    settings = Settings(
        settings.remindersEnabled, settings.reminderHour, Accent.valueOf(settings.accent),
        settings.displayName, settings.welcomeSeen,
    ),
    challenges = challenges.map { ActiveChallenge(it.challengeId, it.month) },
)
