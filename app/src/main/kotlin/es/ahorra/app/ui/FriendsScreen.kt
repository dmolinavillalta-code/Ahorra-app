package es.ahorra.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.ahorra.app.R
import es.ahorra.app.data.FriendCodec
import es.ahorra.app.ui.theme.LocalBrand
import es.ahorra.app.ui.theme.OnBrand
import es.ahorra.core.AppState
import es.ahorra.core.FriendCard
import es.ahorra.core.Friends
import es.ahorra.core.Ledger
import es.ahorra.core.LedgerResult
import es.ahorra.core.Pets
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/**
 * Pestaña "Amigos". Sin servidor ni cuentas: cada persona comparte su tarjeta (solo datos de juego: nivel, racha,
 * mascotas; nunca importes ni nombres de objetivos) y el amigo la pega aquí. Se puede visitar a cada amigo deslizando.
 */
@Composable
fun FriendsScreen(data: AppState, vm: AhorraViewModel) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }
    var toRemove by remember { mutableStateOf<FriendCard?>(null) }
    val settings = data.settings
    val canShare = settings.displayName.isNotBlank()
    val shareIntro = stringResource(R.string.friends_share_text, settings.displayName)
    val shareTitle = stringResource(R.string.friends_share)

    // Identificador aleatorio local: permite que un amigo actualice tu tarjeta sin duplicarla. No es una cuenta.
    LaunchedEffect(settings.profileId) {
        if (settings.profileId.isBlank()) vm.applySettings(settings.copy(profileId = UUID.randomUUID().toString()))
    }

    val pages = remember(data) { data.friends.sortedBy { it.name.lowercase() } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            val brand = LocalBrand.current
            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .clip(RoundedCornerShape(30.dp))
                    .background(brand.gradient)
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(R.string.friends_title), color = OnBrand, style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.friends_intro), color = OnBrand.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = canShare && settings.profileId.isNotBlank(),
                        onClick = {
                            val card = Friends.myCard(data, today)
                            val text = shareIntro + "\n\n" + FriendCodec.encode(card)
                            vm.skipNextAutoLock()
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(send, shareTitle))
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OnBrand, contentColor = brand.gradientStart),
                    ) { Text(stringResource(R.string.friends_share)) }
                    OutlinedButton(
                        onClick = { showAdd = true },
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = OnBrand),
                    ) { Text(stringResource(R.string.friends_add)) }
                }
                if (!canShare) {
                    Text(stringResource(R.string.friends_need_name), color = OnBrand, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (pages.isEmpty()) {
            item {
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("🤝", style = MaterialTheme.typography.displaySmall)
                        Text(stringResource(R.string.friends_empty_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                        Text(
                            stringResource(R.string.friends_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            item { SectionTitle(stringResource(R.string.friends_visit)) }
            item {
                val pager = rememberPagerState(pageCount = { pages.size })
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalPager(state = pager, pageSpacing = 12.dp) { index ->
                        FriendProfile(pages[index]) { toRemove = pages[index] }
                    }
                    Text(
                        stringResource(R.string.friends_swipe, pager.currentPage + 1, pages.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        item {
            Text(
                stringResource(R.string.friends_privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showAdd) {
        AddFriendDialog(
            onDismiss = { showAdd = false },
            onPaste = { text ->
                val card = FriendCodec.decode(text, LocalDate.now())
                if (card == null) {
                    message = R.string.friends_invalid
                } else {
                    vm.submit({ Ledger.addFriend(it, card, LocalDate.now()) }) { result ->
                        message = when (result) {
                            is LedgerResult.Ok -> R.string.friends_added
                            is LedgerResult.Rejected -> R.string.friends_invalid
                        }
                    }
                    showAdd = false
                }
            },
            readClipboard = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val item: ClipData.Item? = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)
                item?.coerceToText(context)?.toString().orEmpty()
            },
        )
    }
    toRemove?.let { friend ->
        ConfirmDialog(
            title = stringResource(R.string.friends_remove_title, friend.name),
            text = stringResource(R.string.friends_remove_body),
            confirmLabel = stringResource(R.string.action_delete),
            onDismiss = { toRemove = null },
            onConfirm = {
                toRemove = null
                vm.submit({ Ledger.removeFriend(it, friend.profileId) })
            },
        )
    }
    message?.let { MessageDialog(stringResource(it)) { message = null } }
}

@Composable
private fun FriendProfile(card: FriendCard, onRemove: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EmojiBadge("🙂", 52)
                Column(Modifier.weight(1f)) {
                    Text(card.name, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                    Text(
                        stringResource(R.string.level_label, card.level) + " · " + stringResource(levelNameRes(card.level)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("🔥 " + stringResource(R.string.streak_best, card.bestStreak))
                StatPill("🏅 " + stringResource(R.string.friends_achievements, card.achievements))
            }
            Text(
                stringResource(R.string.friends_companions),
                style = MaterialTheme.typography.labelLarge,
            )
            if (card.companions.isEmpty()) {
                Text(stringResource(R.string.friends_no_companions), style = MaterialTheme.typography.bodySmall)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(card.companions) { companion ->
                        val species = Pets.species(companion.speciesId) ?: return@items
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                            PetAvatar(species, 84.dp)
                            Text(
                                companion.petName.ifBlank { stringResource(species.nameRes()) },
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                            RarityChip(species.rarity)
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.pets_collection_count, card.collection.size, Pets.all.size),
                style = MaterialTheme.typography.labelLarge,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(Pets.all) { sp ->
                    PetAvatar(sp, 40.dp, animate = false, discovered = sp.id in card.collection)
                }
            }
            TextButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.friends_remove), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun StatPill(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun AddFriendDialog(onDismiss: () -> Unit, onPaste: (String) -> Unit, readClipboard: () -> String) {
    var text by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.friends_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.friends_add_body), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(16_000) },
                    label = { Text(stringResource(R.string.friends_code_label)) },
                    maxLines = 4,
                )
                TextButton(onClick = { text = readClipboard() }) { Text(stringResource(R.string.friends_paste)) }
                Spacer(Modifier.height(2.dp))
            }
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onPaste(text) }) { Text(stringResource(R.string.friends_add_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
