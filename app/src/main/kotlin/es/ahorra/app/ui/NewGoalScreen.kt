package es.ahorra.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import es.ahorra.app.ui.theme.LocalBrand
import es.ahorra.app.ui.theme.OnBrand
import es.ahorra.core.Pets
import es.ahorra.core.Rarity
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.ahorra.app.R
import es.ahorra.core.Goal
import es.ahorra.core.GoalIcons
import es.ahorra.core.Ledger
import es.ahorra.core.LedgerResult
import es.ahorra.core.Limits
import es.ahorra.core.MoneyMath
import es.ahorra.core.PlanCalculator
import es.ahorra.core.Reason
import java.time.LocalDate
import java.util.UUID

@Composable
fun NewGoalScreen(vm: AhorraViewModel, onBack: () -> Unit) {
    val today = remember { LocalDate.now() }
    // El identificador se fija al abrir la pantalla: si se pulsa "Crear" dos veces, solo se crea un objetivo.
    val goalId = rememberSaveable { UUID.randomUUID().toString() }
    var purpose by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf(GoalIcons.DEFAULT) }
    var currency by rememberSaveable { mutableStateOf(defaultCurrency()) }
    var targetText by rememberSaveable { mutableStateOf("") }
    var monthsText by rememberSaveable { mutableStateOf("12") }
    var dayText by rememberSaveable { mutableStateOf(minOf(today.dayOfMonth, 28).toString()) }
    var error by remember { mutableStateOf<Reason?>(null) }
    var petName by rememberSaveable { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var stage by rememberSaveable { mutableStateOf(0) } // 0 = formulario, 1 = nace, 2 = nacido
    val hold = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val species = remember(goalId) { Pets.speciesFor(goalId, Rarity.COMMON) }

    val target = MoneyMath.parseToMinor(targetText, currency)
    val months = monthsText.toIntOrNull()
    val day = dayText.toIntOrNull()
    val valid = purpose.isNotBlank() &&
        target != null && target > 0 &&
        months != null && months in 1..Limits.MAX_MONTHS &&
        day != null && day in 1..31
    val preview = if (valid) PlanCalculator.buildSchedule(target!!, today, months!!, day!!) else emptyList()

    fun hatch() {
        if (!valid || submitting || stage != 0) return
        submitting = true
        val goal = Goal(
            id = goalId, name = purpose, targetMinor = target!!, currencyCode = currency,
            startDate = today, months = months!!, paymentDay = day!!, icon = icon, petName = petName.trim(),
        )
        vm.submit({ Ledger.createGoal(it, goal) }) { result ->
            submitting = false
            when (result) {
                is LedgerResult.Ok -> stage = 1
                is LedgerResult.Rejected -> {
                    error = result.reason
                    scope.launch { hold.snapTo(0f) }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
    ScreenScaffold(stringResource(R.string.newgoal_title), onBack) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = purpose,
                onValueChange = { purpose = it.take(Limits.MAX_NAME); error = null },
                label = { Text(stringResource(R.string.field_purpose)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.field_icon), style = MaterialTheme.typography.labelLarge)
            IconPicker(selected = icon, onSelect = { icon = it })

            Text(stringResource(R.string.field_currency), style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (listOf(defaultCurrency(), "EUR", "USD", "GBP", "MXN")).distinct().forEach { code ->
                    FilterChip(
                        selected = currency == code,
                        onClick = { currency = code; error = null },
                        label = { Text(code) },
                    )
                }
            }

            OutlinedTextField(
                value = targetText,
                onValueChange = { text ->
                    targetText = text.filter { it.isDigit() || it == ',' || it == '.' }.take(16)
                    error = null
                },
                label = { Text(stringResource(R.string.field_target, currency)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(6, 12, 18, 24, 36).forEach { m ->
                    FilterChip(
                        selected = monthsText == m.toString(),
                        onClick = { monthsText = m.toString(); error = null },
                        label = { Text(stringResource(R.string.months_chip, m)) },
                    )
                }
            }
            OutlinedTextField(
                value = monthsText,
                onValueChange = { monthsText = it.filter(Char::isDigit).take(3); error = null },
                label = { Text(stringResource(R.string.field_months)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = dayText,
                onValueChange = { dayText = it.filter(Char::isDigit).take(2); error = null },
                label = { Text(stringResource(R.string.field_day)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            if (preview.isNotEmpty()) {
                Text(
                    stringResource(R.string.newgoal_preview_monthly, money(preview.first().amountMinor, currency)),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.newgoal_preview_end, preview.last().dueDate.pretty()),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(stringResource(R.string.newgoal_hint), style = MaterialTheme.typography.bodySmall)
            error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }

            Text("🥚 " + stringResource(R.string.hatch_section), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.hatch_section_body), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = petName,
                onValueChange = { petName = it.take(Limits.MAX_PET_NAME) },
                label = { Text(stringResource(R.string.field_pet_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            HoldToHatchButton(
                enabled = valid && !submitting && stage == 0,
                hold = hold,
                onHaptic = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                onComplete = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    hatch()
                },
            )
            if (!valid) {
                Text(stringResource(R.string.hatch_fill_first), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (stage == 0 && hold.value > 0.02f) HoldGlow(hold.value)
    if (stage >= 1) HatchCeremony(species, petName.trim(), stage, onShown = { stage = 2 }, onDone = onBack)
    }
}

/** Botón que hay que mantener pulsado: la pantalla se ilumina y el huevo se agita hasta eclosionar. */
@Composable
private fun HoldToHatchButton(
    enabled: Boolean,
    hold: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    onHaptic: () -> Unit,
    onComplete: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val label = stringResource(R.string.hatch_hold)
    val a11y = stringResource(R.string.hatch_a11y)
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(shape)
            .background(if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant)
            .semantics { if (enabled) onClick(a11y) { onComplete(); true } }
            .pointerInput(enabled) {
                detectTapGestures(onPress = {
                    if (!enabled) return@detectTapGestures
                    onHaptic()
                    val job = scope.launch {
                        hold.animateTo(1f, tween(1600, easing = LinearEasing))
                        onComplete()
                    }
                    tryAwaitRelease()
                    if (hold.value < 1f) {
                        job.cancel()
                        scope.launch { hold.animateTo(0f, tween(300)) }
                    }
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Color.Transparent),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(hold.value)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Text("🥚  $label", style = MaterialTheme.typography.titleMedium, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun Modifier.fillMaxHeight(): Modifier = this.then(Modifier.fillMaxSize())

/** Resplandor que invade la pantalla mientras se mantiene pulsado, con el huevo agitándose en el centro. */
@Composable
private fun HoldGlow(progress: Float) {
    val brand = LocalBrand.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.5f)
            drawRect(brand.gradientEnd.copy(alpha = 0.55f * progress))
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.95f * progress), Color.White.copy(alpha = 0.0f)),
                    center,
                    size.maxDimension * (0.25f + 0.75f * progress),
                ),
                radius = size.maxDimension,
                center = center,
            )
        }
        Box(Modifier.scale(0.55f + 0.55f * progress)) { EggView(intensity = progress) }
    }
}

/** El huevo eclosiona: mitades que se separan, confeti y la mascota con su rareza. */
@Composable
private fun HatchCeremony(
    species: es.ahorra.core.Species,
    petName: String,
    stage: Int,
    onShown: () -> Unit,
    onDone: () -> Unit,
) {
    val brand = LocalBrand.current
    var confetti by remember { mutableStateOf(0) }
    AnimatedBackdrop(brand.gradientStart, brand.gradientEnd, Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            HatchBurst(species, onShown = { onShown(); confetti++ })
            Spacer(Modifier.height(16.dp))
            if (stage >= 2) {
                Text(stringResource(R.string.hatch_born), color = OnBrand, style = MaterialTheme.typography.headlineMedium)
                Text(
                    petName.ifBlank { stringResource(species.nameRes()) },
                    color = OnBrand,
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                )
                if (petName.isNotBlank()) {
                    Text(stringResource(species.nameRes()), color = OnBrand.copy(alpha = 0.8f), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                RarityChip(species.rarity)
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.hatch_evolves),
                    color = OnBrand.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OnBrand, contentColor = brand.gradientStart),
                ) { Text(stringResource(R.string.hatch_go), style = MaterialTheme.typography.titleMedium) }
            }
        }
        ConfettiBurst(trigger = confetti, modifier = Modifier.fillMaxSize())
    }
}
