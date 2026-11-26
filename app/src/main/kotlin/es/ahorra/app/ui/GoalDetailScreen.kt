package es.ahorra.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.ahorra.app.R
import es.ahorra.app.ui.theme.LocalBrand
import es.ahorra.core.Achievement
import es.ahorra.core.AppState
import es.ahorra.core.Contribution
import es.ahorra.core.ContributionKind
import es.ahorra.core.Gamification
import es.ahorra.core.Goal
import es.ahorra.core.GoalStatus
import es.ahorra.core.Ledger
import es.ahorra.core.LedgerResult
import es.ahorra.core.Limits
import es.ahorra.core.MoneyMath
import es.ahorra.core.PlanCalculator
import es.ahorra.core.ProgressSeries
import es.ahorra.core.Reason
import java.time.LocalDate
import java.util.UUID

private data class PendingContribution(val amountMinor: Long, val note: String, val extra: Boolean)

@Composable
fun GoalDetailScreen(data: AppState, vm: AhorraViewModel, goalId: String, onBack: () -> Unit) {
    val goal = data.goals.firstOrNull { it.id == goalId }
    if (goal == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val today = remember { LocalDate.now() }
    val haptic = LocalHapticFeedback.current
    val brand = LocalBrand.current
    val saved = Ledger.savedMinor(data, goal.id)
    val contributions = remember(data, goal.id) {
        data.contributions.filter { it.goalId == goal.id }.sortedByDescending { it.date }
    }
    val schedule = remember(goal) { PlanCalculator.schedule(goal) }
    val series = remember(data, goal) { ProgressSeries.build(goal, data.contributions, today) }
    val next = PlanCalculator.nextPayment(goal, saved, today)
    val basisPoints = PlanCalculator.progressBasisPoints(saved, goal.targetMinor)

    var showForm by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<PendingContribution?>(null) }
    var showEdit by remember { mutableStateOf(false) }
    var showDeleteGoal by remember { mutableStateOf(false) }
    var contributionToDelete by remember { mutableStateOf<Contribution?>(null) }
    // Identificador de la aportación en curso: un reintento o doble toque nunca la registra dos veces.
    var contributionId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var celebrated by rememberSaveable(goal.id) { mutableStateOf(goal.status == GoalStatus.COMPLETED) }
    var showCelebration by remember { mutableStateOf(false) }
    var errorReason by remember { mutableStateOf<Reason?>(null) }
    var confetti by remember { mutableIntStateOf(0) }
    var newAchievements by remember { mutableStateOf<List<Achievement>>(emptyList()) }

    LaunchedEffect(goal.status) {
        if (goal.status == GoalStatus.COMPLETED && !celebrated) {
            celebrated = true
            showCelebration = true
            confetti++
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        if (goal.status != GoalStatus.COMPLETED) celebrated = false
    }

    Box(Modifier.fillMaxSize()) {
        ScreenScaffold(goal.name, onBack) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(30.dp))
                            .background(brand.gradient)
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ProgressRing(fraction = basisPoints / 10_000f, diameter = 190.dp, strokeWidth = 16.dp) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(goalEmoji(goal.icon), fontSize = 40.sp)
                                Text(
                                    stringResource(R.string.home_progress, basisPoints / 100),
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                        AnimatedMoney(saved, goal.currencyCode, MaterialTheme.typography.displaySmall, color = Color.White)
                        Text(
                            stringResource(R.string.home_of_target, money(goal.targetMinor, goal.currencyCode)),
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        when (goal.status) {
                            GoalStatus.COMPLETED -> GlassPill("🏆 " + stringResource(R.string.home_status_completed))
                            GoalStatus.PAUSED -> GlassPill("⏸ " + stringResource(R.string.home_status_paused))
                            GoalStatus.ACTIVE -> {
                                GlassPill(stringResource(R.string.detail_remaining, money(goal.targetMinor - saved, goal.currencyCode)))
                                if (next != null) {
                                    val amount = money(next.outstandingMinor, goal.currencyCode)
                                    val date = next.installment.dueDate.pretty()
                                    Text(
                                        if (next.overdue) stringResource(R.string.home_overdue, amount, date)
                                        else stringResource(R.string.home_next_payment, amount, date),
                                        color = Color.White.copy(alpha = 0.9f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                PlanCalculator.estimatedCompletion(goal)?.let { end ->
                                    Text(
                                        stringResource(R.string.home_estimated_end, end.pretty()),
                                        color = Color.White.copy(alpha = 0.75f),
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (goal.status != GoalStatus.COMPLETED) {
                            Button(
                                onClick = { showForm = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                            ) { Text("＋  " + stringResource(R.string.action_add_contribution), style = MaterialTheme.typography.titleMedium) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { showEdit = true }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.action_edit_plan_short))
                            }
                            when (goal.status) {
                                GoalStatus.ACTIVE -> FilledTonalButton(
                                    onClick = { vm.submit({ Ledger.pauseGoal(it, goal.id, LocalDate.now()) }) },
                                    modifier = Modifier.weight(1f),
                                ) { Text(stringResource(R.string.action_pause)) }
                                GoalStatus.PAUSED -> FilledTonalButton(
                                    onClick = { vm.submit({ Ledger.resumeGoal(it, goal.id, LocalDate.now()) }) },
                                    modifier = Modifier.weight(1f),
                                ) { Text(stringResource(R.string.action_resume)) }
                                GoalStatus.COMPLETED -> Unit
                            }
                        }
                    }
                }

                if (series.actual.size >= 2) {
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        ) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(stringResource(R.string.chart_title), style = MaterialTheme.typography.titleMedium)
                                EvolutionChart(
                                    series = series,
                                    targetMinor = goal.targetMinor,
                                    description = stringResource(R.string.chart_description, money(saved, goal.currencyCode), money(goal.targetMinor, goal.currencyCode)),
                                    lineColor = MaterialTheme.colorScheme.primary,
                                    planColor = MaterialTheme.colorScheme.outline,
                                    gridColor = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("━ " + stringResource(R.string.chart_actual), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                    Text("┅ " + stringResource(R.string.chart_plan), color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                if (schedule.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.detail_schedule_title)) }
                    items(schedule, key = { "i${it.index}" }) { installment ->
                        val paid = PlanCalculator.isPaid(installment, goal, saved)
                        val line = stringResource(
                            R.string.detail_installment,
                            installment.dueDate.pretty(),
                            money(installment.amountMinor, goal.currencyCode),
                        )
                        Text(
                            if (paid) "✅ $line · " + stringResource(R.string.detail_paid) else "⚪ $line",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                item { SectionTitle(stringResource(R.string.detail_history_title)) }
                if (contributions.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.detail_history_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(contributions, key = { it.id }) { c ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            val extra = if (c.kind == ContributionKind.EXTRA) " · ⚡ " + stringResource(R.string.contribution_kind_extra) else ""
                            Text(money(c.amountMinor, goal.currencyCode) + extra, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                c.date.pretty() + if (c.note.isNotBlank()) " · ${c.note}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { contributionToDelete = c }) { Text(stringResource(R.string.action_delete)) }
                    }
                }

                item {
                    TextButton(onClick = { showDeleteGoal = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_delete_goal), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        ConfettiBurst(trigger = confetti, modifier = Modifier.fillMaxSize())
    }

    if (showForm) {
        ContributionFormDialog(
            goal = goal,
            suggestedMinor = next?.outstandingMinor,
            onDismiss = { showForm = false },
            onContinue = { pending = it },
        )
    }
    pending?.let { p ->
        ConfirmDialog(
            title = stringResource(R.string.contribution_confirm_title),
            text = stringResource(R.string.contribution_confirm_body, money(p.amountMinor, goal.currencyCode), goal.name),
            confirmLabel = stringResource(R.string.action_confirm),
            onDismiss = { pending = null },
            onConfirm = {
                pending = null
                showForm = false
                val now = LocalDate.now()
                val before = Gamification.achievements(data, now)
                val contribution = Contribution(
                    id = contributionId,
                    goalId = goal.id,
                    amountMinor = p.amountMinor,
                    date = now,
                    kind = if (p.extra) ContributionKind.EXTRA else ContributionKind.REGULAR,
                    note = p.note,
                )
                vm.submit({ Ledger.addContribution(it, contribution, now) }) { result ->
                    when (result) {
                        is LedgerResult.Ok -> {
                            contributionId = UUID.randomUUID().toString()
                            if (result.changed) {
                                val after = Gamification.achievements(result.state, now)
                                newAchievements = (after - before).toList().sorted()
                                confetti++
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                        is LedgerResult.Rejected -> errorReason = result.reason
                    }
                }
            },
        )
    }
    if (showEdit) {
        EditPlanDialog(
            goal = goal,
            onDismiss = { showEdit = false },
            onSave = { name, target, months, day, icon ->
                showEdit = false
                vm.submit({ Ledger.updatePlan(it, goal.id, name, target, months, day, LocalDate.now(), icon) }) { result ->
                    if (result is LedgerResult.Rejected) errorReason = result.reason
                }
            },
        )
    }
    if (showDeleteGoal) {
        ConfirmDialog(
            title = stringResource(R.string.delete_goal_title),
            text = stringResource(R.string.delete_goal_body),
            confirmLabel = stringResource(R.string.action_delete),
            onDismiss = { showDeleteGoal = false },
            onConfirm = {
                showDeleteGoal = false
                vm.submit({ Ledger.deleteGoal(it, goal.id) })
            },
        )
    }
    contributionToDelete?.let { c ->
        ConfirmDialog(
            title = stringResource(R.string.delete_contribution_title),
            text = stringResource(R.string.delete_contribution_body, money(c.amountMinor, goal.currencyCode)),
            confirmLabel = stringResource(R.string.action_delete),
            onDismiss = { contributionToDelete = null },
            onConfirm = {
                contributionToDelete = null
                vm.submit({ Ledger.removeContribution(it, c.id) })
            },
        )
    }
    if (showCelebration) {
        CelebrationDialog(goal.name) { showCelebration = false }
    } else if (newAchievements.isNotEmpty()) {
        AchievementDialog(newAchievements) { newAchievements = emptyList() }
    }
    errorReason?.let { reason ->
        MessageDialog(stringResource(reason.messageRes())) { errorReason = null }
    }
}

@Composable
private fun ContributionFormDialog(
    goal: Goal,
    suggestedMinor: Long?,
    onDismiss: () -> Unit,
    onContinue: (PendingContribution) -> Unit,
) {
    var amountText by rememberSaveable {
        mutableStateOf(suggestedMinor?.let { MoneyMath.toPlain(it, goal.currencyCode) } ?: "")
    }
    var note by rememberSaveable { mutableStateOf("") }
    var extra by rememberSaveable { mutableStateOf(false) }
    val amount = MoneyMath.parseToMinor(amountText, goal.currencyCode)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.contribution_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { text -> amountText = text.filter { it.isDigit() || it == ',' || it == '.' }.take(16) },
                    label = { Text(stringResource(R.string.contribution_amount, goal.currencyCode)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(Limits.MAX_NOTE) },
                    label = { Text(stringResource(R.string.contribution_note)) },
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = extra, onCheckedChange = { extra = it })
                    Text(stringResource(R.string.contribution_extra))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = amount != null && amount > 0,
                onClick = { onContinue(PendingContribution(amount!!, note, extra)) },
            ) { Text(stringResource(R.string.action_continue)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun EditPlanDialog(
    goal: Goal,
    onDismiss: () -> Unit,
    onSave: (name: String, targetMinor: Long, months: Int, day: Int, icon: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(goal.name) }
    var icon by rememberSaveable { mutableStateOf(goal.icon) }
    var targetText by rememberSaveable { mutableStateOf(MoneyMath.toPlain(goal.targetMinor, goal.currencyCode)) }
    var monthsText by rememberSaveable { mutableStateOf(goal.months.toString()) }
    var dayText by rememberSaveable { mutableStateOf(goal.paymentDay.toString()) }
    val target = MoneyMath.parseToMinor(targetText, goal.currencyCode)
    val months = monthsText.toIntOrNull()
    val day = dayText.toIntOrNull()
    val valid = name.isNotBlank() && target != null && target > 0 &&
        months != null && months in 1..Limits.MAX_MONTHS && day != null && day in 1..31

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_edit_plan)) },
        text = {
            run {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(Limits.MAX_NAME) },
                        label = { Text(stringResource(R.string.field_purpose)) },
                        singleLine = true,
                    )
                    IconPicker(selected = icon, onSelect = { icon = it })
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { text -> targetText = text.filter { it.isDigit() || it == ',' || it == '.' }.take(16) },
                        label = { Text(stringResource(R.string.field_target, goal.currencyCode)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    OutlinedTextField(
                        value = monthsText,
                        onValueChange = { monthsText = it.filter(Char::isDigit).take(3) },
                        label = { Text(stringResource(R.string.field_months)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = dayText,
                        onValueChange = { dayText = it.filter(Char::isDigit).take(2) },
                        label = { Text(stringResource(R.string.field_day)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Text(stringResource(R.string.edit_plan_hint), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSave(name, target!!, months!!, day!!, icon) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Celebración sin presión: una animación breve y un mensaje de logro. */
@Composable
private fun CelebrationDialog(goalName: String, onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "celebration",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.celebration_title)) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎉", fontSize = 56.sp, modifier = Modifier.scale(scale))
                Text(stringResource(R.string.celebration_body, goalName), textAlign = TextAlign.Center)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } },
    )
}

/** Logros recién conseguidos con esta aportación. */
@Composable
private fun AchievementDialog(achievements: List<Achievement>, onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "achievement",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.achievement_new_title)) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                achievements.forEach { a ->
                    Text(a.emoji(), fontSize = 48.sp, modifier = Modifier.scale(scale))
                    Text(stringResource(a.labelRes()), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } },
    )
}
