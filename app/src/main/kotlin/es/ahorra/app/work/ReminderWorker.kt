package es.ahorra.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import es.ahorra.app.AhorraApp
import es.ahorra.app.data.RepoState
import es.ahorra.core.GoalStatus
import es.ahorra.core.Ledger
import es.ahorra.core.PlanCalculator
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Avisa solo si hay una aportación que toca hoy o está atrasada. No toca la red ni modifica datos. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as AhorraApp
        val state = app.repository.state.first { it !is RepoState.Loading }
        val data = (state as? RepoState.Ready)?.data ?: return Result.success()
        if (!data.settings.remindersEnabled) return Result.success()

        val today = LocalDate.now()
        val due = data.goals
            .filter { it.status == GoalStatus.ACTIVE }
            .mapNotNull { goal ->
                PlanCalculator.nextPayment(goal, Ledger.savedMinor(data, goal.id), today)
                    ?.takeIf { !it.installment.dueDate.isAfter(today) }
                    ?.let { goal to it }
            }
        Notifications.postReminder(applicationContext, due)
        return Result.success()
    }
}
