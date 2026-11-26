package es.ahorra.core

import java.security.GeneralSecurityException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SecurityCryptoTest {

    private val data = """{"goals":[{"name":"Viaje","targetMinor":300000}]}""".toByteArray(Charsets.UTF_8)
    private val password = "correct horse".toCharArray()
    private val fast = PasswordCrypto.MIN_ITERATIONS

    // ---- Copias con contraseña -------------------------------------------------------------------

    @Test
    fun passwordRoundTripWorksWithUnicodePasswords() {
        val encrypted = PasswordCrypto.encrypt(data, password)
        assertTrue(PasswordCrypto.isEncrypted(encrypted))
        assertFalse(PasswordCrypto.isEncrypted(data))
        assertContentEquals(data, PasswordCrypto.decrypt(encrypted, password))

        val unicode = "contraseña-ñ-€-9".toCharArray()
        assertContentEquals(data, PasswordCrypto.decrypt(PasswordCrypto.encrypt(data, unicode, fast), unicode))
    }

    @Test
    fun theEncryptedFileHidesTheContent() {
        val text = String(PasswordCrypto.encrypt(data, password, fast), Charsets.ISO_8859_1)
        assertFalse(text.contains("Viaje"))
        assertFalse(text.contains("goals"))
    }

    @Test
    fun wrongPasswordIsRejected() {
        val encrypted = PasswordCrypto.encrypt(data, password, fast)
        assertFailsWith<GeneralSecurityException> { PasswordCrypto.decrypt(encrypted, "wrong password".toCharArray()) }
        assertFailsWith<GeneralSecurityException> { PasswordCrypto.decrypt(encrypted, "correct horsE".toCharArray()) }
    }

    @Test
    fun tamperingWithAnyPartIsDetected() {
        val encrypted = PasswordCrypto.encrypt(data, password, fast)
        val probes = listOf(0, 5, 11, 14, 15, 20, 31, 32, 40, 43, 44, encrypted.size / 2, encrypted.size - 1)
        for (i in probes) {
            val tampered = encrypted.copyOf()
            tampered[i] = (tampered[i].toInt() xor 0x01).toByte()
            assertFailsWith<GeneralSecurityException>("byte $i") { PasswordCrypto.decrypt(tampered, password) }
        }
    }

    @Test
    fun truncatedAndForeignFilesAreRejected() {
        val encrypted = PasswordCrypto.encrypt(data, password, fast)
        assertFailsWith<GeneralSecurityException> { PasswordCrypto.decrypt(ByteArray(0), password) }
        assertFailsWith<GeneralSecurityException> { PasswordCrypto.decrypt(data, password) }
        assertFailsWith<GeneralSecurityException> { PasswordCrypto.decrypt(encrypted.copyOf(10), password) }
        assertFailsWith<GeneralSecurityException> { PasswordCrypto.decrypt(encrypted.copyOf(encrypted.size - 1), password) }
    }

    @Test
    fun hostileIterationCountsAreRefusedWithoutHangingTheApp() {
        val encrypted = PasswordCrypto.encrypt(data, password, fast)
        val hostile = encrypted.copyOf()
        hostile[11] = 0x7F; hostile[12] = 0xFF.toByte(); hostile[13] = 0xFF.toByte(); hostile[14] = 0xFF.toByte()
        assertFailsWith<GeneralSecurityException> { PasswordCrypto.decrypt(hostile, password) }
        val tiny = encrypted.copyOf()
        tiny[11] = 0; tiny[12] = 0; tiny[13] = 0; tiny[14] = 1
        assertFailsWith<GeneralSecurityException> { PasswordCrypto.decrypt(tiny, password) }
    }

    @Test
    fun shortPasswordsAndBadParametersAreRefused() {
        assertFailsWith<IllegalArgumentException> { PasswordCrypto.encrypt(data, "short".toCharArray()) }
        assertFailsWith<IllegalArgumentException> { PasswordCrypto.encrypt(data, password, iterations = 10) }
    }

    @Test
    fun everyEncryptionUsesFreshSaltAndIv() {
        val a = PasswordCrypto.encrypt(data, password, fast)
        val b = PasswordCrypto.encrypt(data, password, fast)
        assertNotEquals(a.toList(), b.toList())
        assertNotEquals(a.copyOfRange(15, 31).toList(), b.copyOfRange(15, 31).toList()) // sal
    }

    // ---- PIN ----------------------------------------------------------------------------------------------

    @Test
    fun pinFormatAndWeakPinsAreChecked() {
        assertTrue(PinPolicy.isValidFormat("482915"))
        assertFalse(PinPolicy.isValidFormat("12345"))
        assertFalse(PinPolicy.isValidFormat("1234567"))
        assertFalse(PinPolicy.isValidFormat("12a456"))
        listOf("000000", "111111", "123456", "234567", "654321", "987654").forEach { assertTrue(PinPolicy.isWeak(it), it) }
        listOf("482915", "135790", "121212", "100000").forEach { assertFalse(PinPolicy.isWeak(it), it) }
    }

    @Test
    fun pinIsStoredAsSaltedHashAndVerified() {
        val record = PinPolicy.create("482915")
        assertTrue(PinPolicy.verify("482915", record))
        assertFalse(PinPolicy.verify("482916", record))
        assertFalse(PinPolicy.verify("48291", record))
        assertFalse(PinPolicy.verify("", record))
        assertFalse(String(record.hash, Charsets.ISO_8859_1).contains("482915"))

        val other = PinPolicy.create("482915")
        assertNotEquals(record.salt.toList(), other.salt.toList())
        assertNotEquals(record.hash.toList(), other.hash.toList())
        assertFailsWith<IllegalArgumentException> { PinPolicy.create("abc") }
    }

    @Test
    fun lockoutEscalatesEveryFiveFailuresAndIsCapped() {
        var s = LockoutState()
        val now = 1_000_000L
        repeat(4) { s = Lockout.onFailure(s, now) }
        assertEquals(0L, Lockout.remainingMillis(s, now))
        assertEquals(1, Lockout.attemptsLeftInRound(s))

        s = Lockout.onFailure(s, now) // 5.º fallo
        assertEquals(30_000L, Lockout.remainingMillis(s, now))
        repeat(5) { s = Lockout.onFailure(s, now) } // 10.º
        assertEquals(60_000L, Lockout.remainingMillis(s, now))
        repeat(5) { s = Lockout.onFailure(s, now) } // 15.º
        assertEquals(120_000L, Lockout.remainingMillis(s, now))
        repeat(60) { s = Lockout.onFailure(s, now) }
        assertEquals(Lockout.MAX_MILLIS, Lockout.remainingMillis(s, now))
    }

    @Test
    fun lockoutExpiresResetsAndCannotBeInflatedByAMovedClock() {
        var s = LockoutState()
        repeat(5) { s = Lockout.onFailure(s, 0L) }
        assertEquals(10_000L, Lockout.remainingMillis(s, 20_000L))
        assertEquals(0L, Lockout.remainingMillis(s, 30_000L))
        assertEquals(0L, Lockout.remainingMillis(s, 99_999_999L))
        // Reloj retrasado años: la espera mostrada nunca supera el máximo.
        assertEquals(Lockout.MAX_MILLIS, Lockout.remainingMillis(LockoutState(5, Long.MAX_VALUE / 2), 0L))
        assertEquals(LockoutState(), Lockout.onSuccess())
    }
}
