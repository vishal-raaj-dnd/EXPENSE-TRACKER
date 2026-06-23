package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Wallet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletManagerScreen(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit
) {
    val wallets by viewModel.allWallets.collectAsStateWithLifecycle(emptyList())

    var walletName by remember { mutableStateOf("") }
    var walletType by remember { mutableStateOf("Cash") }
    var walletBalance by remember { mutableStateOf("") }
    
    // For editing an existing wallet:
    var editingWallet by remember { mutableStateOf<Wallet?>(null) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val walletTypes = listOf("Cash", "Bank", "Credit Card", "Other")

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Wallet Manager", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("wallet_manager_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Form Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (editingWallet == null) "Add Custom Wallet" else "Edit Wallet: ${editingWallet?.name}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        OutlinedTextField(
                            value = walletName,
                            onValueChange = { walletName = it },
                            label = { Text("Wallet Name (e.g., Pocket Cash)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wallet_name_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        // Wallet Type Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            ExposedDropdownMenuBox(
                                expanded = typeDropdownExpanded,
                                onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = walletType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Wallet Type") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                        .testTag("wallet_type_dropdown"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                ExposedDropdownMenu(
                                    expanded = typeDropdownExpanded,
                                    onDismissRequest = { typeDropdownExpanded = false }
                                ) {
                                    walletTypes.forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type) },
                                            onClick = {
                                                walletType = type
                                                typeDropdownExpanded = false
                                            },
                                            modifier = Modifier.testTag("wallet_type_option_$type")
                                        )
                                    }
                                }
                            }
                        }

                        // Wallet Balance
                        OutlinedTextField(
                            value = walletBalance,
                            onValueChange = { walletBalance = it },
                            label = { Text("Starting Balance (e.g., 1500.00)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wallet_balance_input"),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (editingWallet != null) {
                                Button(
                                    onClick = {
                                        editingWallet = null
                                        walletName = ""
                                        walletType = "Cash"
                                        walletBalance = ""
                                        focusManager.clearFocus()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            val context = androidx.compose.ui.platform.LocalContext.current
                            Button(
                                onClick = {
                                    if (walletName.isNotBlank()) {
                                        val bal = walletBalance.toDoubleOrNull()
                                        if (bal == null) {
                                            android.widget.Toast.makeText(context, "Please enter a valid balance amount.", android.widget.Toast.LENGTH_SHORT).show()
                                        } else if (walletType != "Credit Card" && bal < 0.0) {
                                            android.widget.Toast.makeText(context, "${walletType} balance cannot be negative.", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            val currentEditing = editingWallet
                                            if (currentEditing != null) {
                                                viewModel.updateWallet(
                                                    id = currentEditing.id,
                                                    name = walletName,
                                                    type = walletType,
                                                    balance = bal
                                                )
                                            } else {
                                                viewModel.insertWallet(walletName, walletType, bal)
                                            }
                                            editingWallet = null
                                            walletName = ""
                                            walletType = "Cash"
                                            walletBalance = ""
                                            focusManager.clearFocus()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (editingWallet == null) "Add Wallet" else "Save Changes",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Wallet List
            item {
                Text(
                    text = "Configured Funding Wallets",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            items(wallets) { wallet ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = wallet.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Type: ${wallet.type} • Balance: ₹${formatAmount(wallet.balance)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Edit Button (pencil)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .clickable {
                                        editingWallet = wallet
                                        walletName = wallet.name
                                        walletType = wallet.type
                                        walletBalance = String.format(java.util.Locale.US, "%.2f", wallet.balance)
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Wallet",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Delete Button (trash)
                            var showDeleteConfirm by remember { mutableStateOf(false) }
                            if (showDeleteConfirm) {
                                AlertDialog(
                                    onDismissRequest = { showDeleteConfirm = false },
                                    title = { Text("Delete Wallet?") },
                                    text = { Text("Are you sure you want to delete wallet '${wallet.name}'? This action cannot be undone.") },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                viewModel.deleteWallet(wallet)
                                                showDeleteConfirm = false
                                            }
                                        ) {
                                            Text("Delete", color = MaterialTheme.colorScheme.error)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDeleteConfirm = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                                    .clickable {
                                        showDeleteConfirm = true
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Wallet",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
