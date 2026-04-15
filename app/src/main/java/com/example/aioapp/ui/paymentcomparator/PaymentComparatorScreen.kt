package com.example.aioapp.ui.paymentcomparator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.aioapp.R
import java.util.Locale

private val installmentOptions = listOf(1, 2, 3, 6, 9, 12, 18, 24)

@Composable
fun PaymentComparatorScreen(
    viewModel: PaymentComparatorViewModel,
    padding: PaddingValues,
    navController: NavController
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { InputSection(state, viewModel) }

        state.result?.let { result ->
            item { NominalResultCard(result) }
            if (result.totalRealCost != null) {
                item { RealResultCard(result) }
            }
            result.financingIsAdvantageous?.let {
                item { VerdictBanner(advantageous = it, inflationAdjusted = result.totalRealCost != null) }
            }
            item {
                BreakdownSection(
                    result = result,
                    expanded = state.showBreakdown,
                    onToggle = viewModel::toggleBreakdown
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputSection(state: PaymentComparatorUiState, viewModel: PaymentComparatorViewModel) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.payment_comparator_inputs_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = state.priceInput,
                onValueChange = viewModel::onPriceChange,
                label = { Text(stringResource(R.string.payment_comparator_price)) },
                isError = state.priceError,
                supportingText = if (state.priceError) {
                    { Text(stringResource(R.string.payment_comparator_error_price)) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = stringResource(R.string.payment_comparator_installments_value, state.installmentsInput),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.payment_comparator_installments)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    installmentOptions.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.payment_comparator_installments_value, option))
                            },
                            onClick = {
                                viewModel.onInstallmentsChange(option)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.tnaInput,
                onValueChange = viewModel::onTnaChange,
                label = { Text(stringResource(R.string.payment_comparator_tna)) },
                isError = state.tnaError,
                supportingText = if (state.tnaError) {
                    { Text(stringResource(R.string.payment_comparator_error_tna)) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.inflationInput,
                onValueChange = viewModel::onInflationChange,
                label = { Text(stringResource(R.string.payment_comparator_inflation)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun NominalResultCard(result: PaymentComparisonResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.payment_comparator_nominal_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            ResultRow(stringResource(R.string.payment_comparator_installment_amount), formatCurrency(result.installmentAmount))
            ResultRow(stringResource(R.string.payment_comparator_total_nominal), formatCurrency(result.totalNominalCost))
            ResultRow(stringResource(R.string.payment_comparator_surcharge), formatCurrency(result.nominalSurcharge))
            ResultRow(stringResource(R.string.payment_comparator_tea), formatPercent(result.tea))
        }
    }
}

@Composable
private fun RealResultCard(result: PaymentComparisonResult) {
    val realCost = result.totalRealCost ?: return
    val realSurcharge = result.realSurcharge ?: return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.payment_comparator_real_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            ResultRow(stringResource(R.string.payment_comparator_real_total), formatCurrency(realCost))
            ResultRow(
                stringResource(R.string.payment_comparator_real_surcharge),
                formatCurrency(realSurcharge),
                valueColor = if (realSurcharge < 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun VerdictBanner(advantageous: Boolean, inflationAdjusted: Boolean) {
    val (icon, bgColor, textRes) = if (advantageous) {
        Triple(
            Icons.Default.Check,
            Color(0xFF2E7D32),
            R.string.payment_comparator_verdict_advantageous
        )
    } else {
        Triple(
            Icons.Default.Close,
            Color(0xFFC62828),
            R.string.payment_comparator_verdict_single_cheaper
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor.copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(bgColor, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(
                    stringResource(textRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = bgColor
                )
                if (inflationAdjusted) {
                    Text(
                        stringResource(R.string.payment_comparator_verdict_inflation_basis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakdownSection(
    result: PaymentComparisonResult,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val hasInflation = result.totalRealCost != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.payment_comparator_breakdown_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onToggle) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Header row
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.payment_comparator_breakdown_month),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(0.15f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.payment_comparator_breakdown_nominal),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(if (hasInflation) 0.42f else 0.85f),
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.Bold
                        )
                        if (hasInflation) {
                            Text(
                                stringResource(R.string.payment_comparator_breakdown_real),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.weight(0.43f),
                                textAlign = TextAlign.End,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    result.breakdown.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Text(
                                "${item.month}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(0.15f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                formatCurrency(item.nominalAmount),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(if (hasInflation) 0.42f else 0.85f),
                                textAlign = TextAlign.End
                            )
                            if (hasInflation) {
                                Text(
                                    formatCurrency(item.realAmount),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(0.43f),
                                    textAlign = TextAlign.End,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

private fun formatCurrency(value: Double): String =
    String.format(Locale.getDefault(), "%,.2f", value)

private fun formatPercent(value: Double): String =
    String.format(Locale.getDefault(), "%.2f%%", value * 100.0)
