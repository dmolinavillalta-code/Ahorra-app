package es.ahorra.app.data

import es.ahorra.core.Lockout
import es.ahorra.core.PinPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Qué se ve al abrir la app. */
sealed interface Gate {
    data object Loading : Gate
    data object Open : Gate
    data object Locked : Gate

    /** El archivo de bloqueo no se puede leer: no se abre solo; la salida es empezar de nuevo. */
    data object Broken : Gate
}

sealed interface PinResult {
    data object Success : PinResult
    data class Wrong(val attemptsLeft: Int) : PinResult
    data class LockedOut(val remainingMillis: Long) : PinResult
}

/**
 * Estado del bloqueo de la app. Es una barrera de interfaz: protege frente a quien coge el móvil desbloqueado.
 * Un reinicio de la app (o de la clave) con PIN activo siempre arranca bloqueado.
 */
class LockController(
    private val store: LockStore,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    private val mutex = Mutex()
    private val _config = MutableStateFlow(LockConfig())
    val config: StateFlow<LockConfig> = _config.asStateFlow()
    private val _gate = MutableStateFlow<Gate>(Gate.Loading)
    val gate: StateFlow<Gate> = _gate.asStateFlow()

    @Volatile private var skipNextLock = false
    @Volatile private var stoppedAtElapsed = 0L

    suspend fun load() = mutex.withLock {
        when (val loaded = withContext(Dispatchers.IO) { store.load() }) {
            LockLoad.Empty -> {
                _config.value = LockConfig()
                _gate.value = Gate.Open
            }
            is LockLoad.Data -> {
                _config.value = loaded.config
                _gate.value = if (loaded.config.pinEnabled) Gate.Locked else Gate.Open
            }
            LockLoad.Unreadable -> _gate.value = Gate.Broken
        }
    }

    /** Comprueba el PIN respetando el bloqueo progresivo. Si es correcto, abre la app. */
    suspend fun tryPin(pin: String): PinResult = mutex.withLock {
        val result = verifyLocked(pin)
        if (result is PinResult.Success) _gate.value = Gate.Open
        result
    }

    /** Comprueba el PIN actual sin cambiar la pantalla (para cambiarlo o quitarlo). */
    suspend fun checkPin(pin: String): PinResult = mutex.withLock { verifyLocked(pin) }

    suspend fun setPin(pin: String): Boolean = mutex.withLock {
        if (!PinPolicy.isValidFormat(pin) || PinPolicy.isWeak(pin)) return@withLock false
        val record = withContext(Dispatchers.Default) { PinPolicy.create(pin) }
        update(_config.value.copy(pin = record, lockout = Lockout.onSuccess()))
        _gate.value = Gate.Open
        true
    }

    /** Quita el PIN (también la huella, que depende de él). Hay que haber comprobado el PIN antes. */
    suspend fun removePin() = mutex.withLock {
        update(_config.value.copy(pin = null, biometric = false, lockout = Lockout.onSuccess()))
        _gate.value = Gate.Open
    }

    /** Desbloqueo tras una huella válida. Solo vale si el usuario la activó y hay PIN. */
    suspend fun unlockWithBiometric() = mutex.withLock {
        val cfg = _config.value
        if (cfg.pinEnabled && cfg.biometric) {
            update(cfg.copy(lockout = Lockout.onSuccess()))
            _gate.value = Gate.Open
        }
    }

    suspend fun setBiometric(enabled: Boolean) = mutex.withLock {
        val cfg = _config.value
        if (cfg.pinEnabled) update(cfg.copy(biometric = enabled))
    }

    suspend fun setAutoLockSeconds(seconds: Int) = mutex.withLock {
        if (seconds in LockConfig.AUTO_LOCK_OPTIONS) update(_config.value.copy(autoLockSeconds = seconds))
    }

    suspend fun setHideContent(hide: Boolean) = mutex.withLock { update(_config.value.copy(hideContent = hide)) }

    fun lockNow() {
        if (_config.value.pinEnabled && _gate.value == Gate.Open) _gate.value = Gate.Locked
    }

    /** Llamar justo antes de abrir un selector del sistema (archivos, permisos) para que volver no bloquee. */
    fun skipNextAutoLock() {
        skipNextLock = true
    }

    fun onStopped(elapsedRealtimeMillis: Long) {
        stoppedAtElapsed = elapsedRealtimeMillis
    }

    fun onStarted(elapsedRealtimeMillis: Long) {
        val stoppedAt = stoppedAtElapsed
        stoppedAtElapsed = 0L
        if (skipNextLock) {
            skipNextLock = false
            return
        }
        val cfg = _config.value
        if (cfg.pinEnabled && _gate.value == Gate.Open && stoppedAt > 0L &&
            elapsedRealtimeMillis - stoppedAt >= cfg.autoLockSeconds * 1000L
        ) {
            _gate.value = Gate.Locked
        }
    }

    /** Se llama al borrar todos los datos (ya fuera del hilo principal): vuelve a un estado limpio y abierto. */
    fun wipe() {
        store.delete()
        _config.value = LockConfig()
        _gate.value = Gate.Open
    }

    private suspend fun verifyLocked(pin: String): PinResult {
        val cfg = _config.value
        val record = cfg.pin ?: return PinResult.Success
        val now = nowMillis()
        val remaining = Lockout.remainingMillis(cfg.lockout, now)
        if (remaining > 0) return PinResult.LockedOut(remaining)
        val ok = withContext(Dispatchers.Default) { PinPolicy.verify(pin, record) }
        if (ok) {
            update(cfg.copy(lockout = Lockout.onSuccess()))
            return PinResult.Success
        }
        val lockout = Lockout.onFailure(cfg.lockout, now)
        update(cfg.copy(lockout = lockout))
        val left = Lockout.remainingMillis(lockout, now)
        return if (left > 0) PinResult.LockedOut(left) else PinResult.Wrong(Lockout.attemptsLeftInRound(lockout))
    }

    /** El estado en memoria manda: si falla el guardado, el bloqueo sigue aplicándose en esta sesión. */
    private suspend fun update(config: LockConfig) {
        _config.value = config
        runCatching { withContext(Dispatchers.IO) { store.save(config) } }
    }
}
