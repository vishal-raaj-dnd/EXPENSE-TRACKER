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

    var isGlobal by remember { mutableStateOf(true) }
    var selectedCategoryName by remember { mutableStateOf("") }
    var limitAmountStr by remember { mutableStateOf("") }
    
    // Automatically select first category if not global
    LaunchedEffect(isGlobal, categories) {
        if (!isGlobal && categories.isNotEmpty() && selectedCategoryName.isEmpty()) {
            selectedCategoryName = categories.first().name
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Budget Manager", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
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
            // Section Title: Add Budget
            item {
                Text(
                    text = "Set Monthly Spending Limit",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Input Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Global vs Category selector
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { isGlobal = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isGlobal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("select_global_budget")
                            ) {
                                Text("Global Budget", color = if (isGlobal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { isGlobal = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isGlobal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("select_category_budget")
                            ) {
                                Text("Per Category", color = if (!isGlobal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Category Dropdown/Selector (when Category is chosen)
                        if (!isGlobal) {
                            var expanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth().testTag("category_dropdown_trigger")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (selectedCategoryName.isEmpty()) "Select Category" else "Category: $selectedCategoryName",
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat.name) },
                                            onClick = {
                                                selectedCategoryName = cat.name
                                                expanded = false
                                            },
                                            modifier = Modifier.testTag("category_menu_item_${cat.name}")
                                        )
                                    }
                                }
                            }
                        }

                        // Limit Amount Input
                        OutlinedTextField(
                            value = limitAmountStr,
                            onValueChange = { limitAmountStr = it },
                            label = { Text("Monthly Limit Amount (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("budget_amount_input"),
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
                                    val cat = if (isGlobal) null else selectedCategoryName
                                    // Current monthYear is "05/2026"
                                    viewModel.insertBudget(
                                        isGlobal = isGlobal,
                                        categoryName = cat,
                                        limitAmount = amt,
                                        monthYear = "05/2026"
                                    )
                                    limitAmountStr = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("save_budget_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Set Spending Limit", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // Section: Current Budgets list
            item {
                Text(
                    text = "Active Spending Limits (May 2026)",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (budgets.isEmpty()) {
                item {
                    Text(
                        text = "No limits set yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                items(budgets) { budget ->
                    val label = if (budget.isGlobal) "Global Limit" else budget.categoryName ?: "Other"
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("active_budget_item_${label}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (budget.isGlobal) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        imageVector = if (budget.isGlobal) Icons.Default.Public else Icons.Default.Category,
                                        contentDescription = "Budget Icon",
                                        tint = if (budget.isGlobal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "Limit: ₹${String.format(Locale.US, "%.2f", budget.limitAmount)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteBudget(budget) }, modifier = Modifier.testTag("delete_budget_${label}")) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Budget", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionManagerScreen(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit
) {
    val subscriptions by viewModel.allSubscriptions.collectAsStateWithLifecycle()
    val spaces by viewModel.allSpaces.collectAsStateWithLifecycle()
    val wallets by viewModel.allWallets.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedSpaceId by remember { mutableStateOf<Long?>(null) }
    var selectedWalletId by remember { mutableStateOf<Long?>(null) }
    var selectedCategory by remember { mutableStateOf("Bills") }
    var selectedPayerId by remember { mutableStateOf<Long?>(null) }
    var intervalType by remember { mutableStateOf("Monthly") }

    // Dropdown states
    var spaceExpanded by remember { mutableStateOf(false) }
    var walletExpanded by remember { mutableStateOf(false) }
    var catExpanded by remember { mutableStateOf(false) }
    var payerExpanded by remember { mutableStateOf(false) }
    var intervalExpanded by remember { mutableStateOf(false) }

    // When lists load, preselect defaults
    LaunchedEffect(spaces, wallets, categories) {
        if (selectedSpaceId == null && spaces.isNotEmpty()) {
            selectedSpaceId = spaces.first().id
        }
        if (wallets.isNotEmpty()) {
            if (selectedWalletId == null || !wallets.any { it.id == selectedWalletId }) {
                selectedWalletId = wallets.first().id
            }
        } else {
            selectedWalletId = null
        }
        if (selectedCategory.isEmpty() && categories.isNotEmpty()) {
            selectedCategory = categories.first().name
        }
    }

    val activeMembers = allUsers

    LaunchedEffect(activeMembers) {
        if (selectedPayerId == null && activeMembers.isNotEmpty()) {
            selectedPayerId = activeMembers.first().id
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Subscription Manager", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("subscription_back_button")) {
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
            // Section: Add Recurring Transaction / Subscription
            item {
                Text(
                    text = "Add Recurring Expense",
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
                        
                        // Name Input
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Service/Bill Name (e.g., Netflix, Rent)") },
                            modifier = Modifier.fillMaxWidth().testTag("sub_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        )

                        // Amount Input
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = { Text("Billing Amount (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("sub_amount_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        )

                        // Space Selector Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val currentSpaceName = spaces.find { it.id == selectedSpaceId }?.name ?: "Select Split Group/Space"
                            OutlinedButton(
                                onClick = { spaceExpanded = true },
                                modifier = Modifier.fillMaxWidth().testTag("sub_space_trigger")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Post to Space: $currentSpaceName", color = MaterialTheme.colorScheme.onSurface)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            DropdownMenu(
                                expanded = spaceExpanded,
                                onDismissRequest = { spaceExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                spaces.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.name) },
                                        onClick = {
                                            selectedSpaceId = s.id
                                            spaceExpanded = false
                                        },
                                        modifier = Modifier.testTag("sub_space_item_${s.name}")
                                    )
                                }
                            }
                        }

                        // Wallet Selector Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val currentWalletName = wallets.find { it.id == selectedWalletId }?.name ?: "Select Wallet"
                            OutlinedButton(
                                onClick = { walletExpanded = true },
                                modifier = Modifier.fillMaxWidth().testTag("sub_wallet_trigger")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Fund from Wallet: $currentWalletName", color = MaterialTheme.colorScheme.onSurface)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            DropdownMenu(
                                expanded = walletExpanded,
                                onDismissRequest = { walletExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                wallets.forEach { w ->
                                    DropdownMenuItem(
                                        text = { Text(w.name) },
                                        onClick = {
                                            selectedWalletId = w.id
                                            walletExpanded = false
                                        },
                                        modifier = Modifier.testTag("sub_wallet_item_${w.name}")
                                    )
                                }
                            }
                        }

                        // Paid By Selector Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val currentPayerName = activeMembers.find { it.id == selectedPayerId }?.name ?: "Select Payer"
                            OutlinedButton(
                                onClick = { payerExpanded = true },
                                modifier = Modifier.fillMaxWidth().testTag("sub_payer_trigger")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Paid By: $currentPayerName", color = MaterialTheme.colorScheme.onSurface)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            DropdownMenu(
                                expanded = payerExpanded,
                                onDismissRequest = { payerExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                activeMembers.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m.name) },
                                        onClick = {
                                            selectedPayerId = m.id
                                            payerExpanded = false
                                        },
                                        modifier = Modifier.testTag("sub_payer_item_${m.name}")
                                    )
                                }
                            }
                        }

                        // Category Selector Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { catExpanded = true },
                                modifier = Modifier.fillMaxWidth().testTag("sub_category_trigger")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Category: $selectedCategory", color = MaterialTheme.colorScheme.onSurface)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            DropdownMenu(
                                expanded = catExpanded,
                                onDismissRequest = { catExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            selectedCategory = cat.name
                                            catExpanded = false
                                        },
                                        modifier = Modifier.testTag("sub_category_item_${cat.name}")
                                    )
                                }
                            }
                        }

                        // Interval Selector Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { intervalExpanded = true },
                                modifier = Modifier.fillMaxWidth().testTag("sub_interval_trigger")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Interval Frequency: $intervalType", color = MaterialTheme.colorScheme.onSurface)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            DropdownMenu(
                                expanded = intervalExpanded,
                                onDismissRequest = { intervalExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                listOf("Daily", "Weekly", "Monthly").forEach { freq ->
                                    DropdownMenuItem(
                                        text = { Text(freq) },
                                        onClick = {
                                            intervalType = freq
                                            intervalExpanded = false
                                        },
                                        modifier = Modifier.testTag("sub_interval_item_$freq")
                                    )
                                }
                            }
                        }

                        // Add Button
                        Button(
                            onClick = {
                                val amt = amountStr.toDoubleOrNull()
                                val spaceId = selectedSpaceId
                                val walletId = selectedWalletId ?: 1L
                                val payerId = selectedPayerId ?: 1L
                                if (!name.isEmpty() && amt != null && amt > 0.0 && spaceId != null) {
                                    viewModel.insertSubscription(
                                        spaceId = spaceId,
                                        paidById = payerId,
                                        name = name,
                                        amount = amt,
                                        category = selectedCategory,
                                        walletId = walletId,
                                        intervalType = intervalType,
                                        nextDueDate = System.currentTimeMillis() - 1000L
                                    )
                                    name = ""
                                    amountStr = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("save_sub_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Setup Recurring Expense", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Section: Current active subscriptions
            item {
                Text(
                    text = "Active Subscriptions",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (subscriptions.isEmpty()) {
                item {
                    Text(
                        text = "No recurring expenses set up.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                items(subscriptions) { sub ->
                    val spaceName = spaces.find { it.id == sub.spaceId }?.name ?: "Loading..."
                    val simpleDate = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(sub.nextDueDate))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("sub_item_${sub.name}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Autorenew,
                                        contentDescription = "Sub icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = sub.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "Amount: ₹${String.format(Locale.US, "%.2f", sub.amount)} • ${sub.intervalType}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    Text(text = "Group: $spaceName", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = "Next Run: $simpleDate", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteSubscription(sub) }, modifier = Modifier.testTag("delete_sub_${sub.name}")) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Sub", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
