package es.ahorra.app.data

import java.security.GeneralSecurityException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AesGcmPayloadCipherTest {

    private fun newKey(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    private val key = newKey()
    private val cipher = AesGcmPayloadCipher { key }
    private val secret = """{"goals":[{"name":"Móvil","targetMinor":150000}]}""".toByteArray(Charsets.UTF_8)

    @Test
    fun roundTripReturnsTheSameBytes() {
        assertContentEquals(secret, cipher.decrypt(cipher.encrypt(secret)))
    }

    @Test
    fun ciphertextDoesNotContainThePlaintext() {
        val encrypted = String(cipher.encrypt(secret), Charsets.ISO_8859_1)
        assertFalse(encrypted.contains("Móvil"))
        assertFalse(encrypted.contains("goals"))
    }

    @Test
    fun everyEncryptionUsesAFreshIv() {
        val ivs = (1..200).map { cipher.encrypt(secret).copyOfRange(1, 13).toList() }.toSet()
        assertEquals(200, ivs.size)
        assertNotEquals(cipher.encrypt(secret).toList(), cipher.encrypt(secret).toList())
    }

    @Test
    fun anyFlippedByteIsDetected() {
        val encrypted = cipher.encrypt(secret)
        for (i in encrypted.indices) {
            val tampered = encrypted.copyOf()
            tampered[i] = (tampered[i].toInt() xor 0x01).toByte()
            assertFailsWith<GeneralSecurityException>("byte $i") { cipher.decrypt(tampered) }
        }
    }

    @Test
    fun truncatedOrEmptyPayloadIsRejected() {
        val encrypted = cipher.encrypt(secret)
        assertFailsWith<GeneralSecurityException> { cipher.decrypt(ByteArray(0)) }
        assertFailsWith<GeneralSecurityException> { cipher.decrypt(encrypted.copyOf(5)) }
        assertFailsWith<GeneralSecurityException> { cipher.decrypt(encrypted.copyOf(encrypted.size - 1)) }
    }

    @Test
    fun aDifferentKeyCannotDecrypt() {
        val other = AesGcmPayloadCipher { newKey() }
        assertFailsWith<GeneralSecurityException> { other.decrypt(cipher.encrypt(secret)) }
    }

    @Test
    fun emptyPlaintextRoundTrips() {
        assertTrue(cipher.decrypt(cipher.encrypt(ByteArray(0))).isEmpty())
    }
}
