package com.example.aioapp.ui.bbq

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.aioapp.R
import com.example.aioapp.ui.components.AppTopAppBar
import com.example.aioapp.ui.theme.LocalAppGradient
import com.example.aioapp.ui.theme.RobotoMono
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BBQScreen(
    viewModel: BBQViewModel,
    navController: NavController,
    padding: PaddingValues
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.feature_bbq),
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
            item { ExpenseSplitterSection(state, viewModel) }
            item { ResultsSection(state) }
        }
    }
}

@Composable
private fun CustomCard(content: @Composable ColumnScope.() -> Unit) {
    val gradientColors = LocalAppGradient.current
    val cardBorder = Brush.horizontalGradient(gradientColors.map { it.copy(alpha = 0.2f) })

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, cardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            content()
        }
    }
}

@Composable
private fun InputSection(state: BBQUiState, viewModel: BBQViewModel) {
    CustomCard {
        Text(
            text = stringResource(R.string.bbq_participants),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            fontFamily = RobotoMono
        )

        CounterRow(
            label = stringResource(R.string.bbq_men),
            count = state.menCount,
            onCountChange = viewModel::updateMenCount
        )
        CounterRow(
            label = stringResource(R.string.bbq_women),
            count = state.womenCount,
            onCountChange = viewModel::updateWomenCount
        )
        CounterRow(
            label = stringResource(R.string.bbq_children),
            count = state.childrenCount,
            onCountChange = viewModel::updateChildrenCount
        )
    }
}

@Composable
private fun CounterRow(
    label: String,
    count: Int,
    onCountChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = RobotoMono
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { onCountChange(count - 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Decrease",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            OutlinedTextField(
                value = if (count == 0) "" else count.toString(),
                onValueChange = { newValueStr ->
                    val newValue = newValueStr.toIntOrNull() ?: 0
                    onCountChange(newValue)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(64.dp),
                textStyle = TextStyle(
                    fontFamily = RobotoMono,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                ),
                shape = RoundedCornerShape(8.dp)
            )

            IconButton(
                onClick = { onCountChange(count + 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpenseSplitterSection(state: BBQUiState, viewModel: BBQViewModel) {
    CustomCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.bbq_expense_divider),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontFamily = RobotoMono
            )
            Switch(
                checked = state.enableExpenseSplitter,
                onCheckedChange = viewModel::toggleExpenseSplitter
            )
        }
        
        AnimatedVisibility(visible = state.enableExpenseSplitter) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.totalCostInput,
                    onValueChange = viewModel::updateTotalCost,
                    label = { Text(stringResource(R.string.bbq_total_cost), fontFamily = RobotoMono) },
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
                
                state.costPerAdult?.let { cost ->
                    ResultRow(
                        label = String.format(stringResource(R.string.bbq_cost_per_adult).replace("%1\$s", "")),
                        value = formatCurrency(cost),
                        isHighlighted = true
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultsSection(state: BBQUiState) {
    CustomCard {
        Text(
            text = stringResource(R.string.bbq_results),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            fontFamily = RobotoMono
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        if (state.totalMeatKg > 0) {
            ResultRow(label = stringResource(R.string.bbq_food_meat), value = String.format(Locale.getDefault(), stringResource(R.string.bbq_results_meat), state.totalMeatKg))
        }
        if (state.totalBreadUnits > 0) {
            ResultRow(label = stringResource(R.string.bbq_food_bread), value = String.format(Locale.getDefault(), stringResource(R.string.bbq_results_bread), state.totalBreadUnits))
        }
        if (state.totalCoalKg > 0) {
            ResultRow(label = stringResource(R.string.bbq_food_coal), value = String.format(Locale.getDefault(), stringResource(R.string.bbq_results_coal), state.totalCoalKg))
        }
        if (state.totalDrinksLiters > 0) {
            ResultRow(label = stringResource(R.string.bbq_drinks), value = String.format(Locale.getDefault(), stringResource(R.string.bbq_results_drinks), state.totalDrinksLiters))
        }
    }
}

private fun BBQUiState.totalAdultsOrChildren(): Int = menCount + womenCount + childrenCount

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
    String.format(Locale.getDefault(), "$%,.2f", value)
