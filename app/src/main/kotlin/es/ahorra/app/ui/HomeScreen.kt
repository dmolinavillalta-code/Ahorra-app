package es.ahorra.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.ahorra.app.R
import es.ahorra.app.ui.theme.LocalBrand
import es.ahorra.core.AppState
import es.ahorra.core.CurrencySummary
import es.ahorra.core.Gamification
import es.ahorra.core.Goal
import es.ahorra.core.GoalStatus
import es.ahorra.core.Ledger
import es.ahorra.core.PlanCalculator
import es.ahorra.core.Summary
import java.time.LocalDate

/** Pestaña "Hoy": resumen con degradado, nivel y racha, y los objetivos como tarjetas. */
@Composable
fun HomeScreen(
    data: AppState,
    onOpenGoal: (String) -> Unit,
    onNewGoal: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val streak = remember(data) { Gamification.streak(data, today) }
    val level = remember(data) { Gamification.level(data.contributions.size) }
    val summaries = remember(data) { Summary.byCurrency(data) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewGoal,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Text("＋  " + stringResource(R.string.home_new_goal), style = MaterialTheme.typography.labelLarge) }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                HeroCard(
                    name = data.settings.displayName,
                    summaries = summaries,
                    levelNumber = level.number,
                    streakMonths = streak.current,
                )
            }
            if (data.goals.isEmpty()) {
                item { EmptyState() }
            }
            items(data.goals, key = { it.id }) { goal ->
                GoalCard(goal, Ledger.savedMinor(data, goal.id), today) { onOpenGoal(goal.id) }
            }
        }
    }
}

@Composable
private fun HeroCard(name: String, summaries: List<CurrencySummary>, levelNumber: Int, streakMonths: Int) {
    val brand = LocalBrand.current
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .clip(RoundedCornerShape(30.dp))
            .background(brand.gradient)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            if (name.isBlank()) stringResource(greetingRes()) else stringResource(greetingRes()) + ", " + name,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.titleMedium,
        )
        if (summaries.isEmpty()) {
            Text(stringResource(R.string.hero_empty), color = Color.White, style = MaterialTheme.typography.headlineSmall)
        } else {
            Text(stringResource(R.string.hero_saved_label), color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelLarge)
            summaries.forEachIndexed { index, summary ->
                AnimatedMoney(
                    minor = summary.savedMinor,
                    currencyCode = summary.currencyCode,
                    style = if (index == 0) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Text(
                    stringResource(R.string.home_of_target, money(summary.targetMinor, summary.currencyCode)),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                val fraction = if (summary.targetMinor > 0) summary.savedMinor.toFloat() / summary.targetMinor else 0f
                ProgressBar(
                    fraction = fraction,
                    description = stringResource(R.string.home_progress, (fraction * 100).toInt().coerceIn(0, 100)),
                    trackColor = Color.White.copy(alpha = 0.22f),
                    barBrush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.8f), Color.White)),
                    thickness = 8.dp,
                )
                if (summary.monthlyMinor > 0) {
                    Text(
                        stringResource(R.string.hero_monthly, money(summary.monthlyMinor, summary.currencyCode)),
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        Row(
            Modifier
                .padding(top = 10.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassPill("⭐ " + stringResource(R.string.level_label, levelNumber))
            GlassPill("🔥 " + stringResource(R.string.streak_label, streakMonths))
        }
    }
}

@Composable
private fun EmptyState() {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("🌱", fontSize = 56.sp)
            Text(
                stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.home_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Tarjeta de un objetivo: icono, nombre, anillo de progreso y estado del plan. */
@Composable
fun GoalCard(goal: Goal, saved: Long, today: LocalDate, onClick: () -> Unit) {
    val basisPoints = PlanCalculator.progressBasisPoints(saved, goal.targetMinor)
    val percentText = stringResource(R.string.home_progress, basisPoints / 100)
    val next = remember(goal, saved, today) { PlanCalculator.nextPayment(goal, saved, today) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ProgressRing(
                fraction = basisPoints / 10_000f,
                diameter = 72.dp,
                strokeWidth = 7.dp,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                startColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                endColor = MaterialTheme.colorScheme.primary,
            ) { Text(goalEmoji(goal.icon), fontSize = 28.sp) }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(goal.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    money(saved, goal.currencyCode) + " " + stringResource(R.string.home_of_target, money(goal.targetMinor, goal.currencyCode)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(percentText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                when (goal.status) {
                    GoalStatus.COMPLETED -> Text(
                        "🏆 " + stringResource(R.string.home_status_completed),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    GoalStatus.PAUSED -> Text(
                        "⏸ " + stringResource(R.string.home_status_paused),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    GoalStatus.ACTIVE -> if (next != null) {
                        val amount = money(next.outstandingMinor, goal.currencyCode)
                        val date = next.installment.dueDate.pretty()
                        Text(
                            if (next.overdue) stringResource(R.string.home_overdue, amount, date)
                            else stringResource(R.string.home_next_payment, amount, date),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (next.overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
