package es.ahorra.app.ui

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import es.ahorra.app.R
import es.ahorra.core.Accent
import es.ahorra.core.Achievement
import es.ahorra.core.ChallengeType
import es.ahorra.core.MoneyMath
import es.ahorra.core.Rarity
import es.ahorra.core.Species
import es.ahorra.core.Reason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

fun money(minor: Long, currencyCode: String): String = MoneyMath.format(minor, currencyCode, Locale.getDefault())

fun LocalDate.pretty(): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).format(this)

/** Moneda por defecto según la región del dispositivo; EUR si no se puede determinar. */
fun defaultCurrency(): String {
    val code = runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull()
    return if (code != null && MoneyMath.isValidCurrency(code)) code else "EUR"
}

/** Saludo según la hora del día. */
@StringRes
fun greetingRes(time: LocalTime = LocalTime.now()): Int = when (time.hour) {
    in 6..11 -> R.string.greeting_morning
    in 12..19 -> R.string.greeting_afternoon
    in 20..23 -> R.string.greeting_evening
    else -> R.string.greeting_night
}

fun goalEmoji(key: String): String = when (key) {
    "phone" -> "📱"
    "plane" -> "✈️"
    "car" -> "🚗"
    "home" -> "🏠"
    "laptop" -> "💻"
    "gift" -> "🎁"
    "heart" -> "❤️"
    "school" -> "🎓"
    "ring" -> "💍"
    "bike" -> "🚲"
    "shield" -> "🛡️"
    "music" -> "🎵"
    "game" -> "🎮"
    "sun" -> "☀️"
    else -> "🎯"
}

fun Achievement.emoji(): String = when (this) {
    Achievement.FIRST_GOAL -> "🌱"
    Achievement.FIRST_CONTRIBUTION -> "🪙"
    Achievement.EXTRA_CONTRIBUTION -> "⚡"
    Achievement.STREAK_3 -> "🔥"
    Achievement.STREAK_6 -> "🚀"
    Achievement.STREAK_12 -> "👑"
    Achievement.PERCENT_25 -> "🌤️"
    Achievement.HALFWAY -> "⛰️"
    Achievement.PERCENT_75 -> "🏔️"
    Achievement.GOAL_COMPLETED -> "🏆"
    Achievement.TEN_CONTRIBUTIONS -> "🔟"
    Achievement.TWO_GOALS -> "🎯"
    Achievement.CHALLENGE_DONE -> "🎖️"
}

@StringRes
fun ChallengeType.titleRes(): Int = when (this) {
    ChallengeType.EXTRA -> R.string.challenge_extra_title
    ChallengeType.THREE -> R.string.challenge_three_title
    ChallengeType.EARLY -> R.string.challenge_early_title
    ChallengeType.WEEKS -> R.string.challenge_weeks_title
}

@StringRes
fun ChallengeType.descRes(): Int = when (this) {
    ChallengeType.EXTRA -> R.string.challenge_extra_desc
    ChallengeType.THREE -> R.string.challenge_three_desc
    ChallengeType.EARLY -> R.string.challenge_early_desc
    ChallengeType.WEEKS -> R.string.challenge_weeks_desc
}

fun ChallengeType.emoji(): String = when (this) {
    ChallengeType.EXTRA -> "⚡"
    ChallengeType.THREE -> "🎯"
    ChallengeType.EARLY -> "🌅"
    ChallengeType.WEEKS -> "📆"
}

@StringRes
fun levelNameRes(level: Int): Int = when (level) {
    1 -> R.string.level_name_1
    2 -> R.string.level_name_2
    3 -> R.string.level_name_3
    4 -> R.string.level_name_4
    5 -> R.string.level_name_5
    6 -> R.string.level_name_6
    7 -> R.string.level_name_7
    else -> R.string.level_name_8
}

@StringRes
fun Reason.messageRes(): Int = when (this) {
    Reason.INVALID_NAME -> R.string.error_invalid_name
    Reason.INVALID_TARGET, Reason.INVALID_CURRENCY -> R.string.error_invalid_target
    Reason.INVALID_MONTHS -> R.string.error_invalid_months
    Reason.INVALID_DAY -> R.string.error_invalid_day
    Reason.INVALID_AMOUNT -> R.string.error_invalid_amount
    Reason.INVALID_DATE -> R.string.error_invalid_date
    Reason.GOAL_COMPLETED -> R.string.error_goal_completed
    Reason.STORAGE_ERROR -> R.string.error_storage
    else -> R.string.error_generic
}

