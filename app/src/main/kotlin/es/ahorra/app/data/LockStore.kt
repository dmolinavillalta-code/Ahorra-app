package es.ahorra.app.data

import es.ahorra.core.LockoutState
import es.ahorra.core.PinRecord
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64

/** Configuración del bloqueo de la app. El PIN se guarda solo como sal + hash. */
data class LockConfig(
    val pin: PinRecord? = null,
    val biometric: Boolean = false,
    val autoLockSeconds: Int = DEFAULT_AUTO_LOCK_SECONDS,
    val hideContent: Boolean = false,
    val lockout: LockoutState = LockoutState(),
) {
    val pinEnabled: Boolean get() = pin != null

    companion object {
        const val DEFAULT_AUTO_LOCK_SECONDS = 30
        val AUTO_LOCK_OPTIONS = listOf(0, 30, 300)
    }
}

@Serializable
private data class LockDto(
    val pinSalt: String = "",
    val pinHash: String = "",
    val pinIterations: Int = 0,
    val biometric: Boolean = false,
    val autoLockSeconds: Int = LockConfig.DEFAULT_AUTO_LOCK_SECONDS,
    val hideContent: Boolean = false,
    val failedAttempts: Int = 0,
    val lockedUntilMillis: Long = 0L,
)

sealed interface LockLoad {
    data object Empty : LockLoad
    data class Data(val config: LockConfig) : LockLoad

    /** Hay un archivo de bloqueo pero no se puede leer: no se asume "sin bloqueo". */
    data object Unreadable : LockLoad
}

/** Archivo cifrado propio para el bloqueo (misma clave del Keystore y misma escritura atómica que los datos). */
class LockStore(private val file: File, private val cipher: PayloadCipher) {

    private val tmp = File(file.parentFile, file.name + ".tmp")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(): LockLoad {
        if (!file.exists()) return LockLoad.Empty
        return try {
            val dto = json.decodeFromString(LockDto.serializer(), String(cipher.decrypt(file.readBytes()), Charsets.UTF_8))
            LockLoad.Data(dto.toConfig() ?: return LockLoad.Unreadable)
        } catch (e: Exception) {
            LockLoad.Unreadable
        }
    }

    fun save(config: LockConfig) {
        val bytes = cipher.encrypt(json.encodeToString(LockDto.serializer(), config.toDto()).toByteArray(Charsets.UTF_8))
        FileOutputStream(tmp).use { out ->
            out.write(bytes)
            out.fd.sync()
        }
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    fun delete() {
        file.delete()
        tmp.delete()
    }

    private fun LockConfig.toDto(): LockDto {
        val record = pin
        return LockDto(
            pinSalt = record?.let { Base64.getEncoder().encodeToString(it.salt) } ?: "",
            pinHash = record?.let { Base64.getEncoder().encodeToString(it.hash) } ?: "",
            pinIterations = record?.iterations ?: 0,
            biometric = biometric,
            autoLockSeconds = autoLockSeconds,
            hideContent = hideContent,
            failedAttempts = lockout.failedAttempts,
            lockedUntilMillis = lockout.lockedUntilMillis,
        )
    }

    /** null si el contenido es incoherente (se trata como ilegible). */
    private fun LockDto.toConfig(): LockConfig? {
        val record = if (pinSalt.isEmpty() && pinHash.isEmpty()) {
            null
        } else {
            if (pinIterations !in 100_000..2_000_000) return null
            PinRecord(Base64.getDecoder().decode(pinSalt), Base64.getDecoder().decode(pinHash), pinIterations)
        }
        return LockConfig(
            pin = record,
            biometric = biometric && record != null,
            autoLockSeconds = if (autoLockSeconds in LockConfig.AUTO_LOCK_OPTIONS) autoLockSeconds else LockConfig.DEFAULT_AUTO_LOCK_SECONDS,
            hideContent = hideContent,
            lockout = LockoutState(failedAttempts.coerceAtLeast(0), lockedUntilMillis),
        )
    }
}
