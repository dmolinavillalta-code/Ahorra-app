package es.ahorra.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.ahorra.app.R
import es.ahorra.core.Accent
import es.ahorra.core.AppState
import es.ahorra.core.Gamification
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(data: AppState, vm: AhorraViewModel, onBack: () -> Unit, onWiped: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = data.settings
    val level = Gamification.level(data.contributions.size).number

    var message by remember { mutableStateOf<Int?>(null) }
    var showWipe by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<String?>(null) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.applySettings(settings.copy(remindersEnabled = true))
        if (!granted) message = R.string.settings_notifications_denied
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            message = if (writeText(context, uri, vm.exportJson())) R.string.settings_export_ok else R.string.settings_export_failed
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = readTextLimited(context, uri)
            if (text == null) message = R.string.settings_import_failed else pendingImport = text
        }
    }

    ScreenScaffold(stringResource(R.string.settings_title), onBack) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.settings_reminders), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_reminders_toggle), Modifier.weight(1f))
                Switch(
                    checked = settings.remindersEnabled,
                    onCheckedChange = { on ->
                        if (on && Build.VERSION.SDK_INT >= 33) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            vm.applySettings(settings.copy(remindersEnabled = on))
                        }
                    },
                )
            }
            if (settings.remindersEnabled) {
                Text(stringResource(R.string.settings_reminder_hour, settings.reminderHour))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(8, 9, 12, 18, 20).forEach { hour ->
                        FilterChip(
                            selected = settings.reminderHour == hour,
                            onClick = { vm.applySettings(settings.copy(reminderHour = hour)) },
                            label = { Text("$hour:00") },
                        )
                    }
                }
                Text(stringResource(R.string.settings_reminder_note), style = MaterialTheme.typography.bodySmall)
            }

            Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Accent.entries.forEach { accent ->
                    val unlocked = accent.unlockLevel <= level
                    FilterChip(
                        selected = settings.accent == accent,
                        enabled = unlocked,
                        onClick = { vm.applySettings(settings.copy(accent = accent)) },
                        label = {
                            Text(
                                stringResource(accent.labelRes()) +
                                    if (unlocked) "" else " · " + stringResource(R.string.settings_unlock_level, accent.unlockLevel),
                            )
                        },
                    )
                }
            }

            Text(stringResource(R.string.settings_data), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.settings_data_body), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(
                onClick = { exportLauncher.launch("ahorra-copia.json") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.settings_export)) }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.settings_import)) }
            TextButton(onClick = { showWipe = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_wipe), color = MaterialTheme.colorScheme.error)
            }

            Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.settings_about_body), style = MaterialTheme.typography.bodyMedium)
        }
    }

    message?.let { MessageDialog(stringResource(it)) { message = null } }
    pendingImport?.let { text ->
        ConfirmDialog(
            title = stringResource(R.string.settings_import_title),
            text = stringResource(R.string.settings_import_body),
            confirmLabel = stringResource(R.string.settings_import_confirm),
            onDismiss = { pendingImport = null },
            onConfirm = {
                pendingImport = null
                vm.importJson(text) { ok ->
                    message = if (ok) R.string.settings_import_ok else R.string.settings_import_failed
                }
            },
        )
    }
    if (showWipe) {
        ConfirmDialog(
            title = stringResource(R.string.settings_wipe_title),
            text = stringResource(R.string.settings_wipe_body),
            confirmLabel = stringResource(R.string.settings_wipe_confirm),
            onDismiss = { showWipe = false },
            onConfirm = {
                showWipe = false
                vm.wipe(onWiped)
            },
        )
    }
}
