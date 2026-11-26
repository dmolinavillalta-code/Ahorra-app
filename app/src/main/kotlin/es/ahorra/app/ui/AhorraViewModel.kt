package es.ahorra.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.ahorra.app.AhorraApp
import es.ahorra.app.data.Gate
import es.ahorra.app.data.LockConfig
import es.ahorra.app.data.PinResult
import es.ahorra.app.data.RepoState
import es.ahorra.app.work.ReminderScheduler
import es.ahorra.core.AppState
import es.ahorra.core.Ledger
import es.ahorra.core.LedgerResult
import es.ahorra.core.PasswordCrypto
import es.ahorra.core.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException

enum class ImportOutcome { OK, NEEDS_PASSWORD, WRONG_PASSWORD, INVALID }

class AhorraViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AhorraApp
    private val repo = app.repository
    private val lock = app.lockController

    val repoState: StateFlow<RepoState> = repo.state
    val gate: StateFlow<Gate> = lock.gate
    val lockConfig: StateFlow<LockConfig> = lock.config

    /** Aplica una operación del núcleo de forma serializada y persistente; [onResult] recibe el resultado. */
    fun submit(block: (AppState) -> LedgerResult, onResult: (LedgerResult) -> Unit = {}) {
        viewModelScope.launch { onResult(repo.mutate(block)) }
    }

    fun applySettings(settings: Settings) {
        submit({ Ledger.updateSettings(it, settings) }) { result ->
            if (result is LedgerResult.Ok) ReminderScheduler.apply(getApplication<Application>(), result.state.settings)
        }
    }

    // ---- Bloqueo ----------------------------------------------------------------------------------------

    fun tryPin(pin: String, onResult: (PinResult) -> Unit) {
        viewModelScope.launch { onResult(lock.tryPin(pin)) }
    }

    fun checkPin(pin: String, onResult: (PinResult) -> Unit) {
        viewModelScope.launch { onResult(lock.checkPin(pin)) }
    }

    fun setPin(pin: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch { onDone(lock.setPin(pin)) }
    }

    fun removePin() {
        viewModelScope.launch { lock.removePin() }
    }

    fun unlockWithBiometric() {
        viewModelScope.launch { lock.unlockWithBiometric() }
    }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch { lock.setBiometric(enabled) }
    }

    fun setAutoLockSeconds(seconds: Int) {
        viewModelScope.launch { lock.setAutoLockSeconds(seconds) }
    }

    fun setHideContent(hide: Boolean) {
        viewModelScope.launch { lock.setHideContent(hide) }
    }

    fun lockNow() = lock.lockNow()

    /** Antes de abrir un selector del sistema: volver de él no debe bloquear la app. */
    fun skipNextAutoLock() = lock.skipNextAutoLock()

    // ---- Copias de seguridad ------------------------------------------------------------------------------

    /** Con [password] (mínimo 8 caracteres) la copia sale cifrada; sin ella, en JSON legible. */
    suspend fun exportBytes(password: CharArray?): ByteArray = withContext(Dispatchers.Default) {
        val json = repo.exportJson().toByteArray(Charsets.UTF_8)
        if (password == null) json else PasswordCrypto.encrypt(json, password)
    }

    suspend fun importBytes(bytes: ByteArray, password: CharArray?): ImportOutcome {
        val plain = if (PasswordCrypto.isEncrypted(bytes)) {
            if (password == null) return ImportOutcome.NEEDS_PASSWORD
            try {
                withContext(Dispatchers.Default) { PasswordCrypto.decrypt(bytes, password) }
            } catch (e: GeneralSecurityException) {
                return ImportOutcome.WRONG_PASSWORD
            }
        } else {
            bytes
        }
        val text = runCatching { String(plain, Charsets.UTF_8) }.getOrNull() ?: return ImportOutcome.INVALID
        val ok = repo.importJson(text)
        if (ok) {
            (repo.state.value as? RepoState.Ready)?.let {
                ReminderScheduler.apply(getApplication<Application>(), it.data.settings)
            }
        }
        return if (ok) ImportOutcome.OK else ImportOutcome.INVALID
    }

    fun wipe(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.wipe()
            onDone()
        }
    }
}
