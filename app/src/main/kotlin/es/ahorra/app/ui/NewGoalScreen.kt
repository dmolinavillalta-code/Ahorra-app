package es.ahorra.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
    var currency by rememberSaveable { mutableStateOf(defaultCurrency()) }
    var targetText by rememberSaveable { mutableStateOf("") }
    var monthsText by rememberSaveable { mutableStateOf("12") }
    var dayText by rememberSaveable { mutableStateOf(minOf(today.dayOfMonth, 28).toString()) }
    var error by remember { mutableStateOf<Reason?>(null) }
    var submitting by remember { mutableStateOf(false) }

    val target = MoneyMath.parseToMinor(targetText, currency)
    val months = monthsText.toIntOrNull()
    val day = dayText.toIntOrNull()
    val valid = purpose.isNotBlank() &&
        target != null && target > 0 &&
        months != null && months in 1..Limits.MAX_MONTHS &&
        day != null && day in 1..31
    val preview = if (valid) PlanCalculator.buildSchedule(target!!, today, months!!, day!!) else emptyList()

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

            Button(
                enabled = valid && !submitting,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    submitting = true
                    val goal = Goal(
                        id = goalId,
                        name = purpose,
                        targetMinor = target!!,
                        currencyCode = currency,
                        startDate = today,
                        months = months!!,
                        paymentDay = day!!,
                    )
                    vm.submit({ Ledger.createGoal(it, goal) }) { result ->
                        submitting = false
                        when (result) {
                            is LedgerResult.Ok -> onBack()
                            is LedgerResult.Rejected -> error = result.reason
                        }
                    }
                },
            ) { Text(stringResource(R.string.action_create)) }
        }
    }
}
