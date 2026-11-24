package es.ahorra.app.data

import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface PayloadCipher {
    fun encrypt(plain: ByteArray): ByteArray

    /** Lanza [GeneralSecurityException] si el contenido fue alterado o la clave no corresponde. */
    fun decrypt(payload: ByteArray): ByteArray
}

/**
 * AES-256-GCM (cifrado autenticado). Formato: [1 byte longitud IV][IV][texto cifrado + etiqueta].
 * El IV lo genera el proveedor criptográfico en cada cifrado (aleatorio con el Keystore); nunca se reutiliza.
 * No depende de Android, para poder probarlo en la JVM.
 */
class AesGcmPayloadCipher(private val keyProvider: () -> SecretKey) : PayloadCipher {

    override fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider())
        cipher.updateAAD(AAD)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain)
        return byteArrayOf(iv.size.toByte()) + iv + encrypted
    }

    override fun decrypt(payload: ByteArray): ByteArray {
        if (payload.isEmpty()) throw GeneralSecurityException("Contenido vacío")
        val ivLength = payload[0].toInt() and 0xFF
        if (ivLength != IV_LENGTH || payload.size <= 1 + ivLength) throw GeneralSecurityException("Formato no válido")
        val iv = payload.copyOfRange(1, 1 + ivLength)
        val encrypted = payload.copyOfRange(1 + ivLength, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keyProvider(), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(AAD)
        return cipher.doFinal(encrypted)
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_BITS = 128
        val AAD = "ahorra:v1".toByteArray(Charsets.UTF_8)
    }
}
