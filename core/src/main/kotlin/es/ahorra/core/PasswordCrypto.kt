package es.ahorra.core

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cifrado de copias de seguridad con contraseña: PBKDF2-HMAC-SHA256 + AES-256-GCM.
 *
 * Formato: "AHORRA-ENC1" | iteraciones (4 bytes) | sal (16) | IV (12) | texto cifrado + etiqueta.
 * Toda la cabecera se autentica (AAD): alterar un solo byte hace fallar el descifrado.
 * Una contraseña débil sigue siendo débil: se exige una longitud mínima, nada más.
 */
object PasswordCrypto {
    const val MIN_PASSWORD_LENGTH = 8
    const val DEFAULT_ITERATIONS = 210_000
    const val MIN_ITERATIONS = 100_000
    const val MAX_ITERATIONS = 2_000_000

    private val MAGIC = "AHORRA-ENC1".toByteArray(Charsets.US_ASCII)
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val TAG_BITS = 128
    private val HEADER_LENGTH = MAGIC.size + 4 + SALT_LENGTH + IV_LENGTH

    fun isEncrypted(data: ByteArray): Boolean =
        data.size > MAGIC.size && data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    fun encrypt(
        plain: ByteArray,
        password: CharArray,
        iterations: Int = DEFAULT_ITERATIONS,
        random: SecureRandom = SecureRandom(),
    ): ByteArray {
        require(password.size >= MIN_PASSWORD_LENGTH) { "Contraseña demasiado corta" }
        require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "Iteraciones fuera de rango" }
        val salt = ByteArray(SALT_LENGTH).also(random::nextBytes)
        val iv = ByteArray(IV_LENGTH).also(random::nextBytes)
        val header = MAGIC + ByteBuffer.allocate(4).putInt(iterations).array() + salt + iv
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(header)
        return header + cipher.doFinal(plain)
    }

    /** Lanza [GeneralSecurityException] si la contraseña es incorrecta o el contenido fue alterado. */
    fun decrypt(payload: ByteArray, password: CharArray): ByteArray {
        if (!isEncrypted(payload) || payload.size <= HEADER_LENGTH + TAG_BITS / 8 - 1) {
            throw GeneralSecurityException("Formato no válido")
        }
        val iterations = ByteBuffer.wrap(payload, MAGIC.size, 4).int
        if (iterations !in MIN_ITERATIONS..MAX_ITERATIONS) throw GeneralSecurityException("Parámetros no válidos")
        val salt = payload.copyOfRange(MAGIC.size + 4, MAGIC.size + 4 + SALT_LENGTH)
        val iv = payload.copyOfRange(MAGIC.size + 4 + SALT_LENGTH, HEADER_LENGTH)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(payload.copyOfRange(0, HEADER_LENGTH))
        return cipher.doFinal(payload, HEADER_LENGTH, payload.size - HEADER_LENGTH)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
