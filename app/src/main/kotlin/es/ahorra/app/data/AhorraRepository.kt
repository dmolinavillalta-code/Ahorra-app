package es.ahorra.app.data

import es.ahorra.core.AppState
import es.ahorra.core.Ledger
import es.ahorra.core.LedgerResult
import es.ahorra.core.Reason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate

sealed interface RepoState {
    data object Loading : RepoState
    data class Ready(val data: AppState) : RepoState
    data object Unreadable : RepoState
}

/**
 * Única fuente de verdad. Todas las escrituras pasan por [mutate]: se serializan con un cerrojo,
 * se validan con las reglas del núcleo y se guardan en disco ANTES de publicarse en pantalla.
 */
class AhorraRepository(
    private val store: EncryptedStateStore,
    private val onWipe: () -> Unit,
) {
    private val _state = MutableStateFlow<RepoState>(RepoState.Loading)
    val state: StateFlow<RepoState> = _state.asStateFlow()
    private val mutex = Mutex()

    suspend fun load() {
        mutex.withLock {
            val loaded = withContext(Dispatchers.IO) { store.load() }
            _state.value = when (loaded) {
                is LoadResult.Empty -> RepoState.Ready(AppState())
                is LoadResult.Data -> RepoState.Ready(loaded.state)
                is LoadResult.Unreadable -> RepoState.Unreadable
            }
        }
    }

    suspend fun mutate(block: (AppState) -> LedgerResult): LedgerResult = mutex.withLock {
        val current = (_state.value as? RepoState.Ready)?.data
            ?: return@withLock LedgerResult.Rejected(Reason.NOT_READY)
        val result = block(current)
        if (result is LedgerResult.Ok && result.changed) {
            try {
                withContext(Dispatchers.IO) { store.save(result.state) }
            } catch (e: Exception) {
                return@withLock LedgerResult.Rejected(Reason.STORAGE_ERROR)
            }
            _state.value = RepoState.Ready(result.state)
        }
        result
    }

    fun exportJson(): String = StorageFormat.encode((_state.value as? RepoState.Ready)?.data ?: AppState())

    /** Sustituye todos los datos por los de un archivo, solo si supera la validación completa. */
    suspend fun importJson(text: String): Boolean = mutex.withLock {
        val candidate = StorageFormat.decode(text) ?: return@withLock false
        val clean = Ledger.sanitizeImport(candidate, LocalDate.now()) ?: return@withLock false
        try {
            withContext(Dispatchers.IO) { store.save(clean) }
        } catch (e: Exception) {
            return@withLock false
        }
        _state.value = RepoState.Ready(clean)
        true
    }

    /** Borra el archivo, la clave del Keystore y los recordatorios. Es irreversible. */
    suspend fun wipe() {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                store.delete()
                onWipe()
            }
            _state.value = RepoState.Ready(AppState())
        }
    }
}
