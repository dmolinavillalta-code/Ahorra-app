package es.ahorra.app.ui

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import es.ahorra.app.R
import es.ahorra.core.Accent
import es.ahorra.core.Achievement
import es.ahorra.core.MoneyMath
import es.ahorra.core.Reason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDate
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
    Achievement.HALFWAY -> R.string.ach_halfway
    Achievement.GOAL_COMPLETED -> R.string.ach_completed
}

@StringRes
fun Accent.labelRes(): Int = when (this) {
    Accent.TEAL -> R.string.accent_teal
    Accent.INDIGO -> R.string.accent_indigo
    Accent.AMBER -> R.string.accent_amber
    Accent.ROSE -> R.string.accent_rose
}

private const val MAX_IMPORT_BYTES = 10 * 1024 * 1024

/** Lee un archivo elegido por la persona (selector del sistema, sin permisos). null si falla o es demasiado grande. */
suspend fun readTextLimited(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
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
            out.toString(Charsets.UTF_8.name())
        }
    }.getOrNull()
}

suspend fun writeText(context: Context, uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val stream = context.contentResolver.openOutputStream(uri) ?: return@runCatching false
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        true
    }.getOrDefault(false)
}
