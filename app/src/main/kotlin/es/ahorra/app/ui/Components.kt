package es.ahorra.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ahorra.app.R
import es.ahorra.core.Goal
import es.ahorra.core.GoalStatus
import es.ahorra.core.PlanCalculator
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(title: String, onBack: (() -> Unit)?, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
                    }
                },
            )
        },
        content = content,
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun MessageDialog(text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } },
    )
}

/** Resumen de un objetivo: ahorrado, progreso animado, próxima aportación y fecha estimada de fin. */
@Composable
fun GoalSummary(goal: Goal, saved: Long, today: LocalDate) {
    val basisPoints = PlanCalculator.progressBasisPoints(saved, goal.targetMinor)
    val progress by animateFloatAsState(
        targetValue = basisPoints / 10_000f,
        animationSpec = tween(durationMillis = 700),
        label = "progress",
    )
    val percentText = stringResource(R.string.home_progress, basisPoints / 100)
    val next = remember(goal, saved, today) { PlanCalculator.nextPayment(goal, saved, today) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(goal.name, style = MaterialTheme.typography.titleMedium)
        Text(money(saved, goal.currencyCode), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.home_of_target, money(goal.targetMinor, goal.currencyCode)),
            style = MaterialTheme.typography.bodyMedium,
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .semantics { contentDescription = percentText },
        )
        Text(percentText, style = MaterialTheme.typography.labelLarge)
        when (goal.status) {
            GoalStatus.COMPLETED -> Text(
                stringResource(R.string.home_status_completed),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
            )
            GoalStatus.PAUSED -> Text(
                stringResource(R.string.home_status_paused),
                style = MaterialTheme.typography.titleSmall,
            )
            GoalStatus.ACTIVE -> {
                if (next != null) {
                    val amount = money(next.outstandingMinor, goal.currencyCode)
                    val date = next.installment.dueDate.pretty()
                    Text(
                        if (next.overdue) {
                            stringResource(R.string.home_overdue, amount, date)
                        } else {
                            stringResource(R.string.home_next_payment, amount, date)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                PlanCalculator.estimatedCompletion(goal)?.let { end ->
                    Text(
                        stringResource(R.string.home_estimated_end, end.pretty()),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
