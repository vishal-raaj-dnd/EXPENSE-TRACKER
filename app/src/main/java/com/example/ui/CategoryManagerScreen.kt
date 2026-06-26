package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerScreen(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit
) {
    val categories by viewModel.allCategories.collectAsStateWithLifecycle(emptyList())

    var categoryName by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf<Long?>(null) }
    var selectedIconName by remember { mutableStateOf("category") }
    var parentDropdownExpanded by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    val focusManager = LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    // Available Icons for custom picker
    val iconOptions = listOf(
        "restaurant" to Icons.Default.Restaurant,
        "shopping_cart" to Icons.Default.ShoppingCart,
        "directions_car" to Icons.Default.DirectionsCar,
        "movie" to Icons.Default.Movie,
        "event" to Icons.Default.Event,
        "spa" to Icons.Default.Spa,
        "school" to Icons.Default.School,
        "payments" to Icons.Default.Payments,
        "flight" to Icons.Default.Flight,
        "home" to Icons.Default.Home,
        "sports_esports" to Icons.Default.SportsEsports,
        "category" to Icons.Default.Category
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Category Manager", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("category_manager_back")) {
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
        if (categoryToDelete != null) {
            AlertDialog(
                onDismissRequest = { categoryToDelete = null },
                title = { Text("Delete Category", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete the category \"${categoryToDelete?.name}\"? All its subcategories will also be deleted. This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            categoryToDelete?.let { viewModel.deleteCategory(it) }
                            categoryToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { categoryToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section: Add New Category Custom Card Form
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
                            text = "Add Custom Category",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        // 1. Category name textfield
                        OutlinedTextField(
                            value = categoryName,
                            onValueChange = { categoryName = it },
                            label = { Text("Category Name (e.g., Groceries)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("category_name_input"),
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

                        // 2. Parent dropdown selector (if nesting, e.g. parent-to-child mapping)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val selectedParentName = if (selectedParentId == null) {
                                "None (Make Top-Level Parent)"
                            } else {
                                categories.find { it.id == selectedParentId }?.name ?: "None"
                            }

                            ExposedDropdownMenuBox(
                                expanded = parentDropdownExpanded,
                                onExpandedChange = { parentDropdownExpanded = !parentDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedParentName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Parent Category (Optional)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentDropdownExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                        .testTag("parent_category_dropdown"),
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
                                    expanded = parentDropdownExpanded,
                                    onDismissRequest = { parentDropdownExpanded = false },
                                    modifier = Modifier.heightIn(max = 240.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("None (Make Top-Level Parent)") },
                                        onClick = {
                                            selectedParentId = null
                                            parentDropdownExpanded = false
                                        }
                                    )
                                    // Only top level categories (with parentId == null) can be a parent
                                    categories.filter { it.parentId == null }.forEach { parentCat ->
                                        DropdownMenuItem(
                                            text = { Text(parentCat.name) },
                                            onClick = {
                                                selectedParentId = parentCat.id
                                                parentDropdownExpanded = false
                                            },
                                            modifier = Modifier.testTag("parent_option_${parentCat.name}")
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Icon Picker Row
                        Text(
                            text = "Assign Material Icon",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(iconOptions) { (key, icon) ->
                                val isSelected = selectedIconName == key
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedIconName = key }
                                        .padding(10.dp)
                                        .testTag("icon_picker_$key")
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = key,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = key.replace("_", " "),
                                        fontSize = 9.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // 4. Save Submit Button
                        Button(
                            onClick = {
                                if (categoryName.isNotBlank()) {
                                    viewModel.insertCategory(
                                        name = categoryName.trim(),
                                        iconName = selectedIconName,
                                        parentId = selectedParentId,
                                        context = context
                                    )
                                    categoryName = ""
                                    selectedParentId = null
                                    selectedIconName = "category"
                                    focusManager.clearFocus()
                                }
                            },
                            enabled = categoryName.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_custom_category"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Text("Save Category", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Section: Explorer Tree Title
            item {
                Text(
                    text = "Nest Hierarchy Explorer",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Categorized List
            if (categories.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No custom categories found.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            } else {
                // Group root parent categories (parentId == null) and render children inline
                val parentCategories = categories.filter { it.parentId == null }
                val uncategorizedChildren = categories.filter { child ->
                    child.parentId != null && !parentCategories.any { parent -> parent.id == child.parentId }
                }

                // Render parent categories followed by their nested child categories
                parentCategories.forEach { parent ->
                    item(key = "parent_${parent.id}") {
                        CategoryNodeItem(
                            category = parent,
                            isChild = false,
                            onDelete = { categoryToDelete = parent }
                        )
                    }

                    val children = categories.filter { it.parentId == parent.id }
                    items(children, key = { "child_${it.id}" }) { child ->
                        CategoryNodeItem(
                            category = child,
                            isChild = true,
                            onDelete = { categoryToDelete = child }
                        )
                    }
                }

                if (uncategorizedChildren.isNotEmpty()) {
                    item {
                        Text(
                            text = "Orphaned nested items",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(uncategorizedChildren) { child ->
                        CategoryNodeItem(
                            category = child,
                            isChild = true,
                            onDelete = { categoryToDelete = child }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CategoryNodeItem(
    category: Category,
    isChild: Boolean,
    onDelete: () -> Unit
) {
    val iconsMap = mapOf(
        "restaurant" to Icons.Default.Restaurant,
        "shopping_cart" to Icons.Default.ShoppingCart,
        "directions_car" to Icons.Default.DirectionsCar,
        "movie" to Icons.Default.Movie,
        "event" to Icons.Default.Event,
        "spa" to Icons.Default.Spa,
        "school" to Icons.Default.School,
        "payments" to Icons.Default.Payments,
        "flight" to Icons.Default.Flight,
        "home" to Icons.Default.Home,
        "sports_esports" to Icons.Default.SportsEsports,
        "category" to Icons.Default.Category
    )
    val displayIcon = iconsMap[category.iconName.lowercase()] ?: Icons.Default.Category

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isChild) 32.dp else 0.dp, top = 2.dp, bottom = 2.dp)
            .background(
                color = if (isChild) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = if (isChild) MaterialTheme.colorScheme.outline.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isChild) {
            Icon(
                imageVector = Icons.Default.SubdirectoryArrowRight,
                contentDescription = "Nested under",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isChild) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = displayIcon,
                contentDescription = category.name,
                tint = if (isChild) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.name,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
            Text(
                text = if (isChild) "Nested Sub-category" else "Top-level Category",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        var catMenuExpanded by remember { mutableStateOf(false) }
        Box {
            IconButton(
                onClick = { catMenuExpanded = true },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("cat_menu_${category.name}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Category Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(
                expanded = catMenuExpanded,
                onDismissRequest = { catMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Delete", color = Color(0xFFE57373)) },
                    onClick = {
                        catMenuExpanded = false
                        onDelete()
                    },
                    modifier = Modifier.testTag("delete_cat_${category.name}")
                )
            }
        }
    }
}
