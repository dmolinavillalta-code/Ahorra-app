package es.ahorra.app.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.ahorra.app.R
import es.ahorra.app.data.LockConfig
import es.ahorra.app.data.PinResult
import es.ahorra.app.ui.theme.LocalBrand
import es.ahorra.core.Lockout
import es.ahorra.core.PinPolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import es.ahorra.app.ui.theme.OnBrand

// ---- Piezas comunes -------------------------------------------------------------------------------------------

/** Seis puntos que se rellenan al teclear. Con [error] tiemblan (el temblor lo anima quien los usa). */
@Composable
private fun PinDots(filled: Int, shakeX: Float, error: Boolean) {
    Row(
        Modifier.offset { IntOffset(shakeX.roundToInt(), 0) },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(PinPolicy.PIN_LENGTH) { index ->
            val on = index < filled
            val scale by animateFloatAsState(
                targetValue = if (on) 1f else 0.72f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                label = "dot",
            )
            Box(
                Modifier
                    .size(16.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        when {
                            error -> Color(0xFFB3261E)
                            on -> OnBrand
                            else -> OnBrand.copy(alpha = 0.30f)
                        },
                    ),
            )
        }
    }
}

@Composable
private fun PinKey(label: String, description: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.38f else 0.16f))
            .clickable(enabled = enabled, role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics { if (description != null) contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = OnBrand, fontSize = 28.sp, style = MaterialTheme.typography.headlineMedium)
    }
}

/** Teclado numérico propio: sin teclado del sistema, sin sugerencias ni historial de escritura. */
@Composable
private fun PinPad(
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    leftSlot: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                row.forEach { d -> PinKey(d.toString(), enabled = enabled) { onDigit(d) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) { leftSlot() }
            PinKey("0", enabled = enabled) { onDigit('0') }
            PinKey("⌫", description = stringResource(R.string.pin_delete), enabled = enabled) { onDelete() }
        }
    }
}

