package es.ahorra.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.ahorra.app.R
import es.ahorra.core.AppState
import es.ahorra.core.Gamification
import es.ahorra.core.Goal
import es.ahorra.core.Ledger
import es.ahorra.core.Level
import es.ahorra.core.StreakInfo
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    data: AppState,
    onOpenGoal: (String) -> Unit,
    onNewGoal: () -> Unit,
    onSettings: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val streak = remember(data) { Gamification.streak(data, today) }
    val level = remember(data) { Gamification.level(data.contributions.size) }
    val achievements = remember(data) { Gamification.achievements(data, today).toList().sorted() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = onSettings) { Text(stringResource(R.string.settings_title)) }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNewGoal) { Text(stringResource(R.string.home_new_goal)) }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { HabitRow(level, streak) }
            if (data.goals.isEmpty()) {
                item { EmptyState() }
            }
            items(data.goals, key = { it.id }) { goal ->
                GoalCard(goal, Ledger.savedMinor(data, goal.id), today) { onOpenGoal(goal.id) }
            }
            if (achievements.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.achievements_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(achievements, key = { it.name }) { achievement ->
                    Text("★ " + stringResource(achievement.labelRes()))
                }
            }
        }
    }
}

@Composable
private fun HabitRow(level: Level, streak: StreakInfo) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.weight(1f)) {
            Column(Modifier.padding(12.dp)) {
                Text(stringResource(R.string.level_label, level.number), style = MaterialTheme.typography.titleMedium)
                val next = level.contributionsToNext
                Text(
                    if (next != null) stringResource(R.string.level_next, next) else stringResource(R.string.level_max),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Card(Modifier.weight(1f)) {
            Column(Modifier.padding(12.dp)) {
                Text(stringResource(R.string.streak_label, streak.current), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.home_empty_body), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun GoalCard(goal: Goal, saved: Long, today: LocalDate, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            GoalSummary(goal, saved, today)
        }
    }
}
