package com.example.aioapp.ui.paymentcomparator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.aioapp.R
import com.example.aioapp.ui.theme.LocalAppGradient
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val EDITOR_ANIM_MS = 350
private val Green = Color(0xFF2E7D32)
private val Red = Color(0xFFC62828)

@Composable
fun PaymentComparatorScreen(
    viewModel: PaymentComparatorViewModel,
    padding: PaddingValues,
    navController: NavController
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val result = state.result
    val listState = rememberLazyListState()
    // A DataStore read takes milliseconds; drawing before it would show the form open and then animate it closed
    if (!state.loaded) return

    // The first valid keystroke inserts the verdict and metrics above the form. Left alone, the list shows
    // them and pushes the field being typed in under the keyboard; scrolling to the form keeps it in place.
    val hasResult = result != null
    var previousHasResult by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(hasResult) {
        if (previousHasResult == false && hasResult && listState.firstVisibleItemIndex == 0) {
            listState.scrollToItem(index = 2, scrollOffset = listState.firstVisibleItemScrollOffset)
        }
        previousHasResult = hasResult
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            // Without it the keyboard covers the lower fields instead of shrinking the list
            .consumeWindowInsets(padding)
            .imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (result != null) {
            item(key = "verdict") { VerdictHero(result) }
            item(key = "metrics") { MetricsGrid(result) }
        }

        // Stable key: when the first valid keystroke inserts the results above, the lazy list
        // keeps this item (and the focused text field) instead of re-creating it
        item(key = "inputs") {
            InputsCard(
                state = state,
                result = result,
                viewModel = viewModel,
                // Without a result there is nothing to summarize, so the form stays open
                expanded = state.showInputs || result == null
            )
        }

        if (result != null) {
            item(key = "breakdown") {
                BreakdownSection(
                    result = result,
                    expanded = state.showBreakdown,
                    onToggle = viewModel::toggleBreakdown
                )
            }
        }
    }
}

