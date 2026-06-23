package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import android.widget.Toast
import android.content.Intent
import android.content.Context
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: ExpenseViewModel,
    onNavigateToAddExpense: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("EMI Loan", "Interest", "GST / Tax", "Cash Counter")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tools_screen_root")
    ) {
        // Upper atmospheric section with title
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = if (selectedTab == 3) "Cash & Online Counter" else "Ledger Audit Utilities",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (selectedTab == 3) {
                    "Count physical currency denominations and track digital online wallet balances dynamically."
                } else {
                    "Natively styled calculation engines for EMI, Interest dynamic yields, and precise tax breakdowns."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        // Custom stylized material TabRow
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            tabTitles.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Tab(
                    selected = isSelected,
                    onClick = { selectedTab = index },
                    modifier = Modifier.testTag("tools_tab_$index"),
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content area per tab
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            when (selectedTab) {
                0 -> EmiCalculatorPanel(viewModel, onNavigateToAddExpense)
                1 -> InterestCalculatorPanel(viewModel, onNavigateToAddExpense)
                2 -> GstCalculatorPanel(viewModel, onNavigateToAddExpense)
                3 -> CashCalculatorPanel()
            }
        }
    }
}

@Composable
fun EmiCalculatorPanel(
    viewModel: ExpenseViewModel,
    onNavigateToAddExpense: () -> Unit
) {
    val context = LocalContext.current
    var principalText by remember { mutableStateOf("10000") }
    var rateText by remember { mutableStateOf("8.5") }
    var yearsText by remember { mutableStateOf("5") }

    // Recompiles calculation outcomes as inputs change
    val principal = principalText.toDoubleOrNull() ?: 0.0
    val annualRate = rateText.toDoubleOrNull() ?: 0.0
    val years = yearsText.toDoubleOrNull() ?: 0.0

    val emi = remember(principal, annualRate, years) {
        if (principal <= 0.0 || annualRate < 0.0 || years <= 0.0 || principal.isNaN() || annualRate.isNaN() || years.isNaN() || !principal.isFinite() || !annualRate.isFinite() || !years.isFinite()) 0.0
        else {
            val r = (annualRate / 12.0) / 100.0
            val n = Math.round(years * 12).toInt()
            if (n <= 0) 0.0
            else if (n > 600) 0.0
            else if (r == 0.0) principal / n
            else {
                val factor = Math.pow(1.0 + r, n.toDouble())
                if (factor.isInfinite() || factor.isNaN() || factor == 1.0) 0.0
                else principal * r * (factor / (factor - 1.0))
            }
        }
    }

    val totalPayment = if (emi > 0.0 && years > 0.0) {
        val n = Math.round(years * 12).toInt()
        emi * n
    } else 0.0
    val totalInterest = if (totalPayment > 0.0) totalPayment - principal else 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Plan Loan Installments",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = principalText,
                        onValueChange = { principalText = it },
                        label = { Text("Loan Principal (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("emi_principal_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = rateText,
                            onValueChange = { rateText = it },
                            label = { Text("Interest Rate (% p.a.)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("emi_rate_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        OutlinedTextField(
                            value = yearsText,
                            onValueChange = { yearsText = it },
                            label = { Text("Tenure (Years)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("emi_tenure_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "EQUATED MONTHLY INSTALLMENT (EMI)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "₹${formatAmount(emi)}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total Principal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text("₹${formatAmount(principal)}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Total Interest", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text("₹${formatAmount(totalInterest)}", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text("Payment Sum", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text("₹${formatAmount(totalPayment)}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        item {
            Column {
                Button(
                    onClick = {
                        if (emi > 0.0) {
                            viewModel.setPendingExpenseAmount(emi)
                            onNavigateToAddExpense()
                            Toast.makeText(context, "Carried monthly EMI over to expense form!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Calculate a valid EMI amount first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("emi_save_expense_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "", tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save EMI as Expense", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (totalPayment > 0.0) {
                            viewModel.setPendingExpenseAmount(totalPayment)
                            onNavigateToAddExpense()
                            Toast.makeText(context, "Carried full loan sum over as expense", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Calculate a valid payment sum first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("emi_save_total_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "", tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Loan Sum as Expense", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun InterestCalculatorPanel(
    viewModel: ExpenseViewModel,
    onNavigateToAddExpense: () -> Unit
) {
    val context = LocalContext.current
    var principalText by remember { mutableStateOf("5000") }
    var rateText by remember { mutableStateOf("6") }
    var yearsText by remember { mutableStateOf("3") }
    var isCompound by remember { mutableStateOf(true) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var compoundFrequency by remember { mutableStateOf(12) } // 12 = Monthly, 4 = Quarterly, 1 = Yearly

    val principal = principalText.toDoubleOrNull() ?: 0.0
    val rate = rateText.toDoubleOrNull() ?: 0.0
    val years = yearsText.toDoubleOrNull() ?: 0.0

    val outputs = remember(principal, rate, years, isCompound, compoundFrequency) {
        if (principal <= 0.0 || rate < 0.0 || years < 0.0 || principal.isNaN() || rate.isNaN() || years.isNaN() || !principal.isFinite() || !rate.isFinite() || !years.isFinite()) Pair(0.0, 0.0)
        else {
            if (!isCompound) {
                val interest = (principal * rate * years) / 100
                Pair(interest, principal + interest)
            } else {
                val n = compoundFrequency.toDouble()
                val total = principal * Math.pow(1 + (rate / (n * 100)), n * years)
                Pair(total - principal, total)
            }
        }
    }

    val interestEarned = outputs.first
    val accumulatedTotal = outputs.second

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Wealth & Debt Growth Evaluator",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // Mode Selector (Simple vs Compound Switch buttons)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isCompound) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { isCompound = false }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("interest_type_simple"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Simple",
                                    color = if (!isCompound) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCompound) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { isCompound = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("interest_type_compound"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Compound",
                                    color = if (isCompound) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = principalText,
                        onValueChange = { principalText = it },
                        label = { Text("Principal Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("interest_principal_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = rateText,
                            onValueChange = { rateText = it },
                            label = { Text("Annual Rate (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("interest_rate_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        OutlinedTextField(
                            value = yearsText,
                            onValueChange = { yearsText = it },
                            label = { Text("Period (Years)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("interest_years_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    if (isCompound) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Compounding Habit: ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf(12 to "Monthly", 4 to "Quarterly", 1 to "Yearly").forEach { (freq, label) ->
                                    val isSelected = compoundFrequency == freq
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
                                            .clickable { compoundFrequency = freq }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ACCUMULATED VALUE",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "₹${formatAmount(accumulatedTotal)}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Initial Capital", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text("₹${formatAmount(principal)}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text("Total Yield (Interest)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text("₹${formatAmount(interestEarned)}", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        item {
            Column {
                Button(
                    onClick = {
                        if (interestEarned > 0.0) {
                            viewModel.setPendingExpenseAmount(interestEarned)
                            onNavigateToAddExpense()
                            Toast.makeText(context, "Carried calculated interest over as expense amount!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Set some Principal and interest variables first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("interest_save_expense_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "", tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Yield (Interest) as Expense", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (accumulatedTotal > 0.0) {
                            viewModel.setPendingExpenseAmount(accumulatedTotal)
                            onNavigateToAddExpense()
                            Toast.makeText(context, "Carried total accrued value over to expense form!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Enter valid values first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("interest_save_total_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "", tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Accrued Sum as Expense", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun GstCalculatorPanel(
    viewModel: ExpenseViewModel,
    onNavigateToAddExpense: () -> Unit
) {
    val context = LocalContext.current
    var baseText by remember { mutableStateOf("120") }
    var taxRateText by remember { mutableStateOf("18") }
    var isInclusive by remember { mutableStateOf(false) } // False = Tax Exclusive (Add), True = Tax Inclusive (Extract)

    val baseAmount = baseText.toDoubleOrNull() ?: 0.0
    val taxRate = (taxRateText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)

    val outcomes = remember(baseAmount, taxRate, isInclusive) {
        if (baseAmount <= 0.0 || taxRate < 0.0 || baseAmount.isNaN() || taxRate.isNaN() || !baseAmount.isFinite() || !taxRate.isFinite()) Triple(0.0, 0.0, 0.0)
        else {
            if (!isInclusive) {
                // Add Tax
                val tax = (baseAmount * taxRate) / 100
                Triple(baseAmount, tax, baseAmount + tax)
            } else {
                // Extract Tax
                val originalBase = baseAmount / (1 + (taxRate / 100))
                val tax = baseAmount - originalBase
                Triple(originalBase, tax, baseAmount)
            }
        }
    }

    val finalBase = outcomes.first
    val calculatedTax = outcomes.second
    val finalTotal = outcomes.third

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GST & General Sales Tax Calculator",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // Add vs Extract Toggle
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isInclusive) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { isInclusive = false }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("gst_type_add"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Add Tax",
                                    color = if (!isInclusive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isInclusive) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { isInclusive = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("gst_type_extract"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Split/Extract",
                                    color = if (isInclusive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = baseText,
                        onValueChange = { baseText = it },
                        label = { Text(if (!isInclusive) "Original Price (Excl. Tax) (₹)" else "Final Price (Incl. Tax) (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("gst_amount_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = taxRateText,
                            onValueChange = { taxRateText = it },
                            label = { Text("Tax Slab (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("gst_rate_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        // Quick Tax Slab helpers
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Slab Presets:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("5", "12", "18", "28").forEach { preset ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (taxRateText == preset) MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                            .border(1.dp, if (taxRateText == preset) MaterialTheme.colorScheme.secondary else Color.Transparent, RoundedCornerShape(6.dp))
                                            .clickable { taxRateText = preset }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(preset + "%", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL TAX COMPONENT",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "₹${formatAmount(calculatedTax)}",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Tax-Exclusive Price", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text("₹${formatAmount(finalBase)}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Tax-Inclusive Price", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text("₹${formatAmount(finalTotal)}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        item {
            Column {
                Button(
                    onClick = {
                        if (finalTotal > 0.0) {
                            viewModel.setPendingExpenseAmount(finalTotal)
                            onNavigateToAddExpense()
                            Toast.makeText(context, "Carried final tax-inclusive price as expense amount!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Evaluate a valid pricing amount first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("gst_save_total_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "", tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Total (Tax-Inclusive) as Expense", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (calculatedTax > 0.0) {
                            viewModel.setPendingExpenseAmount(calculatedTax)
                            onNavigateToAddExpense()
                            Toast.makeText(context, "Carried purely calculated tax amount over!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Calculate valid tax variables first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("gst_save_tax_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "", tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Tax Amount purely as Expense", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CashCalculatorPanel() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("cash_calculator_prefs", Context.MODE_PRIVATE) }
    
    var selectedCurrencyCode by remember { mutableStateOf(sharedPrefs.getString("selected_currency_code", "INR") ?: "INR") }
    var selectedCurrencySymbol by remember { mutableStateOf(sharedPrefs.getString("selected_currency_symbol", "₹") ?: "₹") }
    var showOnline by remember { mutableStateOf(sharedPrefs.getBoolean("show_online", false)) }
    
    var allDenominationsStr by remember { 
        mutableStateOf(sharedPrefs.getString("all_denominations", "2000,500,200,100,50,20,10,5,2,1") ?: "2000,500,200,100,50,20,10,5,2,1") 
    }
    var enabledDenominationsStr by remember { 
        mutableStateOf(sharedPrefs.getString("enabled_denominations", "2000,500,200,100,50,20,10,5,2,1") ?: "2000,500,200,100,50,20,10,5,2,1") 
    }

    val defaultDenominations = remember { listOf(2000, 500, 200, 100, 50, 20, 10, 5, 2, 1) }

    val allDenominations = remember(allDenominationsStr) {
        allDenominationsStr.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .distinct()
            .sortedDescending()
    }

    val enabledDenominationsSet = remember(enabledDenominationsStr) {
        enabledDenominationsStr.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }

    val denominations = remember(enabledDenominationsSet) {
        enabledDenominationsSet.sortedDescending()
    }

    val countTextMap = remember { mutableStateMapOf<Int, String>() }

    var allOnlineWalletsStr by remember {
        mutableStateOf(sharedPrefs.getString("all_online_wallets", "Amazon Pay,UPI / Google Pay,Card / NetBanking") ?: "Amazon Pay,UPI / Google Pay,Card / NetBanking")
    }
    var enabledOnlineWalletsStr by remember {
        mutableStateOf(sharedPrefs.getString("enabled_online_wallets", "Amazon Pay,UPI / Google Pay,Card / NetBanking") ?: "Amazon Pay,UPI / Google Pay,Card / NetBanking")
    }
    val defaultOnlineWallets = remember { listOf("Amazon Pay", "UPI / Google Pay", "Card / NetBanking") }

    val allOnlineWallets = remember(allOnlineWalletsStr) {
        allOnlineWalletsStr.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    val enabledOnlineWalletsSet = remember(enabledOnlineWalletsStr) {
        enabledOnlineWalletsStr.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    val enabledOnlineWallets = remember(enabledOnlineWalletsSet) {
        allOnlineWallets.filter { enabledOnlineWalletsSet.contains(it) }
    }

    val onlineWalletTextMap = remember { mutableStateMapOf<String, String>() }

    val counts = denominations.associateWith { countTextMap[it]?.toIntOrNull() ?: 0 }
    val subtotals = denominations.associateWith { it.toLong() * (counts[it] ?: 0) }
    val cashGrandTotal = subtotals.values.sum()
    var showClearConfirmation by remember { mutableStateOf(false) }

    val onlineTotal = if (showOnline) {
        enabledOnlineWallets.sumOf { onlineWalletTextMap[it]?.toDoubleOrNull() ?: 0.0 }
    } else 0.0

    val grandTotal = cashGrandTotal + onlineTotal
    val totalNotes = counts.values.sum()

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showDenomManager by remember { mutableStateOf(false) }
    var showOnlineWalletManager by remember { mutableStateOf(false) }
    var showAddCustomDenomDialog by remember { mutableStateOf(false) }
    var showAddCustomOnlineWalletDialog by remember { mutableStateOf(false) }

    val currencyOptions = listOf(
        "INR" to "₹",
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "JPY" to "¥",
        "AED" to "د.إ"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cash & Online Counter",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Box {
                        var menuExpanded by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(36.dp).testTag("cash_counter_settings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Select Currency") },
                                onClick = {
                                    menuExpanded = false
                                    showCurrencyDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                modifier = Modifier.testTag("menu_select_currency")
                            )
                            DropdownMenuItem(
                                text = { Text("Add/Remove Denominations") },
                                onClick = {
                                    menuExpanded = false
                                    showDenomManager = true
                                },
                                leadingIcon = { Icon(Icons.Default.List, contentDescription = null) },
                                modifier = Modifier.testTag("menu_manage_denominations")
                            )
                            DropdownMenuItem(
                                text = { Text("Add/Remove Online Wallets") },
                                onClick = {
                                    menuExpanded = false
                                    showOnlineWalletManager = true
                                },
                                leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                                modifier = Modifier.testTag("menu_manage_online_wallets")
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Show Online")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Checkbox(
                                            checked = showOnline,
                                            onCheckedChange = null,
                                            modifier = Modifier.testTag("menu_show_online_checkbox")
                                        )
                                    }
                                },
                                onClick = {
                                    val newVal = !showOnline
                                    showOnline = newVal
                                    sharedPrefs.edit().putBoolean("show_online", newVal).apply()
                                    menuExpanded = false
                                },
                                modifier = Modifier.testTag("menu_show_online")
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "GRAND TOTAL AMOUNT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$selectedCurrencySymbol${formatAmount(grandTotal)}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("cash_grand_total_text")
                            )
                        }
                    }
                }
            }

            items(denominations) { denom ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(90.dp)
                        ) {
                            Text(
                                text = "$selectedCurrencySymbol$denom",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "X",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Decrement Button
                            OutlinedButton(
                                onClick = {
                                    val current = countTextMap[denom]?.toIntOrNull() ?: 0
                                    if (current > 0) {
                                        countTextMap[denom] = (current - 1).toString()
                                    } else {
                                        countTextMap[denom] = ""
                                    }
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(32.dp).testTag("decrement_$denom"),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                            }

                            // Editable Input
                            BasicTextField(
                                value = countTextMap[denom] ?: "",
                                onValueChange = { newVal ->
                                    if (newVal.isEmpty() || newVal.all { it.isDigit() }) {
                                        countTextMap[denom] = newVal
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(32.dp)
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .wrapContentHeight(Alignment.CenterVertically)
                                    .testTag("input_$denom")
                            )

                            // Increment Button
                            OutlinedButton(
                                onClick = {
                                    val current = countTextMap[denom]?.toIntOrNull() ?: 0
                                    countTextMap[denom] = (current + 1).toString()
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(32.dp).testTag("increment_$denom"),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Text(
                            text = "=",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )

                        val sub = subtotals[denom] ?: 0
                        Text(
                            text = if (sub > 0) "$selectedCurrencySymbol${formatAmount(sub)}" else "—",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (sub > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.width(100.dp).testTag("subtotal_$denom"),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            if (showOnline) {
                item {
                    Text(
                        text = "Online Balances",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (enabledOnlineWallets.isEmpty()) {
                                Text(
                                    text = "No online wallets enabled.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                enabledOnlineWallets.forEach { wallet ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = wallet,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        OutlinedTextField(
                                            value = onlineWalletTextMap[wallet] ?: "",
                                            onValueChange = { newVal ->
                                                if (newVal.isEmpty() || newVal.all { it.isDigit() || it == '.' }) {
                                                    onlineWalletTextMap[wallet] = newVal
                                                }
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                textAlign = TextAlign.End,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            placeholder = {
                                                Text(
                                                    text = "0.00",
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textAlign = TextAlign.End
                                                )
                                            },
                                            prefix = {
                                                Text(
                                                    text = selectedCurrencySymbol,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            modifier = Modifier.width(160.dp).testTag("online_${wallet.lowercase().replace(" ", "_")}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Subtotal / Note counts card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total Note Count", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$totalNotes Notes", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Cash Value Sum", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$selectedCurrencySymbol${formatAmount(cashGrandTotal)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        
                        if (showOnline && onlineTotal > 0.0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Online Balances", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$selectedCurrencySymbol${formatAmount(onlineTotal)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("GRAND TOTAL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("$selectedCurrencySymbol${formatAmount(grandTotal)}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("cash_grand_total_summary"))
                        }
                    }
                }
            }

            // Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showClearConfirmation) {
                        AlertDialog(
                            onDismissRequest = { showClearConfirmation = false },
                            title = { Text("Clear Counter Data?") },
                            text = { Text("Are you sure you want to clear all denomination counts and wallet balances? This cannot be undone.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showClearConfirmation = false
                                        denominations.forEach { countTextMap[it] = "" }
                                        enabledOnlineWallets.forEach { onlineWalletTextMap[it] = "" }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Clear")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showClearConfirmation = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    Button(
                        onClick = {
                            showClearConfirmation = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.weight(1f).height(46.dp).testTag("cash_counter_clear_btn"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            if (grandTotal > 0.0) {
                                val sb = StringBuilder()
                                sb.append("💵 Cash & Online Counter Breakdown 💵\n\n")
                                if (cashGrandTotal > 0) {
                                    sb.append("--- Cash Denominations ---\n")
                                    denominations.forEach { denom ->
                                        val qty = counts[denom] ?: 0
                                        if (qty > 0) {
                                            sb.append("$selectedCurrencySymbol$denom x $qty = $selectedCurrencySymbol${formatAmount(subtotals[denom] ?: 0)}\n")
                                        }
                                    }
                                    sb.append("Total Notes/Coins: $totalNotes\n")
                                    sb.append("Cash Total: $selectedCurrencySymbol${formatAmount(cashGrandTotal)}\n\n")
                                }
                                
                                if (showOnline && onlineTotal > 0.0) {
                                    sb.append("--- Online Balances ---\n")
                                    enabledOnlineWallets.forEach { wallet ->
                                        val amt = onlineWalletTextMap[wallet]?.toDoubleOrNull() ?: 0.0
                                        if (amt > 0.0) {
                                            sb.append("$wallet: $selectedCurrencySymbol${formatAmount(amt)}\n")
                                        }
                                    }
                                    sb.append("Online Total: $selectedCurrencySymbol${formatAmount(onlineTotal)}\n\n")
                                }
                                
                                sb.append("-----------------------------\n")
                                sb.append("GRAND TOTAL: $selectedCurrencySymbol${formatAmount(grandTotal)}")

                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, sb.toString())
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Cash Counter Breakdown"))
                            } else {
                                Toast.makeText(context, "Enter some counts or online balances first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f).height(46.dp).testTag("cash_counter_share_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Share", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Sub-screen overlay for Denomination Manager (Slide-in from right)
        AnimatedVisibility(
            visible = showDenomManager,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().testTag("denom_manager_surface"),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showDenomManager = false },
                            modifier = Modifier.testTag("denom_manager_back_btn")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Manage Denominations",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Add Button
                    Button(
                        onClick = { showAddCustomDenomDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("add_custom_denom_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Custom Denomination", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Denominations List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(allDenominations) { denom ->
                            val isEnabled = enabledDenominationsSet.contains(denom)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isEnabled) 
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) 
                                    else 
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isEnabled) 
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) 
                                    else 
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("denom_item_$denom")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$selectedCurrencySymbol$denom",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (!defaultDenominations.contains(denom)) {
                                            IconButton(
                                                onClick = {
                                                    val newAll = allDenominations - denom
                                                    val newEnabled = enabledDenominationsSet - denom
                                                    
                                                    allDenominationsStr = newAll.sortedDescending().joinToString(",")
                                                    enabledDenominationsStr = newEnabled.sortedDescending().joinToString(",")
                                                    
                                                    sharedPrefs.edit()
                                                        .putString("all_denominations", allDenominationsStr)
                                                        .putString("enabled_denominations", enabledDenominationsStr)
                                                        .apply()
                                                    
                                                    Toast.makeText(context, "Removed denomination $denom", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.testTag("delete_denom_$denom")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Custom Denomination",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                        
                                        Switch(
                                            checked = isEnabled,
                                            onCheckedChange = { checked ->
                                                val newEnabled = if (checked) {
                                                    enabledDenominationsSet + denom
                                                } else {
                                                    enabledDenominationsSet - denom
                                                }
                                                enabledDenominationsStr = newEnabled.sortedDescending().joinToString(",")
                                                sharedPrefs.edit().putString("enabled_denominations", enabledDenominationsStr).apply()
                                            },
                                            modifier = Modifier.testTag("switch_denom_$denom")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Sub-screen overlay for Online Wallet Manager (Slide-in from right)
        AnimatedVisibility(
            visible = showOnlineWalletManager,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().testTag("online_wallet_manager_surface"),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showOnlineWalletManager = false },
                            modifier = Modifier.testTag("online_wallet_manager_back_btn")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Manage Online Wallets",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Add Button
                    Button(
                        onClick = { showAddCustomOnlineWalletDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("add_custom_online_wallet_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Custom Online Wallet", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Wallets List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(allOnlineWallets) { wallet ->
                            val isEnabled = enabledOnlineWalletsSet.contains(wallet)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isEnabled) 
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) 
                                    else 
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isEnabled) 
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) 
                                    else 
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("online_wallet_item_$wallet")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = wallet,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (!defaultOnlineWallets.contains(wallet)) {
                                            IconButton(
                                                onClick = {
                                                    val newAll = allOnlineWallets - wallet
                                                    val newEnabled = enabledOnlineWalletsSet - wallet
                                                    
                                                    allOnlineWalletsStr = newAll.joinToString(",")
                                                    enabledOnlineWalletsStr = newEnabled.joinToString(",")
                                                    
                                                    sharedPrefs.edit()
                                                        .putString("all_online_wallets", allOnlineWalletsStr)
                                                        .putString("enabled_online_wallets", enabledOnlineWalletsStr)
                                                        .apply()
                                                    
                                                    Toast.makeText(context, "Removed wallet $wallet", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.testTag("delete_online_wallet_$wallet")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Custom Wallet",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                        
                                        Switch(
                                            checked = isEnabled,
                                            onCheckedChange = { checked ->
                                                val newEnabled = if (checked) {
                                                    enabledOnlineWalletsSet + wallet
                                                } else {
                                                    enabledOnlineWalletsSet - wallet
                                                }
                                                enabledOnlineWalletsStr = newEnabled.joinToString(",")
                                                sharedPrefs.edit().putString("enabled_online_wallets", enabledOnlineWalletsStr).apply()
                                            },
                                            modifier = Modifier.testTag("switch_online_wallet_$wallet")
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

    // Dialogs
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    currencyOptions.forEach { (code, symbol) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedCurrencyCode = code
                                    selectedCurrencySymbol = symbol
                                    sharedPrefs.edit()
                                        .putString("selected_currency_code", code)
                                        .putString("selected_currency_symbol", symbol)
                                        .apply()
                                    showCurrencyDialog = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("currency_option_$code"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$code ($symbol)",
                                fontWeight = if (selectedCurrencyCode == code) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCurrencyCode == code) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (selectedCurrencyCode == code) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showCurrencyDialog = false },
                    modifier = Modifier.testTag("currency_dialog_cancel")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddCustomDenomDialog) {
        var newDenomText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCustomDenomDialog = false },
            title = { Text("Add Custom Denomination", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newDenomText,
                    onValueChange = { newVal ->
                        if (newVal.isEmpty() || newVal.all { it.isDigit() }) {
                            newDenomText = newVal
                        }
                    },
                    label = { Text("Value") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_denom_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val value = newDenomText.toIntOrNull()
                        if (value != null && value > 0) {
                            if (!allDenominations.contains(value)) {
                                val newAll = (allDenominations + value).sortedDescending()
                                val newEnabled = (enabledDenominationsSet + value).sortedDescending()
                                
                                allDenominationsStr = newAll.joinToString(",")
                                enabledDenominationsStr = newEnabled.joinToString(",")
                                
                                sharedPrefs.edit()
                                    .putString("all_denominations", allDenominationsStr)
                                    .putString("enabled_denominations", enabledDenominationsStr)
                                    .apply()
                                
                                Toast.makeText(context, "Denomination $selectedCurrencySymbol$value added", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Denomination already exists", Toast.LENGTH_SHORT).show()
                            }
                            showAddCustomDenomDialog = false
                        } else {
                            Toast.makeText(context, "Enter a valid value", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("custom_denom_add_confirm")
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddCustomDenomDialog = false },
                    modifier = Modifier.testTag("custom_denom_add_cancel")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddCustomOnlineWalletDialog) {
        var newWalletText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCustomOnlineWalletDialog = false },
            title = { Text("Add Custom Online Wallet", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newWalletText,
                    onValueChange = { newWalletText = it },
                    label = { Text("Wallet Name (e.g. PhonePe)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_online_wallet_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newWalletText.trim()
                        if (name.isNotEmpty()) {
                            if (!allOnlineWallets.contains(name)) {
                                val newAll = allOnlineWallets + name
                                val newEnabled = enabledOnlineWalletsSet + name
                                
                                allOnlineWalletsStr = newAll.joinToString(",")
                                enabledOnlineWalletsStr = newEnabled.joinToString(",")
                                
                                sharedPrefs.edit()
                                    .putString("all_online_wallets", allOnlineWalletsStr)
                                    .putString("enabled_online_wallets", enabledOnlineWalletsStr)
                                    .apply()
                                
                                Toast.makeText(context, "Wallet $name added", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Wallet already exists", Toast.LENGTH_SHORT).show()
                            }
                            showAddCustomOnlineWalletDialog = false
                        } else {
                            Toast.makeText(context, "Enter a valid name", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("custom_online_wallet_add_confirm")
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddCustomOnlineWalletDialog = false },
                    modifier = Modifier.testTag("custom_online_wallet_add_cancel")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