@StringRes
fun Achievement.labelRes(): Int = when (this) {
    Achievement.FIRST_GOAL -> R.string.ach_first_goal
    Achievement.FIRST_CONTRIBUTION -> R.string.ach_first_contribution
    Achievement.EXTRA_CONTRIBUTION -> R.string.ach_extra
    Achievement.STREAK_3 -> R.string.ach_streak_3
    Achievement.STREAK_6 -> R.string.ach_streak_6
    Achievement.STREAK_12 -> R.string.ach_streak_12
    Achievement.PERCENT_25 -> R.string.ach_percent_25
    Achievement.HALFWAY -> R.string.ach_halfway
    Achievement.PERCENT_75 -> R.string.ach_percent_75
    Achievement.GOAL_COMPLETED -> R.string.ach_completed
    Achievement.TEN_CONTRIBUTIONS -> R.string.ach_ten
    Achievement.TWO_GOALS -> R.string.ach_two_goals
    Achievement.CHALLENGE_DONE -> R.string.ach_challenge
}

@StringRes
fun Accent.labelRes(): Int = when (this) {
    Accent.TEAL -> R.string.accent_teal
    Accent.INDIGO -> R.string.accent_indigo
    Accent.AMBER -> R.string.accent_amber
    Accent.ROSE -> R.string.accent_rose
    Accent.EMERALD -> R.string.accent_emerald
    Accent.VIOLET -> R.string.accent_violet
}

fun Species.emoji(): String = when (id) {
    "chick" -> "🐥"
    "bunny" -> "🐰"
    "kitten" -> "🐱"
    "puppy" -> "🐶"
    "hamster" -> "🐹"
    "froglet" -> "🐸"
    "fox" -> "🦊"
    "panda" -> "🐼"
    "koala" -> "🐨"
    "owl" -> "🦉"
    "penguin" -> "🐧"
    "unicorn" -> "🦄"
    "octopus" -> "🐙"
    "peacock" -> "🦚"
    "wolf" -> "🐺"
    "lion" -> "🦁"
    "dragon" -> "🐉"
    else -> "🐋"
}

@StringRes
fun Species.nameRes(): Int = when (id) {
    "chick" -> R.string.pet_chick
    "bunny" -> R.string.pet_bunny
    "kitten" -> R.string.pet_kitten
    "puppy" -> R.string.pet_puppy
    "hamster" -> R.string.pet_hamster
    "froglet" -> R.string.pet_froglet
    "fox" -> R.string.pet_fox
    "panda" -> R.string.pet_panda
    "koala" -> R.string.pet_koala
    "owl" -> R.string.pet_owl
    "penguin" -> R.string.pet_penguin
    "unicorn" -> R.string.pet_unicorn
    "octopus" -> R.string.pet_octopus
    "peacock" -> R.string.pet_peacock
    "wolf" -> R.string.pet_wolf
    "lion" -> R.string.pet_lion
    "dragon" -> R.string.pet_dragon
    else -> R.string.pet_whale
}

@StringRes
fun Rarity.labelRes(): Int = when (this) {
    Rarity.COMMON -> R.string.rarity_common
    Rarity.RARE -> R.string.rarity_rare
    Rarity.EPIC -> R.string.rarity_epic
    Rarity.LEGENDARY -> R.string.rarity_legendary
}

@StringRes
fun Rarity.howRes(): Int = when (this) {
    Rarity.COMMON -> R.string.rarity_how_common
    Rarity.RARE -> R.string.rarity_how_rare
    Rarity.EPIC -> R.string.rarity_how_epic
    Rarity.LEGENDARY -> R.string.rarity_how_legendary
}

private const val MAX_IMPORT_BYTES = 10 * 1024 * 1024

/** Lee un archivo elegido por la persona (selector del sistema, sin permisos). null si falla o es demasiado grande. */
suspend fun readBytesLimited(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_IMPORT_BYTES) return@runCatching null
                out.write(buffer, 0, read)
            }
            out.toByteArray()
        }
    }.getOrNull()
}

suspend fun writeBytes(context: Context, uri: Uri, bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val stream = context.contentResolver.openOutputStream(uri) ?: return@runCatching false
        stream.use { it.write(bytes) }
        true
    }.getOrDefault(false)
}
