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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.ahorra.app.R
import es.ahorra.app.ui.theme.LocalBrand
import es.ahorra.app.ui.theme.OnBrand
import es.ahorra.core.AppState
import es.ahorra.core.Goal
import es.ahorra.core.Ledger
import es.ahorra.core.Limits
import es.ahorra.core.Pets
import es.ahorra.core.Rarity
import es.ahorra.core.Species

/** Pestaña "Mascotas": tus compañeros y el álbum de colección (las no descubiertas aparecen en silueta). */
@Composable
fun PetsScreen(data: AppState, vm: AhorraViewModel) {
    val brand = LocalBrand.current
    val found = remember(data) { data.collection.toSet() }
    var renaming by remember { mutableStateOf<Goal?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                Text(stringResource(R.string.pets_title), color = OnBrand, style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.pets_collection_count, found.size, Pets.all.size),
                    color = OnBrand.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.titleMedium,
                )
                ProgressBar(
                    fraction = found.size.toFloat() / Pets.all.size,
                    description = stringResource(R.string.pets_collection_count, found.size, Pets.all.size),
                    trackColor = OnBrand.copy(alpha = 0.15f),
                    barBrush = Brush.horizontalGradient(listOf(OnBrand.copy(alpha = 0.6f), OnBrand)),
                )
                Text(stringResource(R.string.pets_intro), color = OnBrand.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (data.goals.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.pets_companions)) }
            data.goals.forEach { goal ->
                item(key = "c-" + goal.id) { CompanionRow(data, goal) { renaming = goal } }
            }
        }

        Rarity.entries.forEach { rarity ->
            item(key = "h-" + rarity.name) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RarityChip(rarity)
                        Text(
                            Pets.ofRarity(rarity).count { it.id in found }.toString() + "/" + Pets.ofRarity(rarity).size,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Text(
                        stringResource(rarity.howRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Pets.ofRarity(rarity).chunked(3).forEach { row ->
                item(key = "r-" + row.first().id) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { sp -> SpeciesCard(sp, sp.id in found, Modifier.weight(1f)) }
                        repeat(3 - row.size) { Column(Modifier.weight(1f)) {} }
                    }
                }
            }
        }
    }

    renaming?.let { goal ->
        var name by remember(goal.id) { mutableStateOf(goal.petName) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(R.string.pet_rename_title)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(Limits.MAX_PET_NAME) },
                    label = { Text(stringResource(R.string.field_pet_name)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.submit({ Ledger.renamePet(it, goal.id, name) })
                    renaming = null
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun CompanionRow(data: AppState, goal: Goal, onRename: () -> Unit) {
    val species = Pets.companionOf(data, goal)
    val fraction = Pets.nextRarityFraction(data, goal)
    val next = Rarity.entries.getOrNull(species.rarity.ordinal + 1)
    Card(
        onClick = onRename,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PetAvatar(species, 72.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(goal.petName.ifBlank { stringResource(species.nameRes()) }, style = MaterialTheme.typography.titleMedium)
                Text(
                    goalEmoji(goal.icon) + " " + goal.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                RarityChip(species.rarity)
                if (next != null) {
                    ProgressBar(
                        fraction = fraction,
                        description = stringResource(R.string.pets_next_form, stringResource(next.labelRes())),
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        barBrush = Brush.horizontalGradient(listOf(next.color(), next.color())),
                        thickness = 6.dp,
                    )
                    Text(
                        stringResource(R.string.pets_next_form, stringResource(next.labelRes())) + " · " + stringResource(next.howRes()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeciesCard(species: Species, discovered: Boolean, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (discovered) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PetAvatar(species, 64.dp, animate = false, discovered = discovered)
            Text(
                if (discovered) stringResource(species.nameRes()) else stringResource(R.string.pets_unknown),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
