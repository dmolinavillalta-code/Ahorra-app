package es.ahorra.core

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Aritmética monetaria en unidades menores enteras (céntimos). Nunca se usa Double/Float para dinero.
 */
object MoneyMath {

    /**
     * Divide [totalMinor] en [parts] cuotas enteras que suman EXACTAMENTE el total.
     * El resto se reparte de 1 en 1 entre las primeras cuotas (p. ej. 100 en 3 -> 34, 33, 33).
     */
    fun splitEvenly(totalMinor: Long, parts: Int): List<Long> {
        require(totalMinor >= 0) { "El importe no puede ser negativo" }
        require(parts > 0) { "El número de cuotas debe ser positivo" }
        val base = totalMinor / parts
        val remainder = (totalMinor % parts).toInt()
        return List(parts) { i -> if (i < remainder) base + 1 else base }
    }

    fun isValidCurrency(code: String): Boolean =
        code.length == 3 && runCatching { Currency.getInstance(code) }.isSuccess

    fun fractionDigits(currencyCode: String): Int =
        Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)

    /**
     * Interpreta texto escrito por una persona ("1.500", "1.500,25", "1500.5", "12,5") y devuelve unidades menores.
     * Devuelve null si el texto no es un importe válido o tiene más decimales de los permitidos.
     */
    fun parseToMinor(input: String, currencyCode: String): Long? {
        if (!isValidCurrency(currencyCode)) return null
        val digits = fractionDigits(currencyCode)
        val s = input.trim().replace(" ", "").replace(" ", "")
        if (s.isEmpty() || !s.all { it.isDigit() || it == '.' || it == ',' }) return null
        if (s.first() == '.' || s.first() == ',' || s.last() == '.' || s.last() == ',') return null

        val lastSep = s.lastIndexOfAny(charArrayOf('.', ','))
        val normalized: String
        if (lastSep == -1) {
            normalized = s
        } else {
            val sepChar = s[lastSep]
            val before = s.substring(0, lastSep)
            val after = s.substring(lastSep + 1)
            val otherSepPresent = s.any { (it == '.' || it == ',') && it != sepChar }
            val sameSepCount = s.count { it == sepChar }
            val isDecimal = when {
                otherSepPresent -> true
                sameSepCount > 1 -> false
                after.length == 3 && digits < 3 -> false
                else -> true
            }
            if (isDecimal) {
                if (after.isEmpty() || after.length > digits) return null
                val intPart = before.filter { it.isDigit() }
                if (intPart.isEmpty()) return null
                normalized = "$intPart.$after"
            } else {
                // Separador de miles: todos los grupos tras el primero deben tener exactamente 3 cifras.
                val groups = s.split(sepChar)
                if (groups.drop(1).any { it.length != 3 } || groups.first().isEmpty()) return null
                normalized = groups.joinToString("")
            }
        }
        return try {
            BigDecimal(normalized).movePointRight(digits).longValueExact()
        } catch (e: ArithmeticException) {
            null
        } catch (e: NumberFormatException) {
            null
        }
    }

    /** Texto editable sin símbolo (para precargar campos): 15000 -> "150.00". */
    fun toPlain(minor: Long, currencyCode: String): String =
        BigDecimal.valueOf(minor, fractionDigits(currencyCode)).toPlainString()

    fun format(minor: Long, currencyCode: String, locale: Locale): String {
        val currency = Currency.getInstance(currencyCode)
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = currency
        return formatter.format(BigDecimal.valueOf(minor, currency.defaultFractionDigits.coerceAtLeast(0)))
    }
}