@Composable
private fun VerdictHero(result: PaymentComparisonResult) {
    val realCost = result.totalRealCost
    val advantageous = result.financingIsAdvantageous

    val color = when (advantageous) {
        true -> Green
        false -> Red
        null -> MaterialTheme.colorScheme.onSurface
    }
    val title = when (advantageous) {
        true -> stringResource(R.string.payment_comparator_verdict_advantageous)
        false -> stringResource(R.string.payment_comparator_verdict_single_cheaper)
        null -> stringResource(R.string.payment_comparator_surcharge)
    }
    val amount = when {
        advantageous == true && realCost != null -> formatCurrency(result.price - realCost)
        realCost != null -> "+" + formatCurrency(realCost - result.price)
        else -> "+" + formatCurrency(result.nominalSurcharge)
    }
    val caption = when (advantageous) {
        true -> stringResource(R.string.payment_comparator_verdict_saving_caption)
        false -> stringResource(R.string.payment_comparator_verdict_extra_caption)
        null -> stringResource(R.string.payment_comparator_verdict_no_inflation_caption)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (advantageous == null) MaterialTheme.colorScheme.surfaceVariant else color.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = color)
            Text(
                amount,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(caption, style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}

@Composable
private fun MetricsGrid(result: PaymentComparisonResult) {
    val tiles = buildList {
        add(stringResource(R.string.payment_comparator_metric_installment) to formatCurrency(result.installmentAmount))
        add(stringResource(R.string.payment_comparator_metric_tea) to formatPercent(result.tea))
        add(stringResource(R.string.payment_comparator_metric_total_nominal) to formatCurrency(result.totalNominalCost))
        result.totalRealCost?.let {
            add(stringResource(R.string.payment_comparator_metric_total_real) to formatCurrency(it))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) ->
                    MetricTile(label, value, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * One card whose header is always visible: the input summary when collapsed, the title when open.
 * Only the fields below the header unroll (top to bottom) and fold back (bottom up), so the card
 * itself never disappears and reappears.
 */
@Composable
private fun InputsCard(
    state: PaymentComparatorUiState,
    result: PaymentComparisonResult?,
    viewModel: PaymentComparatorViewModel,
    expanded: Boolean
) {
    val collapsible = result != null

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .then(if (collapsible) Modifier.clickable(onClick = viewModel::toggleInputs) else Modifier),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Crossfade(
                    targetState = expanded || result == null,
                    animationSpec = tween(EDITOR_ANIM_MS / 2),
                    modifier = Modifier.weight(1f),
                    label = "inputsHeader"
                ) { showTitle ->
                    if (showTitle || result == null) {
                        Text(
                            text = stringResource(R.string.payment_comparator_inputs_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            inputsSummary(state, result),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (collapsible) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.Edit,
                        contentDescription = if (expanded) "Collapse"
                        else stringResource(R.string.payment_comparator_edit_inputs),
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(if (expanded) 24.dp else 20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                // Clipped while growing from the top: the first fields show first, the rest unrolls downwards.
                // Closing folds it back from the bottom up.
                enter = expandVertically(tween(EDITOR_ANIM_MS), expandFrom = Alignment.Top) +
                    fadeIn(tween(EDITOR_ANIM_MS / 2)),
                exit = shrinkVertically(tween(EDITOR_ANIM_MS), shrinkTowards = Alignment.Top) +
                    fadeOut(tween(EDITOR_ANIM_MS / 2, delayMillis = EDITOR_ANIM_MS / 2))
            ) {
                InputFields(state, viewModel)
            }
        }
    }
}

@Composable
private fun inputsSummary(state: PaymentComparatorUiState, result: PaymentComparisonResult): String =
    buildList {
        add(formatCurrency(result.price))
        add(stringResource(R.string.payment_comparator_installments_value, result.installments))
        add(stringResource(R.string.payment_comparator_summary_tna, state.tnaInput))
        if (state.inflationInput.isNotBlank()) {
            add(stringResource(R.string.payment_comparator_summary_inflation, state.inflationInput))
        }
    }.joinToString(" · ")

@Composable
private fun InputFields(state: PaymentComparatorUiState, viewModel: PaymentComparatorViewModel) {
    Column(
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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

        InstallmentsPicker(state, viewModel)

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

        InflationField(
            value = state.inflationInput,
            status = state.inflationStatus,
            onValueChange = viewModel::onInflationChange,
            onRetry = viewModel::fetchInflation
        )
    }
}

/**
 * Same corner and border as the outlined text fields (1dp outline, 2dp when active),
 * with the app gradient marking the selection, like other selected items in the app.
 */
@Composable
private fun InstallmentChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val gradientColors = LocalAppGradient.current
    val selectedBorder = remember(gradientColors) { BorderStroke(2.dp, Brush.horizontalGradient(gradientColors)) }

    FilterChip(
        selected = selected,
        onClick = onClick,
        // 32dp = the 48dp minimum minus the chip's 8dp label padding on each side, so short labels stay centered
        label = { Text(text, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 32.dp)) },
        // Below the 48dp touch target the chip gets centered with extra margin, misaligning the rows
        modifier = Modifier.widthIn(min = 48.dp),
        shape = OutlinedTextFieldDefaults.shape,
        border = if (selected) selectedBorder else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color.Transparent,
            selectedLabelColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/** Preset counts as chips, plus a last "Other" chip that reveals a field for any count. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InstallmentsPicker(state: PaymentComparatorUiState, viewModel: PaymentComparatorViewModel) {
    val focusRequester = remember { FocusRequester() }
    // Focus the field only when the user picks "Other", not when the screen opens already on it
    var focusCustomField by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.payment_comparator_installments),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            installmentOptions.forEach { option ->
                InstallmentChip(
                    text = "$option",
                    selected = !state.customInstallmentsSelected && state.installmentsInput == option,
                    onClick = { viewModel.onInstallmentsChange(option) }
                )
            }
            InstallmentChip(
                text = stringResource(R.string.payment_comparator_installments_custom),
                selected = state.customInstallmentsSelected,
                onClick = {
                    focusCustomField = true
                    viewModel.onCustomInstallmentsSelected()
                }
            )
        }

        AnimatedVisibility(visible = state.customInstallmentsSelected) {
            OutlinedTextField(
                value = state.customInstallmentsInput,
                onValueChange = viewModel::onCustomInstallmentsChange,
                label = { Text(stringResource(R.string.payment_comparator_installments_custom_label)) },
                isError = state.installmentsError,
                supportingText = if (state.installmentsError) {
                    { Text(stringResource(R.string.payment_comparator_error_installments, MAX_INSTALLMENTS)) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }
    }

    LaunchedEffect(focusCustomField, state.customInstallmentsSelected) {
        if (focusCustomField && state.customInstallmentsSelected) {
            focusRequester.requestFocus()
            focusCustomField = false
        }
    }
}

/** Pre-filled from the official series; stays editable so it works offline or when the API fails. */
@Composable
private fun InflationField(
    value: String,
    status: InflationStatus,
    onValueChange: (String) -> Unit,
    onRetry: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val supporting = when (status) {
        InflationStatus.Loading -> stringResource(R.string.payment_comparator_inflation_loading)
        is InflationStatus.Fetched -> stringResource(
            R.string.payment_comparator_inflation_fetched,
            status.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
        )
        InflationStatus.Failed -> stringResource(R.string.payment_comparator_inflation_failed)
        InflationStatus.Manual -> stringResource(R.string.payment_comparator_inflation_manual)
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.payment_comparator_inflation)) },
        supportingText = {
            Text(
                supporting,
                color = if (status == InflationStatus.Failed) MaterialTheme.colorScheme.error else Color.Unspecified
            )
        },
        trailingIcon = {
            when (status) {
                InflationStatus.Loading -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                InflationStatus.Failed, InflationStatus.Manual -> IconButton(onClick = onRetry) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.payment_comparator_inflation_retry)
                    )
                }
                is InflationStatus.Fetched -> Unit
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
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
                                    color = Green
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

private fun formatCurrency(value: Double): String =
    String.format(Locale.getDefault(), "$%,.2f", value)

private fun formatPercent(value: Double): String =
    String.format(Locale.getDefault(), "%.2f%%", value * 100.0)
