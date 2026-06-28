package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Budget
import com.example.data.Category
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetManagerScreen(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit
) {
    val budgets by viewModel.allBudgets.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()

    var limitAmountStr by remember { mutableStateOf("") }
    var selectedCategoryName by remember { mutableStateOf<String?>(null) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(categories) {
        if (selectedCategoryName == null && categories.isNotEmpty()) {
            selectedCategoryName = categories.first().name
        }
    }

    val currentMonthYear = SimpleDateFormat("MM/yyyy", Locale.US).format(Date())

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Budget Planner", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("budget_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Set Monthly Limit
            item {
                Text(
                    text = "Configure Monthly Limits",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Input Form Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        
                        // Category Selector Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { categoryDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth().testTag("budget_category_trigger")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Category: ${selectedCategoryName ?: "General"}", color = MaterialTheme.colorScheme.onSurface)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            DropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 240.dp)
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            selectedCategoryName = cat.name
                                            categoryDropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("budget_category_item_${cat.name}")
                                    )
                                }
                            }
                        }

                        // Limit Amount Input
                        OutlinedTextField(
                            value = limitAmountStr,
                            onValueChange = { limitAmountStr = it },
                            label = { Text("Limit Amount (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("budget_limit_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        )

                        // Save Button
                        Button(
                            onClick = {
                                val amt = limitAmountStr.toDoubleOrNull()
                                if (amt != null && amt > 0.0) {
                                    viewModel.insertBudget(
                                        isGlobal = false,
                                        categoryName = selectedCategoryName,
                                        limitAmount = amt,
                                        monthYear = currentMonthYear
                                    )
                                    limitAmountStr = ""
                                }
                            },
                            enabled = limitAmountStr.toDoubleOrNull() != null,
                            modifier = Modifier.fillMaxWidth().testTag("save_budget_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Set Budget Limit", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Section: Current active budgets list
            item {
                Text(
                    text = "Current Monthly Budgets ($currentMonthYear)",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (budgets.isEmpty()) {
                item {
                    Text(
                        text = "No budgets configured for this month.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                items(budgets) { budget ->
                    // Calculate spent amount
                    val matchingCategoryNames = if (budget.isGlobal) null 
                        else budget.categoryName?.let { getCategoryAndChildrenNames(it, categories) }

                    val spent = expenses.filter { exp ->
                        val expenseMonthYear = SimpleDateFormat("MM/yyyy", Locale.US).format(Date(exp.date))
                        val monthMatches = expenseMonthYear == budget.monthYear
                        val categoryMatches = budget.isGlobal || (matchingCategoryNames?.contains(exp.category) ?: false)
                        monthMatches && categoryMatches
                    }.sumOf { it.amount }

                    val percent = if (budget.limitAmount > 0.0) (spent / budget.limitAmount).toFloat() else 0f
                    val displayPercent = percent * 100f
                    
                    val progressColor = when {
                        percent < 0.70f -> Color(0xFF1B5E20) // Green
                        percent < 0.90f -> Color(0xFFE65100) // Orange
                        else -> Color(0xFFB71C1C) // Red
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("budget_item_${budget.categoryName ?: "Global"}")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(progressColor.copy(alpha = 0.15f))
                                    ) {
                                        Icon(
                                            imageVector = if (budget.isGlobal) Icons.Default.Public else Icons.Default.Category,
                                            contentDescription = "Budget Icon",
                                            tint = progressColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (budget.isGlobal) "Global Budget" else "Category: ${budget.categoryName}",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Spent ₹${formatAmount(spent)} of ₹${formatAmount(budget.limitAmount)}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.deleteBudget(budget) },
                                    modifier = Modifier.testTag("delete_budget_${budget.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Budget", tint = MaterialTheme.colorScheme.error)
                                }
                            }

                            // Progress Bar
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                LinearProgressIndicator(
                                    progress = { percent.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = progressColor,
                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", displayPercent)}% used",
                                        color = progressColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (spent > budget.limitAmount) {
                                        Text(
                                            text = "Over budget by ₹${formatAmount(spent - budget.limitAmount)}",
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
