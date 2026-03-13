package com.example.aioapp.ui.paymentcomparator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.aioapp.R
import com.example.aioapp.ui.components.AppTopAppBar
import com.example.aioapp.ui.theme.LocalAppGradient
import com.example.aioapp.ui.theme.RobotoMono
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentComparatorScreen(
    viewModel: PaymentComparatorViewModel,
    padding: PaddingValues,
    navController: NavController
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.feature_payment_comparator),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = RobotoMono,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun InputSection(state: PaymentComparatorUiState, viewModel: PaymentComparatorViewModel) {
    var isCustomMode by remember { mutableStateOf(false) }
    var customInputValue by remember { mutableStateOf("") }
    val gradientColors = LocalAppGradient.current
    val appGradient = Brush.horizontalGradient(gradientColors)
    val cardBorder = Brush.horizontalGradient(gradientColors.map { it.copy(alpha = 0.2f) })

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, cardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.payment_comparator_inputs_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontFamily = RobotoMono
            )

            OutlinedTextField(
                value = state.priceInput,
                onValueChange = viewModel::onPriceChange,
                label = { Text(stringResource(R.string.payment_comparator_price), fontFamily = RobotoMono) },
                isError = state.priceError,
                supportingText = if (state.priceError) {
                    { Text(stringResource(R.string.payment_comparator_error_price), fontFamily = RobotoMono) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontFamily = RobotoMono),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.payment_comparator_installments),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = RobotoMono
                )

                if (!isCustomMode) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3
                    ) {
                        state.availableInstallments.forEach { n ->
                            InstallmentChip(
                                n = n,
                                isSelected = state.installmentsInput == n,
                                onClick = { viewModel.onInstallmentsChange(n) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        InstallmentChip(
                            label = stringResource(R.string.payment_comparator_installments_other),
                            isSelected = false,
                            onClick = { isCustomMode = true },
                            modifier = Modifier.weight(1f)
                        )

                        // Add spacers to maintain 3-column grid
                        val itemsInLastRow = (state.availableInstallments.size + 1) % 3
                        if (itemsInLastRow != 0) {
                            repeat(3 - itemsInLastRow) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customInputValue,
                            onValueChange = { customInputValue = it },
                            placeholder = { Text(stringResource(R.string.payment_comparator_quantity), style = MaterialTheme.typography.bodySmall, fontFamily = RobotoMono) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(fontFamily = RobotoMono),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        IconButton(
                            onClick = {
                                val value = customInputValue.toIntOrNull()
                                if (value != null && value > 0) {
                                    viewModel.addCustomInstallment(value)
                                    customInputValue = ""
                                    isCustomMode = false
                                }
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.payment_comparator_confirm), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { isCustomMode = false; customInputValue = "" }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.payment_comparator_cancel))
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.tnaInput,
                onValueChange = viewModel::onTnaChange,
                label = { Text(stringResource(R.string.payment_comparator_tna), fontFamily = RobotoMono) },
                isError = state.tnaError,
                supportingText = if (state.tnaError) {
                    { Text(stringResource(R.string.payment_comparator_error_tna), fontFamily = RobotoMono) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontFamily = RobotoMono),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.inflationInput,
                onValueChange = viewModel::onInflationChange,
                label = { Text(stringResource(R.string.payment_comparator_inflation), fontFamily = RobotoMono) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontFamily = RobotoMono),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun InstallmentChip(
    modifier: Modifier = Modifier,
    n: Int? = null,
    label: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val gradientColors = LocalAppGradient.current
    val appGradient = Brush.horizontalGradient(gradientColors)
    val text = label ?: n?.toString() ?: ""

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) Modifier.background(appGradient)
                else Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = RobotoMono,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun NominalResultCard(result: PaymentComparisonResult) {
    val gradientColors = LocalAppGradient.current
    val cardBorder = Brush.horizontalGradient(gradientColors.map { it.copy(alpha = 0.2f) })

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, cardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.payment_comparator_nominal_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontFamily = RobotoMono
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ResultRow(stringResource(R.string.payment_comparator_installment_amount), formatCurrency(result.installmentAmount))
            ResultRow(
                stringResource(R.string.payment_comparator_total_nominal),
                formatCurrency(result.totalNominalCost),
                isHighlighted = true
            )
            ResultRow(stringResource(R.string.payment_comparator_surcharge), formatCurrency(result.nominalSurcharge))
            ResultRow(stringResource(R.string.payment_comparator_tea), formatPercent(result.tea))
        }
    }
}

@Composable
private fun RealResultCard(result: PaymentComparisonResult) {
    val realCost = result.totalRealCost ?: return
    val realSurcharge = result.realSurcharge ?: return
    val gradientColors = LocalAppGradient.current
    val cardBorder = Brush.horizontalGradient(gradientColors.map { it.copy(alpha = 0.2f) })

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, cardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.payment_comparator_real_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontFamily = RobotoMono
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ResultRow(
                stringResource(R.string.payment_comparator_real_total),
                formatCurrency(realCost),
                isHighlighted = true
            )
            ResultRow(
                stringResource(R.string.payment_comparator_real_surcharge),
                formatCurrency(realSurcharge),
                valueColor = if (realSurcharge < 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun VerdictBanner(advantageous: Boolean, inflationAdjusted: Boolean) {
    val gradientColors = LocalAppGradient.current
    val (icon, color, textRes) = if (advantageous) {
        Triple(
            Icons.Default.Check,
            Color(0xFF4CAF50),
            R.string.payment_comparator_verdict_advantageous
        )
    } else {
        Triple(
            Icons.Default.Close,
            Color(0xFFE57373),
            R.string.payment_comparator_verdict_single_cheaper
        )
    }

    val bannerBorder = Brush.horizontalGradient(gradientColors.map { it.copy(alpha = 0.3f) })

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, bannerBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape)
                    .border(2.5.dp, color.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            }
            Column {
                Text(
                    stringResource(textRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontFamily = RobotoMono
                )
                if (inflationAdjusted) {
                    Text(
                        stringResource(R.string.payment_comparator_verdict_inflation_basis),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = RobotoMono
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

    val gradientColors = LocalAppGradient.current
    val cardBorder = Brush.horizontalGradient(gradientColors.map { it.copy(alpha = 0.2f) })

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, cardBorder, RoundedCornerShape(16.dp))
            .animateContentSize()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
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
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = RobotoMono
                )
                IconButton(onClick = onToggle) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Header row
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.payment_comparator_breakdown_month),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(0.15f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMono,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.payment_comparator_breakdown_nominal),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(if (hasInflation) 0.42f else 0.85f),
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMono,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (hasInflation) {
                            Text(
                                stringResource(R.string.payment_comparator_breakdown_real),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.weight(0.43f),
                                textAlign = TextAlign.End,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMono,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    result.breakdown.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                "${item.month}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(0.15f),
                                textAlign = TextAlign.Center,
                                fontFamily = RobotoMono,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                formatCurrency(item.nominalAmount),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(if (hasInflation) 0.42f else 0.85f),
                                textAlign = TextAlign.End,
                                fontFamily = RobotoMono,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            if (hasInflation) {
                                Text(
                                    formatCurrency(item.realAmount),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(0.43f),
                                    textAlign = TextAlign.End,
                                    color = Color(0xFF4CAF50),
                                    fontFamily = RobotoMono,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    isHighlighted: Boolean = false
) {
    val gradientColors = LocalAppGradient.current
    val highlightBg = Brush.horizontalGradient(gradientColors.map { it.copy(alpha = 0.15f) })
    val highlightBorder = Brush.horizontalGradient(gradientColors.map { it.copy(alpha = 0.3f) })

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isHighlighted) Modifier
                    .background(highlightBg)
                    .border(1.dp, highlightBorder, RoundedCornerShape(12.dp))
                else Modifier
            )
            .padding(horizontal = 12.dp, vertical = if (isHighlighted) 10.dp else 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isHighlighted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = RobotoMono,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) MaterialTheme.colorScheme.onSurface else valueColor,
            fontFamily = RobotoMono,
            maxLines = 1
        )
    }
}

private fun formatCurrency(value: Double): String =
    String.format(Locale.getDefault(), "%,.2f", value)

private fun formatPercent(value: Double): String =
    String.format(Locale.getDefault(), "%.2f%%", value * 100.0)
