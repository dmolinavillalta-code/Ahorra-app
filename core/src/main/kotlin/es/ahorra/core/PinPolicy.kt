package es.ahorra.core

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** PIN guardado como sal + hash PBKDF2 (nunca el PIN). */
class PinRecord(val salt: ByteArray, val hash: ByteArray, val iterations: Int)

/**
 * Reglas del PIN de la app. Importante: el PIN es una barrera de la INTERFAZ (impide que otra persona
 * abra la app con el móvil desbloqueado). La protección de los datos en el archivo la da la clave del Keystore.
 */
object PinPolicy {
    const val PIN_LENGTH = 6
    private const val ITERATIONS = 100_000
    private const val SALT_LENGTH = 16

    fun isValidFormat(pin: String): Boolean = pin.length == PIN_LENGTH && pin.all { it in '0'..'9' }

    /** PIN trivial: todos iguales o secuencia ascendente/descendente (111111, 123456, 654321). */
    fun isWeak(pin: String): Boolean {
        if (!isValidFormat(pin)) return true
        if (pin.all { it == pin[0] }) return true
        val digits = pin.map { it - '0' }
        val ascending = digits.zipWithNext().all { (a, b) -> b - a == 1 }
        val descending = digits.zipWithNext().all { (a, b) -> a - b == 1 }
        return ascending || descending
    }

    fun create(pin: String, random: SecureRandom = SecureRandom()): PinRecord {
        require(isValidFormat(pin)) { "Formato de PIN no válido" }
        val salt = ByteArray(SALT_LENGTH).also(random::nextBytes)
        return PinRecord(salt, derive(pin, salt, ITERATIONS), ITERATIONS)
    }

    fun verify(pin: String, record: PinRecord): Boolean {
        if (!isValidFormat(pin)) return false
        return MessageDigest.isEqual(derive(pin, record.salt, record.iterations), record.hash)
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, 256)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}

data class LockoutState(val failedAttempts: Int = 0, val lockedUntilMillis: Long = 0L)

/**
 * Bloqueo progresivo: cada 5 fallos seguidos, espera de 30 s que se duplica (60 s, 2 min…) hasta 15 min.
 * Es una medida contra adivinar el PIN a mano, no contra quien controle el reloj del móvil.
 */
object Lockout {
    const val ATTEMPTS_PER_ROUND = 5
    const val BASE_MILLIS = 30_000L
    const val MAX_MILLIS = 15 * 60 * 1000L

    fun remainingMillis(state: LockoutState, nowMillis: Long): Long =
        (state.lockedUntilMillis - nowMillis).coerceIn(0L, MAX_MILLIS)

    fun attemptsLeftInRound(state: LockoutState): Int = ATTEMPTS_PER_ROUND - state.failedAttempts % ATTEMPTS_PER_ROUND

    fun onFailure(state: LockoutState, nowMillis: Long): LockoutState {
        val attempts = state.failedAttempts + 1
        if (attempts % ATTEMPTS_PER_ROUND != 0) return state.copy(failedAttempts = attempts)
        val round = attempts / ATTEMPTS_PER_ROUND
        val duration = minOf(MAX_MILLIS, BASE_MILLIS * (1L shl minOf(round - 1, 10)))
        return LockoutState(attempts, nowMillis + duration)
    }

    fun onSuccess(): LockoutState = LockoutState()
}
