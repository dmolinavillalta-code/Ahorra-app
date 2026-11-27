package es.ahorra.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.ahorra.app.R
import es.ahorra.app.data.LockConfig
import es.ahorra.core.AppState
import es.ahorra.core.Gamification
import es.ahorra.core.Limits
import es.ahorra.core.PasswordCrypto
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(data: AppState, vm: AhorraViewModel, onPinMode: (PinMode) -> Unit, onWiped: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = data.settings
    val level = Gamification.level(data.contributions.size).number
    val lock by vm.lockConfig.collectAsStateWithLifecycle()
    val biometricAvailable = remember { BiometricAuth.isAvailable(context) }
    val promptTitle = stringResource(R.string.biometric_title)
    val promptSubtitle = stringResource(R.string.biometric_subtitle)
    val promptCancel = stringResource(R.string.action_cancel)

    var message by remember { mutableStateOf<Int?>(null) }
    var showWipe by remember { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf(settings.displayName) }

    // Copia de seguridad
    var showExportPassword by remember { mutableStateOf(false) }
    var showPlainWarning by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf<CharArray?>(null) }
    var pendingImport by remember { mutableStateOf<ByteArray?>(null) }
    var confirmedImport by remember { mutableStateOf<ByteArray?>(null) }
    var importNeedsPassword by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.applySettings(settings.copy(remindersEnabled = true))
        if (!granted) message = R.string.settings_notifications_denied
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val password = exportPassword
        exportPassword = null
        if (uri != null) scope.launch {
            val bytes = vm.exportBytes(password)
            password?.fill('\u0000')
            message = if (writeBytes(context, uri, bytes)) {
                if (password != null) R.string.settings_export_ok_encrypted else R.string.settings_export_ok_plain
            } else {
                R.string.settings_export_failed
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val bytes = readBytesLimited(context, uri)
            if (bytes == null) message = R.string.settings_import_failed else pendingImport = bytes
        }
    }

    fun runImport(bytes: ByteArray, password: CharArray?) {
        scope.launch {
            when (vm.importBytes(bytes, password)) {
                ImportOutcome.OK -> { confirmedImport = null; message = R.string.settings_import_ok }
                ImportOutcome.NEEDS_PASSWORD -> { confirmedImport = bytes; importNeedsPassword = true }
                ImportOutcome.WRONG_PASSWORD -> { confirmedImport = bytes; importNeedsPassword = true; message = R.string.settings_import_wrong_password }
                ImportOutcome.INVALID -> { confirmedImport = null; message = R.string.settings_import_failed }
            }
            password?.fill('\u0000')
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)

        // ---- Perfil ----
        SettingsCard(stringResource(R.string.settings_profile), "🙂") {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(Limits.MAX_DISPLAY_NAME) },
                label = { Text(stringResource(R.string.onboarding_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (name.trim() != settings.displayName) {
                TextButton(onClick = { vm.applySettings(settings.copy(displayName = name.trim())) }) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }

        // ---- Seguridad ----
        SettingsCard(stringResource(R.string.settings_security), "🔒") {
            if (!lock.pinEnabled) {
                Text(stringResource(R.string.settings_security_off), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { onPinMode(PinMode.CREATE) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_pin_create))
                }
            } else {
                Text("✅ " + stringResource(R.string.settings_security_on), style = MaterialTheme.typography.titleSmall)
                OutlinedButton(onClick = { onPinMode(PinMode.CHANGE) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_pin_change))
                }
                if (biometricAvailable) {
                    ToggleRow(stringResource(R.string.settings_biometric), lock.biometric) { on ->
                        if (!on) {
                            vm.setBiometric(false)
                        } else {
                            val activity = context.findFragmentActivity()
                            if (activity != null) {
                                BiometricAuth.prompt(
                                    activity,
                                    promptTitle,
                                    promptSubtitle,
                                    promptCancel,
                                    onSuccess = { vm.setBiometric(true) },
                                    onFallback = {},
                                )
                            }
                        }
                    }
                }
                Text(stringResource(R.string.settings_autolock), style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LockConfig.AUTO_LOCK_OPTIONS.forEach { seconds ->
                        FilterChip(
                            selected = lock.autoLockSeconds == seconds,
                            onClick = { vm.setAutoLockSeconds(seconds) },
                            label = {
                                Text(
                                    when (seconds) {
                                        0 -> stringResource(R.string.autolock_immediately)
                                        30 -> stringResource(R.string.autolock_30s)
                                        else -> stringResource(R.string.autolock_5m)
                                    },
                                )
                            },
                        )
                    }
                }
                OutlinedButton(onClick = { vm.lockNow() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_lock_now))
                }
                TextButton(onClick = { onPinMode(PinMode.REMOVE) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_pin_remove), color = MaterialTheme.colorScheme.error)
                }
            }
            ToggleRow(stringResource(R.string.settings_hide_content), lock.hideContent) { vm.setHideContent(it) }
            Text(stringResource(R.string.settings_hide_content_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.settings_security_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // ---- Recordatorios ----
        SettingsCard(stringResource(R.string.settings_reminders), "🔔") {
            ToggleRow(stringResource(R.string.settings_reminders_toggle), settings.remindersEnabled) { on ->
                if (on && Build.VERSION.SDK_INT >= 33) {
                    vm.skipNextAutoLock()
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    vm.applySettings(settings.copy(remindersEnabled = on))
                }
            }
            if (settings.remindersEnabled) {
                Text(stringResource(R.string.settings_reminder_hour, settings.reminderHour))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(8, 9, 12, 18, 20).forEach { hour ->
                        FilterChip(
                            selected = settings.reminderHour == hour,
                            onClick = { vm.applySettings(settings.copy(reminderHour = hour)) },
                            label = { Text("$hour:00") },
                        )
                    }
                }
                Text(stringResource(R.string.settings_reminder_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ---- Datos ----
        SettingsCard(stringResource(R.string.settings_data), "💾") {
            Text(stringResource(R.string.settings_data_body), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = { showExportPassword = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_export_encrypted))
            }
            TextButton(onClick = { showPlainWarning = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_export_plain))
            }
            OutlinedButton(
                onClick = {
                    vm.skipNextAutoLock()
                    importLauncher.launch(arrayOf("*/*"))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.settings_import)) }
            TextButton(onClick = { showWipe = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_wipe), color = MaterialTheme.colorScheme.error)
            }
        }

        // ---- Acerca de ----
        SettingsCard(stringResource(R.string.settings_about), "ℹ️") {
            Text(stringResource(R.string.settings_about_body), style = MaterialTheme.typography.bodyMedium)
        }
    }

    message?.let { MessageDialog(stringResource(it)) { message = null } }

    if (showExportPassword) {
        PasswordDialog(
            title = stringResource(R.string.export_password_title),
            body = stringResource(R.string.export_password_body, PasswordCrypto.MIN_PASSWORD_LENGTH),
            askConfirmation = true,
            confirmLabel = stringResource(R.string.action_continue),
            onDismiss = { showExportPassword = false },
            onSubmit = { password ->
                showExportPassword = false
                exportPassword = password
                vm.skipNextAutoLock()
                exportLauncher.launch("ahorra-copia.ahorra")
            },
        )
    }
    if (showPlainWarning) {
        ConfirmDialog(
            title = stringResource(R.string.export_plain_title),
            text = stringResource(R.string.export_plain_body),
            confirmLabel = stringResource(R.string.action_continue),
            onDismiss = { showPlainWarning = false },
            onConfirm = {
                showPlainWarning = false
                exportPassword = null
                vm.skipNextAutoLock()
                exportLauncher.launch("ahorra-copia.json")
            },
        )
    }
    pendingImport?.let { bytes ->
        ConfirmDialog(
            title = stringResource(R.string.settings_import_title),
            text = stringResource(R.string.settings_import_body),
            confirmLabel = stringResource(R.string.settings_import_confirm),
            onDismiss = { pendingImport = null },
            onConfirm = {
                pendingImport = null
                runImport(bytes, null)
            },
        )
    }
    if (importNeedsPassword) {
        val bytes = confirmedImport
        PasswordDialog(
            title = stringResource(R.string.import_password_title),
            body = stringResource(R.string.import_password_body),
            askConfirmation = false,
            confirmLabel = stringResource(R.string.settings_import_confirm),
            onDismiss = { importNeedsPassword = false; confirmedImport = null },
            onSubmit = { password ->
                importNeedsPassword = false
                if (bytes != null) runImport(bytes, password) else password.fill('\u0000')
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

@Composable
private fun SettingsCard(title: String, emoji: String, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EmojiBadge(emoji, 36)
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Pide una contraseña (y su repetición al crearla). La contraseña nunca se guarda; el [CharArray] se borra tras usarlo. */
@Composable
private fun PasswordDialog(
    title: String,
    body: String,
    askConfirmation: Boolean,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onSubmit: (CharArray) -> Unit,
) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    val longEnough = first.length >= PasswordCrypto.MIN_PASSWORD_LENGTH
    val matches = !askConfirmation || first == second
    val valid = if (askConfirmation) longEnough && matches else first.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(body, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = first,
                    onValueChange = { first = it.take(128) },
                    label = { Text(stringResource(R.string.password_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                if (askConfirmation) {
                    OutlinedTextField(
                        value = second,
                        onValueChange = { second = it.take(128) },
                        label = { Text(stringResource(R.string.password_repeat)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = second.isNotEmpty() && !matches,
                    )
                    Text(stringResource(R.string.password_no_recovery), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSubmit(first.toCharArray()) }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
