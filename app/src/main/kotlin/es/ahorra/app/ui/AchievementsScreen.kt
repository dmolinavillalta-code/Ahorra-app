package es.ahorra.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.ahorra.app.R
import es.ahorra.app.ui.theme.LocalBrand
import es.ahorra.core.Achievement
import es.ahorra.core.AppState
import es.ahorra.core.ChallengeProgress
import es.ahorra.core.ChallengeType
import es.ahorra.core.Challenges
import es.ahorra.core.Gamification
import es.ahorra.core.Ledger
import java.time.LocalDate
import es.ahorra.app.ui.theme.OnBrand

/** Pestaña "Logros": nivel, racha, retos voluntarios del mes y galería de insignias. */
@Composable
fun AchievementsScreen(data: AppState, vm: AhorraViewModel) {
    val today = remember { LocalDate.now() }
    val count = data.contributions.size
    val level = remember(data) { Gamification.level(count) }
    val fraction = remember(data) { Gamification.levelFraction(count) }
    val streak = remember(data) { Gamification.streak(data, today) }
    val unlocked = remember(data) { Gamification.achievements(data, today) }
    val active = remember(data) { Challenges.active(data, today) }
    val available = remember(data) { Challenges.available(data, today) }
    val brand = LocalBrand.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .clip(RoundedCornerShape(30.dp))
                    .background(brand.gradient)
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.level_label, level.number),
                    color = OnBrand.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(levelNameRes(level.number)),
                    color = OnBrand,
                    style = MaterialTheme.typography.displaySmall,
                )
                ProgressBar(
                    fraction = fraction,
                    description = stringResource(R.string.level_label, level.number),
                    trackColor = OnBrand.copy(alpha = 0.22f),
                    barBrush = Brush.horizontalGradient(listOf(OnBrand.copy(alpha = 0.8f), OnBrand)),
                    thickness = 10.dp,
                )
                val next = level.contributionsToNext
                Text(
                    if (next != null) stringResource(R.string.level_next, next) else stringResource(R.string.level_max),
                    color = OnBrand.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    GlassPill("🔥 " + stringResource(R.string.streak_label, streak.current))
                    GlassPill("🏅 " + stringResource(R.string.streak_best, streak.best))
                }
            }
        }

        item { SectionTitle(stringResource(R.string.challenges_title)) }
        item {
            Text(
                stringResource(R.string.challenges_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        active.forEach { progress ->
            item(key = "a-" + progress.type.id) {
                ActiveChallengeCard(progress) {
                    vm.submit({ Ledger.dismissChallenge(it, progress.type.id, progress.month.toString()) })
                }
            }
        }
        available.forEach { type ->
            item(key = "v-" + type.id) {
                AvailableChallengeCard(type) { vm.submit({ Ledger.acceptChallenge(it, type.id, LocalDate.now()) }) }
            }
        }

        item { SectionTitle(stringResource(R.string.badges_title, unlocked.size, Achievement.entries.size)) }
        Achievement.entries.chunked(2).forEach { pair ->
            item(key = "b-" + pair.first().name) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { achievement ->
                        Badge(achievement, achievement in unlocked, Modifier.weight(1f))
                    }
                    if (pair.size == 1) Column(Modifier.weight(1f)) {}
                }
            }
        }
    }
}

@Composable
private fun Badge(achievement: Achievement, unlocked: Boolean, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                if (unlocked) achievement.emoji() else "🔒",
                fontSize = 34.sp,
                modifier = Modifier.alpha(if (unlocked) 1f else 0.55f),
            )
            Text(
                stringResource(achievement.labelRes()),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(if (unlocked) 1f else 0.6f),
            )
        }
    }
}

@Composable
private fun ActiveChallengeCard(progress: ChallengeProgress, onDismiss: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EmojiBadge(if (progress.completed) "✅" else progress.type.emoji(), 44)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(progress.type.titleRes()), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(progress.type.descRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            ProgressBar(
                fraction = progress.fraction,
                description = stringResource(R.string.challenge_progress, progress.current.coerceAtMost(progress.target), progress.target),
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                barBrush = Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), MaterialTheme.colorScheme.primary)),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (progress.completed) stringResource(R.string.challenge_done)
                    else stringResource(R.string.challenge_progress, progress.current, progress.target),
                    style = MaterialTheme.typography.labelLarge,
                    color = LocalBrand.current.accentText,
                    modifier = Modifier.weight(1f),
                )
                if (!progress.completed) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.challenge_leave)) }
                }
            }
        }
    }
}

@Composable
private fun AvailableChallengeCard(type: ChallengeType, onAccept: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EmojiBadge(type.emoji(), 44, MaterialTheme.colorScheme.surfaceContainerHighest)
            Column(Modifier.weight(1f)) {
                Text(stringResource(type.titleRes()), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(type.descRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onAccept) { Text(stringResource(R.string.challenge_accept)) }
        }
    }
}