private fun formatCountdown(millis: Long): String {
    val total = ((millis + 999) / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

// ---- Pantalla de bloqueo (la bienvenida) -----------------------------------------------------------------------

@Composable
fun LockScreen(vm: AhorraViewModel, displayName: String, config: LockConfig) {
    val brand = LocalBrand.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var attemptsLeft by remember { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }
    var lockedUntilElapsed by remember { mutableLongStateOf(0L) }
    var remaining by remember { mutableLongStateOf(0L) }
    var visible by remember { mutableStateOf(false) }
    var biometricTried by rememberSaveable { mutableStateOf(false) }

    val biometricReady = config.biometric && BiometricAuth.isAvailable(context)

    fun startLockout(millis: Long) {
        lockedUntilElapsed = SystemClock.elapsedRealtime() + millis
        remaining = millis
    }

    LaunchedEffect(Unit) {
        visible = true
        val initial = Lockout.remainingMillis(config.lockout, System.currentTimeMillis())
        if (initial > 0) startLockout(initial)
    }
    LaunchedEffect(lockedUntilElapsed) {
        while (true) {
            val left = lockedUntilElapsed - SystemClock.elapsedRealtime()
            remaining = left.coerceAtLeast(0L)
            if (left <= 0L) break
            delay(250)
        }
    }

    val promptTitle = stringResource(R.string.biometric_title)
    val promptSubtitle = stringResource(R.string.biometric_subtitle)
    val promptNegative = stringResource(R.string.biometric_use_pin)
    fun askBiometric() {
        val activity = context.findFragmentActivity() ?: return
        BiometricAuth.prompt(activity, promptTitle, promptSubtitle, promptNegative, onSuccess = { vm.unlockWithBiometric() }, onFallback = {})
    }
    LaunchedEffect(biometricReady) {
        if (biometricReady && !biometricTried && remaining == 0L) {
            biometricTried = true
            delay(700) // deja ver la bienvenida antes del diálogo del sistema
            askBiometric()
        }
    }

    fun submitPin(value: String) {
        vm.tryPin(value) { result ->
            when (result) {
                PinResult.Success -> Unit // la raíz cambia de pantalla sola
                is PinResult.Wrong -> {
                    attemptsLeft = result.attemptsLeft
                    error = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        repeat(3) {
                            shake.animateTo(18f, tween(45))
                            shake.animateTo(-18f, tween(45))
                        }
                        shake.animateTo(0f, tween(45))
                        delay(250)
                        pin = ""
                        error = false
                    }
                }
                is PinResult.LockedOut -> {
                    startLockout(result.remainingMillis)
                    pin = ""
                }
            }
        }
    }

    val lockedOut = remaining > 0L
    val greeting = stringResource(greetingRes())

    AnimatedBackdrop(brand.gradientStart, brand.gradientEnd, Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(12.dp))
                AnimatedVisibility(visible, enter = fadeIn(tween(600)) + slideInVertically(tween(700)) { -it / 2 }) {
                    BrandMark(84.dp, Color.White.copy(alpha = 0.60f), Color.White.copy(alpha = 0.25f))
                }
                Spacer(Modifier.height(18.dp))
                AnimatedVisibility(visible, enter = fadeIn(tween(700, delayMillis = 200))) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(greeting, color = OnBrand.copy(alpha = 0.85f), style = MaterialTheme.typography.titleLarge)
                        Text(
                            displayName.ifBlank { stringResource(R.string.lock_default_name) },
                            color = OnBrand,
                            style = MaterialTheme.typography.displaySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                AnimatedVisibility(visible, enter = fadeIn(tween(700, delayMillis = 400))) {
                    Text(
                        stringResource(R.string.lock_tagline),
                        color = OnBrand.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PinDots(filled = pin.length, shakeX = shake.value, error = error)
                Text(
                    text = when {
                        lockedOut -> stringResource(R.string.lock_locked_out, formatCountdown(remaining))
                        error -> stringResource(R.string.lock_wrong, attemptsLeft)
                        else -> stringResource(R.string.lock_enter_pin)
                    },
                    color = OnBrand.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.height(24.dp),
                )
            }

            PinPad(
                enabled = !lockedOut && !error,
                onDigit = { d ->
                    if (pin.length < PinPolicy.PIN_LENGTH) {
                        pin += d
                        if (pin.length == PinPolicy.PIN_LENGTH) submitPin(pin)
                    }
                },
                onDelete = { pin = pin.dropLast(1) },
                leftSlot = {
                    if (biometricReady) {
                        PinKey("👆", description = stringResource(R.string.biometric_use), enabled = !lockedOut) { askBiometric() }
                    }
                },
            )
        }
    }
}

// ---- Crear, cambiar o quitar el PIN ----------------------------------------------------------------------------

enum class PinMode { CREATE, CHANGE, REMOVE }

private enum class PinStep { CURRENT, NEW, CONFIRM, BIOMETRIC }

@Composable
fun SetPinScreen(vm: AhorraViewModel, mode: PinMode, onDone: () -> Unit, onCancel: () -> Unit) {
    val brand = LocalBrand.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var step by rememberSaveable { mutableStateOf(if (mode == PinMode.CREATE) PinStep.NEW else PinStep.CURRENT) }
    var pin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<Int?>(null) }
    var messageArg by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }

    fun fail(res: Int, arg: Int = 0) {
        message = res
        messageArg = arg
        error = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            repeat(3) {
                shake.animateTo(18f, tween(45))
                shake.animateTo(-18f, tween(45))
            }
            shake.animateTo(0f, tween(45))
            delay(250)
            pin = ""
            error = false
        }
    }

    fun complete(value: String) {
        when (step) {
            PinStep.CURRENT -> {
                busy = true
                vm.checkPin(value) { result ->
                    busy = false
                    when (result) {
                        PinResult.Success -> {
                            message = null
                            pin = ""
                            if (mode == PinMode.REMOVE) {
                                vm.removePin()
                                onDone()
                            } else {
                                step = PinStep.NEW
                            }
                        }
                        is PinResult.Wrong -> fail(R.string.lock_wrong, result.attemptsLeft)
                        is PinResult.LockedOut -> {
                            pin = ""
                            message = R.string.pin_locked_try_later
                        }
                    }
                }
            }
            PinStep.NEW -> {
                if (PinPolicy.isWeak(value)) {
                    fail(R.string.pin_weak)
                } else {
                    firstPin = value
                    pin = ""
                    message = null
                    step = PinStep.CONFIRM
                }
            }
            PinStep.CONFIRM -> {
                if (value != firstPin) {
                    firstPin = ""
                    fail(R.string.pin_mismatch)
                    step = PinStep.NEW
                } else {
                    busy = true
                    vm.setPin(value) { ok ->
                        busy = false
                        if (!ok) {
                            fail(R.string.pin_weak)
                            step = PinStep.NEW
                        } else if (mode == PinMode.CREATE && BiometricAuth.isAvailable(context)) {
                            step = PinStep.BIOMETRIC
                        } else {
                            onDone()
                        }
                    }
                }
            }
            PinStep.BIOMETRIC -> Unit
        }
    }

    AnimatedBackdrop(brand.gradientStart, brand.gradientEnd, Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                TextButton(onClick = onCancel, colors = ButtonDefaults.textButtonColors(contentColor = OnBrand)) {
                    Text("←  " + stringResource(R.string.action_back))
                }
            }

            if (step == PinStep.BIOMETRIC) {
                val activityTitle = stringResource(R.string.biometric_title)
                val subtitle = stringResource(R.string.biometric_subtitle)
                val negative = stringResource(R.string.action_cancel)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("👆", fontSize = 72.sp)
                    Text(
                        stringResource(R.string.biometric_offer_title),
                        color = OnBrand,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(R.string.biometric_offer_body),
                        color = OnBrand.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val activity = context.findFragmentActivity()
                            if (activity == null) {
                                onDone()
                            } else {
                                BiometricAuth.prompt(
                                    activity, activityTitle, subtitle, negative,
                                    onSuccess = { vm.setBiometric(true); onDone() },
                                    onFallback = {},
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OnBrand, contentColor = brand.gradientStart),
                    ) { Text(stringResource(R.string.biometric_enable)) }
                    TextButton(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = OnBrand),
                    ) { Text(stringResource(R.string.biometric_not_now)) }
                }
            } else {
                val title = when (step) {
                    PinStep.CURRENT -> R.string.pin_step_current
                    PinStep.NEW -> R.string.pin_step_new
                    else -> R.string.pin_step_confirm
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🔒", fontSize = 48.sp)
                    Text(
                        stringResource(title),
                        color = OnBrand,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(if (step == PinStep.NEW) R.string.pin_rules else R.string.pin_never_recoverable),
                        color = OnBrand.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    PinDots(filled = pin.length, shakeX = shake.value, error = error)
                    Text(
                        text = message?.let { stringResource(it, messageArg) } ?: "",
                        color = OnBrand.copy(alpha = 0.95f),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.height(24.dp),
                    )
                }
                PinPad(
                    enabled = !busy && !error,
                    onDigit = { d ->
                        if (pin.length < PinPolicy.PIN_LENGTH) {
                            pin += d
                            if (pin.length == PinPolicy.PIN_LENGTH) complete(pin)
                        }
                    },
                    onDelete = { pin = pin.dropLast(1) },
                    leftSlot = {},
                )
            }
        }
    }
}
