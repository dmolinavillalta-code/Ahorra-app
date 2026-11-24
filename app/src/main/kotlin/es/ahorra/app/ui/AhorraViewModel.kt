package es.ahorra.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.ahorra.app.AhorraApp
import es.ahorra.app.data.RepoState
import es.ahorra.app.work.ReminderScheduler
import es.ahorra.core.AppState
import es.ahorra.core.Ledger
import es.ahorra.core.LedgerResult
import es.ahorra.core.Settings
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AhorraViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as AhorraApp).repository

    val repoState: StateFlow<RepoState> = repo.state

    /** Aplica una operación del núcleo de forma serializada y persistente; [onResult] recibe el resultado. */
    fun submit(block: (AppState) -> LedgerResult, onResult: (LedgerResult) -> Unit = {}) {
        viewModelScope.launch { onResult(repo.mutate(block)) }
    }

    fun applySettings(settings: Settings) {
        submit({ Ledger.updateSettings(it, settings) }) { result ->
            if (result is LedgerResult.Ok) ReminderScheduler.apply(getApplication<Application>(), result.state.settings)
        }
    }

    fun exportJson(): String = repo.exportJson()

    fun importJson(text: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repo.importJson(text)
            if (ok) {
                (repo.state.value as? RepoState.Ready)?.let {
                    ReminderScheduler.apply(getApplication<Application>(), it.data.settings)
                }
            }
            onResult(ok)
        }
    }

    fun wipe(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.wipe()
            onDone()
        }
    }
}
