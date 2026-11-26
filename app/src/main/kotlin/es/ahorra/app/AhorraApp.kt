package es.ahorra.app

import android.app.Application
import es.ahorra.app.data.AesGcmPayloadCipher
import es.ahorra.app.data.AhorraRepository
import es.ahorra.app.data.EncryptedStateStore
import es.ahorra.app.data.KeystoreKey
import es.ahorra.app.data.LockController
import es.ahorra.app.data.LockStore
import es.ahorra.app.data.RepoState
import es.ahorra.app.work.Notifications
import es.ahorra.app.work.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class AhorraApp : Application() {

    lateinit var repository: AhorraRepository
        private set
    lateinit var lockController: LockController
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val cipher = AesGcmPayloadCipher { KeystoreKey.getOrCreate() }
        val store = EncryptedStateStore(File(filesDir, "ahorra.dat"), cipher)
        lockController = LockController(LockStore(File(filesDir, "lock.dat"), cipher))
        repository = AhorraRepository(
            store = store,
            onWipe = {
                lockController.wipe()
                KeystoreKey.delete()
                ReminderScheduler.cancel(this)
            },
        )
        Notifications.createChannel(this)
        appScope.launch { lockController.load() }
        appScope.launch {
            repository.load()
            (repository.state.value as? RepoState.Ready)?.let {
                ReminderScheduler.apply(this@AhorraApp, it.data.settings)
            }
        }
    }
}
