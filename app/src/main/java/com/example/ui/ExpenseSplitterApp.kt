package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.flowOf
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.net.Uri
import android.content.Context
import android.content.Intent

// --- Navigation Routes ---
object Routes {
    const val SPACES = "spaces"
    const val ADD_EXPENSE = "add_expense"
    const val TOOLS = "tools"
    const val PROFILE = "profile"
    const val CATEGORY_MANAGER = "category_manager"
    const val BUDGET_MANAGER = "budget_manager"
    const val RECURRING_MANAGER = "recurring_manager"
    const val WALLET_MANAGER = "wallet_manager"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseSplitterApp(
    viewModel: ExpenseViewModel,
    isDarkTheme: Boolean = true,
    onThemeToggle: () -> Unit = {},
    fontScale: Float = 1.0f,
    onFontScaleChange: (Float) -> Unit = {}
) {
    val navController = rememberNavController()
    val activeUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.SPACES
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val appInitState by viewModel.appInitState.collectAsStateWithLifecycle()
    val isDatabaseLoaded = appInitState.isLoaded
    val initialUsersLoaded = appInitState.usersList

    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("settle_split_prefs", android.content.Context.MODE_PRIVATE) }
    var onboardingCompleted by remember { mutableStateOf(sharedPref.getBoolean("onboarding_completed", false)) }

    // If database is loaded and has absolutely no users, we must reset onboarding completed status
    LaunchedEffect(isDatabaseLoaded, initialUsersLoaded) {
        if (isDatabaseLoaded && initialUsersLoaded.isEmpty() && onboardingCompleted) {
            sharedPref.edit().putBoolean("onboarding_completed", false).apply()
            onboardingCompleted = false
        }
    }

    AnimatedContent(
        targetState = if (!isDatabaseLoaded) "loading" else if (!onboardingCompleted || initialUsersLoaded.isEmpty()) "onboarding" else "dashboard",
        transitionSpec = {
            fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(400))
        },
        label = "app_root_state_transition",
        modifier = Modifier.fillMaxSize()
    ) { state ->
        when (state) {
            "loading" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            "onboarding" -> {
                OnboardingScreen(
                    onUserCreated = { username ->
                        val names = username.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        viewModel.createMultipleUsers(names) {
                            sharedPref.edit().putBoolean("onboarding_completed", true).apply()
                            onboardingCompleted = true
                        }
                    }
                )
            }
            "dashboard" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background, // Rich atmospheric depth
                                    MaterialTheme.colorScheme.background  // True deep dark black
                                )
                            )
                        )
                ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = androidx.compose.ui.res.painterResource(
                                    id = if (isDarkTheme) com.example.R.drawable.logo_dark else com.example.R.drawable.logo_light
                                ),
                                contentDescription = "Travel Split Logo",
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                             Text(
                                 text = "Travel Split",
                                 color = MaterialTheme.colorScheme.onSurface,
                                 fontWeight = FontWeight.Bold,
                                 fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                                 fontSize = 20.sp,
                                 letterSpacing = 0.5.sp
                             )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    actions = {
                        IconButton(onClick = onThemeToggle) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        activeUser?.let { user ->
                            Spacer(modifier = Modifier.width(4.dp))
                            if (user.avatarUrl.isNotBlank()) {
                                coil.compose.AsyncImage(
                                    model = user.avatarUrl,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .testTag("top_profile_badge"),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .testTag("top_profile_badge")
                                ) {
                                    Text(
                                        text = user.name.take(1).uppercase(),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                )
            },
            bottomBar = {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), thickness = 1.dp)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        tonalElevation = 0.dp,
                        modifier = Modifier.testTag("bottom_nav")
                    ) {
                    val items = listOf(
                        NavigationItem("Spaces", Routes.SPACES, Icons.Default.Group, "spaces_tab"),
                        NavigationItem("Add", Routes.ADD_EXPENSE, Icons.Default.Add, "add_tab"),
                        NavigationItem("Tools", Routes.TOOLS, Icons.Default.Calculate, "tools_tab"),
                        NavigationItem("Profile", Routes.PROFILE, Icons.Default.Person, "profile_tab")
                    )

                    items.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    if (item.route != Routes.TOOLS && item.route != Routes.ADD_EXPENSE) {
                                        viewModel.selectSpace(null)
                                    }
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                } else if (item.route == Routes.SPACES) {
                                    viewModel.selectSpace(null)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(if (selected) 24.dp else 22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.SPACES,
                modifier = Modifier.padding(innerPadding)
            ) {
            composable(Routes.SPACES) {
                SpacesScreen(
                    viewModel = viewModel,
                    onNavigateToAddExpense = {
                        navController.navigate(Routes.ADD_EXPENSE) {
                            popUpTo(Routes.SPACES) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Routes.ADD_EXPENSE) {
                AddExpenseScreen(viewModel, onSaved = {
                    navController.popBackStack()
                })
            }
            composable(Routes.TOOLS) {
                ToolsScreen(
                    viewModel = viewModel,
                    onNavigateToAddExpense = {
                        navController.navigate(Routes.ADD_EXPENSE) {
                            popUpTo(Routes.TOOLS) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    viewModel = viewModel,
                    fontScale = fontScale,
                    onFontScaleChange = onFontScaleChange,
                    onNavigateToCategoryManager = { navController.navigate(Routes.CATEGORY_MANAGER) },
                    onNavigateToBudgetManager = { navController.navigate(Routes.BUDGET_MANAGER) },
                    onNavigateToSubscriptionManager = { navController.navigate(Routes.RECURRING_MANAGER) },
                    onNavigateToWalletManager = { navController.navigate(Routes.WALLET_MANAGER) }
                )
            }
            composable(Routes.CATEGORY_MANAGER) {
                CategoryManagerScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.BUDGET_MANAGER) {
                BudgetManagerScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.RECURRING_MANAGER) {
                SubscriptionManagerScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.WALLET_MANAGER) {
                WalletManagerScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }


    } // closes Box
    } // closes dashboard state block
    } // closes when(state)
    } // closes AnimatedContent
}

data class NavigationItem(
    val label: String,
    val route: String,
    val icon: ImageVector,
    val testTag: String
)

// --- HOME SCREEN REMOVED ---
@Composable
fun SpaceCard(
    space: Space,
    totalSpent: Double,
    members: List<User>,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("home_space_card_${space.id}")
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = space.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = space.description.ifEmpty { "Split group ledger" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open Splitting Space",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Overlapping avatars
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    members.take(3).forEachIndexed { index, user ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(26.dp)
                                .offset(x = if (index > 0) (-8 * index).dp else 0.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Text(
                                text = user.name.take(1).uppercase(),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                    if (members.size > 3) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(26.dp)
                                .offset(x = (-8 * 3).dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Text(
                                text = "+${members.size - 3}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                // Spent Information
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TOTAL SPENT",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "₹${String.format(java.util.Locale.US, "%.2f", totalSpent)}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}


// --- SPACES SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpacesScreen(viewModel: ExpenseViewModel, onNavigateToAddExpense: () -> Unit) {
    val spaces by viewModel.allSpaces.collectAsStateWithLifecycle()
    val activeSpaceId by viewModel.activeSpaceId.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    var showCreateSpaceDialog by remember { mutableStateOf(false) }
    var showCameraScanner by remember { mutableStateOf(false) }
    var spaceToShare by remember { mutableStateOf<Space?>(null) }
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            showCameraScanner = true
        } else {
            Toast.makeText(context, "Camera permission is required to scan QR codes.", Toast.LENGTH_LONG).show()
        }
    }

    if (showCameraScanner) {
        var isProcessing by remember { mutableStateOf(false) }
        CameraScannerView(
            onQRCodeScanned = { code ->
                if (!isProcessing) {
                    isProcessing = true
                    try {
                        val decompressed = decompressString(code)
                        val payload = sharingJson.decodeFromString(SharedSpacePayload.serializer(), decompressed)
                        viewModel.importSharedSpace(payload) { success ->
                            isProcessing = false
                            if (success) {
                                Toast.makeText(context, "Space '${payload.spaceName}' imported successfully!", Toast.LENGTH_LONG).show()
                                showCameraScanner = false
                            } else {
                                Toast.makeText(context, "Failed to import space.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        isProcessing = false
                        Toast.makeText(context, "Invalid QR Code or corrupted data.", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onClose = {
                showCameraScanner = false
            }
        )
    } else {
        if (spaceToShare != null) {
            ShareSpaceDialog(
                spaceId = spaceToShare!!.id,
                viewModel = viewModel,
                onDismiss = { spaceToShare = null }
            )
        }
        if (activeSpaceId != null) {
            // Render Detail View
            SpaceDetailScreen(viewModel, onNavigateToAddExpense)
        } else {
            // Render List of Spaces
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showCreateSpaceDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("create_space_fab")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Create Group")
                    }
                },
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = innerPadding.calculateBottomPadding() + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Expense Split Spaces",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            IconButton(
                                onClick = {
                                    if (hasCameraPermission) {
                                        showCameraScanner = true
                                    } else {
                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                                    .testTag("scan_to_join_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan to Join",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (spaces.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 64.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = "",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No spaces setup yet.",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Create a space (e.g. flatmates, trip) using the + button to start sharing bills!",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        items(spaces) { space ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.surface,
                                                MaterialTheme.colorScheme.surface
                                            )
                                        )
                                    )
                                    .clickable { viewModel.selectSpace(space.id) }
                                    .testTag("space_item_${space.id}")
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = space.name,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = { spaceToShare = space },
                                                modifier = Modifier.testTag("share_space_item_${space.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "Share Space / QR",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteSpace(space) },
                                                modifier = Modifier.testTag("delete_space_${space.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Space",
                                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = space.description,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.People,
                                            contentDescription = "Members",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "View details and settlement plans",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
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

    // dialog to create a space
    if (showCreateSpaceDialog) {
        var spaceName by remember { mutableStateOf("") }
        var spaceDesc by remember { mutableStateOf("") }
        var memberNamesInput by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showCreateSpaceDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Create New Space",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = spaceName,
                        onValueChange = { spaceName = it },
                        label = { Text("Space Name (e.g., Flatmates)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_space_name_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = spaceDesc,
                        onValueChange = { spaceDesc = it },
                        label = { Text("Description") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = memberNamesInput,
                        onValueChange = { memberNamesInput = it },
                        label = { Text("Add Group Members (comma-separated, optional)") },
                        placeholder = { Text("Rohan, Amit, Sunil") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_space_members_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCreateSpaceDialog = false }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (spaceName.isNotBlank()) {
                                    val names = memberNamesInput.split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                    viewModel.createSpaceWithNewMembers(
                                        spaceName = spaceName.trim(),
                                        spaceDesc = spaceDesc.trim(),
                                        memberNames = names
                                    )
                                    showCreateSpaceDialog = false
                                }
                            },
                            enabled = spaceName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("submit_space_button")
                        ) {
                            Text("Create", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- SPACE DETAIL SCREEN ---
@Composable
fun SpaceDetailScreen(viewModel: ExpenseViewModel, onNavigateToAddExpense: () -> Unit) {
    val space by viewModel.activeSpace.collectAsStateWithLifecycle()
    val members by viewModel.activeSpaceMembers.collectAsStateWithLifecycle()
    val expenses by viewModel.activeSpaceExpenses.collectAsStateWithLifecycle()
    val balances by viewModel.activeSpaceBalances.collectAsStateWithLifecycle()
    val transactions by viewModel.activeSpaceSettlementTransactions.collectAsStateWithLifecycle()

    androidx.activity.compose.BackHandler(enabled = space != null) {
        viewModel.selectSpace(null)
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var addMemberNameText by remember { mutableStateOf("") }

    if (showShareDialog && space != null) {
        ShareSpaceDialog(
            spaceId = space!!.id,
            viewModel = viewModel,
            onDismiss = { showShareDialog = false }
        )
    }

    if (showAddMemberDialog && space != null) {
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Add Space Member", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = addMemberNameText,
                    onValueChange = { addMemberNameText = it },
                    singleLine = true,
                    label = { Text("Member Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.testTag("add_member_name_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (addMemberNameText.isNotBlank()) {
                            viewModel.createAndAddMemberToSpace(
                                spaceId = space!!.id,
                                name = addMemberNameText.trim(),
                                email = "${addMemberNameText.trim().lowercase().replace(" ", "")}@example.com"
                            )
                            addMemberNameText = ""
                            showAddMemberDialog = false
                        }
                    },
                    enabled = addMemberNameText.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemberDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Back Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = { viewModel.selectSpace(null) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                            .testTag("back_to_spaces")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = space?.name ?: "Loading...",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                letterSpacing = 0.5.sp
                            )
                            if (space != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { showShareDialog = true },
                                    modifier = Modifier.size(28.dp).testTag("share_space_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share Space",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { showAddMemberDialog = true },
                                    modifier = Modifier.size(28.dp).testTag("add_member_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "Add Member",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = space?.description ?: "",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // High-end Total Spent indicator
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TOTAL SPENT",
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    val totalSpentValue = expenses.sumOf { it.amount }
                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", totalSpentValue)}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Expenses", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Balances & Settlement", fontWeight = FontWeight.Bold) }
                )
            }

            when (selectedTabIndex) {
                0 -> SpaceExpensesTab(expenses, members, viewModel, onNavigateToAddExpense)
                1 -> SpaceBalancesTab(balances, transactions, viewModel)
            }
        }
    }
}

data class ChartPoint(
    val xLabel: String,
    val date: Long,
    val value: Float
)

@Composable
fun ExpensesOverTimeChart(
    expenses: List<Expense>,
    modifier: Modifier = Modifier
) {
    if (expenses.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Chart updates as expenses are added.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    // Sort chronologically
    val sortedExpenses = remember(expenses) {
        expenses.sortedBy { it.date }
    }

    // Calculate cumulative sums
    val chartPoints = remember(sortedExpenses) {
        var cumul = 0.0
        val pts = ArrayList<ChartPoint>()
        if (sortedExpenses.isNotEmpty()) {
            val firstDate = sortedExpenses.first().date
            pts.add(ChartPoint("Start", firstDate - 24 * 3600 * 1000, 0f))
        }
        for (exp in sortedExpenses) {
            cumul += exp.amount
            pts.add(ChartPoint(
                xLabel = SimpleDateFormat("MMM d", Locale.US).format(Date(exp.date)),
                date = exp.date,
                value = cumul.toFloat()
            ))
        }
        pts
    }

    val minDate = chartPoints.first().date
    val maxDate = chartPoints.last().date
    val rangeDate = if (maxDate == minDate) 1L else maxDate - minDate

    val maxValue = chartPoints.maxOf { it.value }
    val yMax = if (maxValue == 0f) 100f else maxValue * 1.15f // 15% top padding

    // Hold internal interaction states
    var touchX by remember { mutableStateOf<Float?>(null) }
    var activeIdx by remember { mutableStateOf<Int?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CUMULATIVE EXPENSES OVER TIME",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    val currentVal = if (activeIdx != null && activeIdx!! < chartPoints.size) {
                        chartPoints[activeIdx!!].value
                    } else {
                        maxValue
                    }
                    val currentDate = if (activeIdx != null && activeIdx!! < chartPoints.size) {
                        SimpleDateFormat("MMMM d • h:mm a", Locale.US).format(Date(chartPoints[activeIdx!!].date))
                    } else {
                        "Interactive Chart"
                    }
                    
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", currentVal)}",
                            color = MaterialTheme.colorScheme.primary, // Neon Green
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentDate,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .pointerInput(chartPoints) {
                        val w = size.width
                        val minD = chartPoints.firstOrNull()?.date ?: 0L
                        val maxD = chartPoints.lastOrNull()?.date ?: 0L
                        val rangeD = if (maxD == minD) 1L else maxD - minD
                        val computedPointsX = chartPoints.map { pt ->
                            val fx = (pt.date - minD).toFloat() / rangeD.toFloat()
                            fx * w
                        }
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull()
                                if (change != null) {
                                    if (change.pressed) {
                                        val curX = change.position.x
                                        touchX = curX
                                        if (computedPointsX.isNotEmpty()) {
                                            activeIdx = computedPointsX.indices.minByOrNull {
                                                kotlin.math.abs(computedPointsX[it] - curX)
                                            }
                                        }
                                        change.consume()
                                    } else {
                                        touchX = null
                                        activeIdx = null
                                    }
                                }
                            }
                        }
                    }
            ) {
                val chartGridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                val chartPrimaryColor = MaterialTheme.colorScheme.primary
                val chartPrimaryFill = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                val chartPrimaryFillEnd = MaterialTheme.colorScheme.primary.copy(alpha = 0.01f)
                val chartCrosshairColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                val chartHaloColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                val chartDotColor = MaterialTheme.colorScheme.onSurface
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    val computedPoints = chartPoints.map { pt ->
                        val fx = (pt.date - minDate).toFloat() / rangeDate.toFloat()
                        val fy = pt.value / yMax
                        Offset(
                            x = fx * w,
                            y = h - (fy * h)
                        )
                    }

                    // Background grid
                    val gridLinePaint = chartGridColor
                    for (scale in listOf(0.25f, 0.5f, 0.75f)) {
                        drawLine(
                            color = gridLinePaint,
                            start = Offset(0f, h * scale),
                            end = Offset(w, h * scale),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    if (computedPoints.isNotEmpty()) {
                        val strokePath = Path()
                        val fillPath = Path()

                        strokePath.moveTo(computedPoints.first().x, computedPoints.first().y)
                        fillPath.moveTo(computedPoints.first().x, computedPoints.first().y)

                        if (computedPoints.size < 3) {
                            for (p in computedPoints) {
                                strokePath.lineTo(p.x, p.y)
                                fillPath.lineTo(p.x, p.y)
                            }
                        } else {
                            for (i in 1 until computedPoints.size) {
                                val p0 = computedPoints[i - 1]
                                val p1 = computedPoints[i]
                                val cx1 = p0.x + (p1.x - p0.x) * 0.45f
                                val cy1 = p0.y
                                val cx2 = p0.x + (p1.x - p0.x) * 0.55f
                                val cy2 = p1.y
                                strokePath.cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
                                fillPath.cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
                            }
                        }

                        fillPath.lineTo(computedPoints.last().x, h)
                        fillPath.lineTo(computedPoints.first().x, h)
                        fillPath.close()

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    chartPrimaryFill, 
                                    chartPrimaryFillEnd, 
                                    Color.Transparent
                                )
                            )
                        )

                        drawPath(
                            path = strokePath,
                            color = chartPrimaryColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )

                        val currentTouchX = touchX
                        val currentActiveIdx = activeIdx
                        if (currentTouchX != null && currentActiveIdx != null && currentActiveIdx < computedPoints.size) {
                            val intersect = computedPoints[currentActiveIdx]

                            drawLine(
                                color = chartCrosshairColor,
                                start = Offset(intersect.x, 0f),
                                end = Offset(intersect.x, h),
                                strokeWidth = 1.dp.toPx()
                            )

                            drawCircle(
                                color = chartHaloColor,
                                radius = 10.dp.toPx(),
                                center = intersect
                            )

                            drawCircle(
                                color = chartPrimaryColor,
                                radius = 5.dp.toPx(),
                                center = intersect
                            )

                            drawCircle(
                                color = chartDotColor,
                                radius = 2.dp.toPx(),
                                center = intersect
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = chartPoints.first().xLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = chartPoints.last().xLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CategoryPieChart(
    expenses: List<Expense>,
    modifier: Modifier = Modifier
) {
    if (expenses.isEmpty()) return

    val categoryTotals = remember(expenses) {
        expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val total = categoryTotals.sumOf { it.second }
    if (total <= 0.0) return

    val pieColors = listOf(
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFFE91E63), // Pink
        Color(0xFF00BCD4), // Teal
        Color(0xFFFFEB3B), // Yellow
        Color(0xFF795548), // Brown
        Color(0xFF9E9E9E)  // Gray
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "EXPENSES BY CATEGORY",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut Canvas
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(130.dp)
                ) {
                    Canvas(modifier = Modifier.size(110.dp)) {
                        var startAngle = -90f
                        categoryTotals.forEachIndexed { index, (_, amount) ->
                            val sweepAngle = (amount / total).toFloat() * 360f
                            drawArc(
                                color = pieColors[index % pieColors.size],
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Butt)
                            )
                            startAngle += sweepAngle
                        }
                    }
                    // Center total label
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${String.format(Locale.US, "%.0f", total)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Legends
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoryTotals.take(5).forEachIndexed { index, (cat, amt) ->
                        val pct = (amt / total) * 100.0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(pieColors[index % pieColors.size])
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                    text = "${String.format(Locale.US, "%.1f", pct)}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseRow(
    expense: Expense,
    members: List<User>,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Expense?") },
            text = { Text("Are you sure you want to delete this expense? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
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

    val payer = members.find { it.id == expense.paidById }
    val formattedDate = remember(expense.date) {
        val sdf = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.US)
        sdf.format(Date(expense.date))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (expense.category == "Settlement") {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .border(
                                1.dp,
                                if (expense.category == "Settlement") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        val icon = when (expense.category) {
                            "Groceries" -> Icons.Default.ShoppingCart
                            "Transport" -> Icons.Default.DirectionsCar
                            "Settlement" -> Icons.Default.Check
                            else -> Icons.Default.Receipt
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = expense.category,
                            tint = if (expense.category == "Settlement") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = expense.description,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$formattedDate • by ${payer?.name ?: "Unknown"}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", expense.amount)}",
                        color = if (expense.category == "Settlement") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), CircleShape)
                                .testTag("expense_menu_${expense.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Expense Options",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (expense.category != "Settlement") {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Option",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onEdit()
                                    },
                                    modifier = Modifier.testTag("edit_expense_${expense.id}")
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color.Red) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Option",
                                        tint = Color.Red,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteConfirm = true
                                },
                                modifier = Modifier.testTag("delete_expense_${expense.id}")
                            )
                        }
                    }
                }
            }

            if (!expense.attachmentUris.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 72.dp, end = 14.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    expense.attachmentUris.forEach { uriStr ->
                        var isExpanded by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .clickable { isExpanded = true }
                        ) {
                            AsyncImage(
                                model = uriStr,
                                contentDescription = "Receipt Attachment",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (isExpanded) {
                            ZoomableImageDialog(
                                uriStr = uriStr,
                                onDismiss = { isExpanded = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpaceExpensesTab(expenses: List<Expense>, members: List<User>, viewModel: ExpenseViewModel, onNavigateToAddExpense: () -> Unit) {
    val context = LocalContext.current
    val wallets by viewModel.allWallets.collectAsStateWithLifecycle(emptyList())
    val activeSpaceState by viewModel.activeSpace.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRANSACTION HISTORY Feed",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${expenses.size} Items",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (expenses.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = "",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No expenses recorded yet",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add expenses in the '+' tab to feed this group's activity stream & chart statistics automatically.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(expenses) { expense ->
                ExpenseRow(
                    expense = expense,
                    members = members,
                    onDelete = { viewModel.deleteExpense(expense.id) },
                    onEdit = {
                        viewModel.startEditingExpense(expense)
                        onNavigateToAddExpense()
                    }
                )
            }
        }
    }
}

@Composable
fun SpaceBalancesTab(
    balances: List<ExpenseViewModel.MemberBalance>,
    transactions: List<ExpenseViewModel.SettlementTransaction>,
    viewModel: ExpenseViewModel
) {
    val spaceId by viewModel.activeSpaceId.collectAsStateWithLifecycle()
    val space by viewModel.activeSpace.collectAsStateWithLifecycle()
    val activeUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val expenses by viewModel.activeSpaceExpenses.collectAsStateWithLifecycle()
    val wallets by viewModel.allWallets.collectAsStateWithLifecycle(emptyList())
    val splits by viewModel.activeSpaceSplits.collectAsStateWithLifecycle()
    var transactionToSettle by remember { mutableStateOf<ExpenseViewModel.SettlementTransaction?>(null) }
    var selectedWalletForSettle by remember { mutableStateOf<Wallet?>(null) }
    var walletDropdownExpanded by remember { mutableStateOf(false) }

    var userToRemove by remember { mutableStateOf<User?>(null) }
    var userToRename by remember { mutableStateOf<User?>(null) }
    var renameNameText by remember { mutableStateOf("") }

    var summaryExpanded by remember { mutableStateOf(true) }
    var settleExpanded by remember { mutableStateOf(true) }
    var membersExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    if (userToRemove != null) {
        AlertDialog(
            onDismissRequest = { userToRemove = null },
            title = { Text("Remove Member?", fontWeight = FontWeight.Bold) },
            text = { Text("Removing ${userToRemove!!.name} will delete expenses paid by them and redistribute their remaining splits. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        userToRemove?.let { u ->
                            viewModel.removeUserFromSpaceAndRecalculate(spaceId ?: 0L, u.id)
                        }
                        userToRemove = null
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (userToRename != null) {
        AlertDialog(
            onDismissRequest = { userToRename = null },
            title = { Text("Rename Member", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter a new name for ${userToRename!!.name}:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = renameNameText,
                        onValueChange = { renameNameText = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.testTag("rename_member_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameNameText.isNotBlank()) {
                            viewModel.updateUserProfile(userToRename!!.id, renameNameText.trim())
                            userToRename = null
                            renameNameText = ""
                        }
                    },
                    enabled = renameNameText.isNotBlank()
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    val activeTx = transactionToSettle
    if (activeTx != null) {
        AlertDialog(
            onDismissRequest = { transactionToSettle = null },
            title = { Text("Confirm Settlement", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Record payment of ₹${formatAmount(activeTx.amount)} from ${activeTx.debtor.name} to ${activeTx.creditor.name}?")
                    
                    if (wallets.isNotEmpty()) {
                        val currentSel = selectedWalletForSettle ?: wallets.first()
                        LaunchedEffect(wallets) {
                            if (selectedWalletForSettle == null) {
                                selectedWalletForSettle = wallets.first()
                            }
                        }
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { walletDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Wallet: ${currentSel.name} (₹${formatAmount(currentSel.balance)})")
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            
                            DropdownMenu(
                                expanded = walletDropdownExpanded,
                                onDismissRequest = { walletDropdownExpanded = false }
                            ) {
                                wallets.forEach { w ->
                                    DropdownMenuItem(
                                        text = { Text("${w.name} (₹${formatAmount(w.balance)})") },
                                        onClick = {
                                            selectedWalletForSettle = w
                                            walletDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No funding wallets configured. Please create a wallet under Profile -> Wallet Manager.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetWallet = selectedWalletForSettle ?: wallets.firstOrNull()
                        val sId = spaceId
                        if (targetWallet != null && sId != null) {
                            viewModel.settleDebt(
                                spaceId = sId,
                                debtorId = activeTx.debtor.id,
                                creditorId = activeTx.creditor.id,
                                amount = activeTx.amount,
                                walletId = targetWallet.id,
                                context = context
                            )
                        }
                        transactionToSettle = null
                    },
                    enabled = wallets.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToSettle = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Relocated Cumulative line graph
        item {
            ExpensesOverTimeChart(expenses = expenses)
        }

        // Relocated Category Pie Chart
        item {
            CategoryPieChart(expenses = expenses)
        }

        // Relocated Auditing & Export Engine
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = "Audit Reports",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Auditing & Export Engine",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Generate and share formatted spreadsheets or professional line-by-line PDF reports instantly.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val activeSpaceState = space
                        val members = balances.map { it.user }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val currentSpaceObj = activeSpaceState
                                    if (currentSpaceObj != null) {
                                        val excelFile = com.example.utils.ExportEngine.exportSpaceToExcel(
                                            context = context,
                                            spaceName = currentSpaceObj.name,
                                            expenses = expenses,
                                            members = members,
                                            wallets = wallets,
                                            balances = balances,
                                            settlements = transactions
                                        )
                                        if (excelFile != null) {
                                            shareFile(context, excelFile, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                        } else {
                                            Toast.makeText(context, "Failed to compile Excel sheet", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("export_space_excel")
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = "", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("XLSX", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val currentSpaceObj = activeSpaceState
                                    if (currentSpaceObj != null) {
                                        val csvFile = com.example.utils.ExportEngine.exportSpaceToCSV(
                                            context = context,
                                            spaceName = currentSpaceObj.name,
                                            expenses = expenses,
                                            members = members,
                                            wallets = wallets
                                        )
                                        if (csvFile != null) {
                                            shareFile(context, csvFile, "text/csv")
                                        } else {
                                            Toast.makeText(context, "Failed to generate CSV", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("export_space_csv")
                            ) {
                                Icon(Icons.Default.Description, contentDescription = "", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CSV", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val currentSpaceObj = activeSpaceState
                                    if (currentSpaceObj != null) {
                                        val pdfFile = com.example.utils.ExportEngine.generateSpaceReportPDF(
                                            context = context,
                                            spaceName = currentSpaceObj.name,
                                            expenses = expenses,
                                            members = members,
                                            wallets = wallets,
                                            balances = balances,
                                            settlements = transactions,
                                            splits = splits
                                        )
                                        if (pdfFile != null) {
                                            shareFile(context, pdfFile, "application/pdf")
                                        } else {
                                            Toast.makeText(context, "Failed to generate PDF Report", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("export_space_pdf")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF Report", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val currentSpaceObj = activeSpaceState
                                    if (currentSpaceObj != null) {
                                        val zipFile = com.example.utils.ExportEngine.exportSpacePackageZip(
                                            context = context,
                                            spaceName = currentSpaceObj.name,
                                            expenses = expenses,
                                            members = members,
                                            wallets = wallets,
                                            balances = balances,
                                            settlements = transactions,
                                            splits = splits
                                        )
                                        if (zipFile != null) {
                                            shareFile(context, zipFile, "application/zip")
                                        } else {
                                            Toast.makeText(context, "Failed to generate ZIP Package", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("export_space_zip")
                            ) {
                                Icon(Icons.Default.Archive, contentDescription = "", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ZIP Package", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Summary Card Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { summaryExpanded = !summaryExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Summary",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (summaryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Summary",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    if (summaryExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        balances.forEach { balance ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = balance.user.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Charged ₹${String.format(Locale.US, "%.2f", balance.totalOwed)}, Paid ₹${String.format(Locale.US, "%.2f", balance.totalPaid)}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                                
                                val isOwed = balance.netBalance > 0.01
                                val isSettled = kotlin.math.abs(balance.netBalance) <= 0.01
                                val sign = if (isSettled) "" else if (isOwed) "+" else "-"
                                val color = if (isSettled) MaterialTheme.colorScheme.onSurfaceVariant else if (isOwed) Color(0xFF1B5E20) else Color.Red
                                val displayText = if (isSettled) "Settled" else "${sign}₹${String.format(Locale.US, "%.2f", kotlin.math.abs(balance.netBalance))}"
                                
                                Text(
                                    text = displayText,
                                    color = color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            if (balance != balances.last()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }

        // How to settle Card Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settleExpanded = !settleExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "How to settle?",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (settleExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Settlement Plans",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (settleExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        if (transactions.isEmpty()) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = "Everyone is settled! No payments needed.",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            transactions.forEach { tx ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = tx.debtor.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "should pay to ${tx.creditor.name}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            fontSize = 11.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    }
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "₹${String.format(Locale.US, "%.2f", tx.amount)}",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Button(
                                            onClick = {
                                                if (wallets.isEmpty()) {
                                                    Toast.makeText(context, "Please create a funding wallet first under Profile -> Wallet Manager.", Toast.LENGTH_LONG).show()
                                                } else {
                                                    selectedWalletForSettle = wallets.first()
                                                    transactionToSettle = tx
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            modifier = Modifier.height(32.dp).testTag("settle_button_${tx.debtor.id}_${tx.creditor.id}"),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Settle Up", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                                if (tx != transactions.last()) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Group Members modification management section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { membersExpanded = !membersExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Group Members Management",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (membersExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Members List",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (membersExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        balances.forEach { balance ->
                            val member = balance.user
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    ) {
                                        Text(
                                            text = member.name.take(1).uppercase(),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = member.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { userToRename = member; renameNameText = member.name },
                                        modifier = Modifier.size(32.dp).testTag("edit_member_${member.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Rename Member",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (activeUser != null && member.id != activeUser!!.id) {
                                        IconButton(
                                            onClick = { userToRemove = member },
                                            modifier = Modifier.size(32.dp).testTag("remove_member_${member.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove Member",
                                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (balance != balances.last()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- DATA CLASS FOR ITEMIZED SPLIT ---
data class ReceiptItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val memberIds: List<Long> = emptyList()
)

// --- ADD EXPENSE SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(viewModel: ExpenseViewModel, onSaved: () -> Unit) {
    val spaces by viewModel.allSpaces.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    val editingExpense by viewModel.editingExpense.collectAsStateWithLifecycle()
    val editingExpenseSplitsFlow = remember(editingExpense) {
        if (editingExpense != null) {
            viewModel.getSplitsForExpense(editingExpense!!.id)
        } else {
            flowOf(emptyList())
        }
    }
    val editingExpenseSplits by editingExpenseSplitsFlow.collectAsStateWithLifecycle(emptyList())

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearEditingExpense()
        }
    }

    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var showCalculatorDialog by remember { mutableStateOf(false) }

    if (showCalculatorDialog) {
        CalculatorDialog(
            initialValue = amountText,
            onDismiss = { showCalculatorDialog = false },
            onConfirm = { amountText = it }
        )
    }

    val pendingAmount by viewModel.pendingExpenseAmount.collectAsStateWithLifecycle()
    LaunchedEffect(pendingAmount) {
        pendingAmount?.let { amt ->
            amountText = String.format(Locale.US, "%.2f", amt)
            viewModel.clearPendingExpenseAmount()
        }
    }
    var category by remember { mutableStateOf("Groceries") }

    // Dropdown States
    var selectedSpace by remember { mutableStateOf<Space?>(null) }
    var selectedPayerId by remember { mutableStateOf<Long?>(null) }
    
    var spaceExpanded by remember { mutableStateOf(false) }
    var payerExpanded by remember { mutableStateOf(false) }

    // Time/Date state
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val sdf = SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.US)

    // Secure local photo attachments state
    val attachedUris = remember { mutableStateListOf<String>() }
    var zoomTargetUri by remember { mutableStateOf<String?>(null) }
    var tempPhotoFile by remember { mutableStateOf<java.io.File?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var showAttachmentSourceDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                tempPhotoFile?.let { file ->
                    val persistentFile = java.io.File(context.filesDir, "bill_cam_${System.currentTimeMillis()}.jpg")
                    try {
                        file.inputStream().use { input ->
                            persistentFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        attachedUris.add(android.net.Uri.fromFile(persistentFile).toString())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    )

    val launchCameraDirectly = {
        try {
            val tempFile = java.io.File(context.cacheDir, "temp_capture_${System.currentTimeMillis()}.jpg")
            tempPhotoFile = tempFile
            val authority = "${context.packageName}.fileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, tempFile)
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Camera launch failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            launchCameraDirectly()
        } else {
            Toast.makeText(context, "Camera permission is required to take photos of bills.", Toast.LENGTH_LONG).show()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(5),
        onResult = { uris ->
            uris.take(5).forEach { uri ->
                if (attachedUris.size < 5) {
                    val localUriStr = copyUriToLocal(context, uri)
                    if (localUriStr != null) {
                        attachedUris.add(localUriStr)
                    }
                }
            }
        }
    )

    // Tab Index: 0 -> Equally, 1 -> Exact Amount, 2 -> Percentage, 3 -> Itemized Receipt
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("Split Equally", "Exact Amount", "Percentage", "Itemized Receipt")

    // Custom Split states
    val checkedMembers = remember { mutableStateMapOf<Long, Boolean>() }
    val exactAmounts = remember { mutableStateMapOf<Long, String>() }
    val percentages = remember { mutableStateMapOf<Long, String>() }
    
    // Receipt Split states
    var nextItemName by remember { mutableStateOf("") }
    var nextItemPrice by remember { mutableStateOf("") }
    val receiptItems = remember { mutableStateListOf<ReceiptItem>() }

    // Member list of selected space
    val activeMembersList = remember { mutableStateListOf<User>() }

    // Dynamic database states
    val wallets by viewModel.allWallets.collectAsStateWithLifecycle(emptyList())
    val categoriesDb by viewModel.allCategories.collectAsStateWithLifecycle(emptyList())

    var selectedWalletId by remember { mutableStateOf<Long>(1L) }

    // Automatically select the first wallet if not already selected
    LaunchedEffect(wallets) {
        if (wallets.isNotEmpty() && !wallets.any { it.id == selectedWalletId }) {
            selectedWalletId = wallets.first().id
        }
    }

    // Automatically select the first category if current is not in db
    LaunchedEffect(categoriesDb) {
        if (categoriesDb.isNotEmpty() && !categoriesDb.any { it.name == category }) {
            category = categoriesDb.first().name
        }
    }

    LaunchedEffect(editingExpense, editingExpenseSplits, spaces) {
        val expense = editingExpense
        if (expense != null) {
            description = expense.description
            amountText = String.format(Locale.US, "%.2f", expense.amount)
            category = expense.category
            selectedSpace = spaces.find { it.id == expense.spaceId }
            selectedPayerId = expense.paidById
            selectedTimestamp = expense.date
            selectedWalletId = expense.walletId ?: wallets.firstOrNull()?.id ?: 1L
            attachedUris.clear()
            expense.attachmentUris?.let { attachedUris.addAll(it) }

            if (editingExpenseSplits.isNotEmpty()) {
                checkedMembers.clear()
                exactAmounts.clear()
                percentages.clear()
                editingExpenseSplits.forEach { split ->
                    val isParticipating = split.amountOwed > 0.0
                    checkedMembers[split.userId] = isParticipating
                    exactAmounts[split.userId] = if (isParticipating) String.format(Locale.US, "%.2f", split.amountOwed) else ""
                    val pct = if (expense.amount > 0) (split.amountOwed / expense.amount) * 100.0 else 0.0
                    percentages[split.userId] = if (isParticipating) String.format(Locale.US, "%.1f", pct) else ""
                }
                val participatingSplits = editingExpenseSplits.filter { it.amountOwed > 0.0 }
                val firstOwed = participatingSplits.firstOrNull()?.amountOwed ?: 0.0
                val allEqual = participatingSplits.all { kotlin.math.abs(it.amountOwed - firstOwed) < 0.02 }
                if (allEqual) {
                    selectedTabIndex = 0
                } else {
                    selectedTabIndex = 1
                }
            }
        }
    }

    val activeSpace by viewModel.activeSpace.collectAsStateWithLifecycle()

    // Auto-update default selection to active space if set, otherwise fallback
    LaunchedEffect(activeSpace, spaces) {
        if (editingExpense == null) {
            if (activeSpace != null) {
                selectedSpace = activeSpace
            } else if (spaces.isNotEmpty() && selectedSpace == null) {
                selectedSpace = spaces.first()
            }
        }
    }

    // Load actual group members for split
    val currentSpaceId = selectedSpace?.id
    val spaceMembersFlow = remember(currentSpaceId) {
        if (currentSpaceId != null) viewModel.getMembersOfSpace(currentSpaceId) else flowOf(emptyList())
    }
    val spaceMembers by spaceMembersFlow.collectAsStateWithLifecycle(emptyList())

    LaunchedEffect(spaceMembers) {
        if (spaceMembers.isNotEmpty()) {
            activeMembersList.clear()
            activeMembersList.addAll(spaceMembers)
            
            // Populate defaults
            spaceMembers.forEach { member ->
                if (!checkedMembers.containsKey(member.id)) {
                    checkedMembers[member.id] = true
                }
                if (!exactAmounts.containsKey(member.id)) {
                    exactAmounts[member.id] = ""
                }
                if (!percentages.containsKey(member.id)) {
                    percentages[member.id] = ""
                }
            }
            if (selectedPayerId == null || !spaceMembers.any { it.id == selectedPayerId }) {
                selectedPayerId = spaceMembers.first().id
            }
        }
    }

    // When items are updated in Itemized Receipt, update the total amount text automatically
    val receiptTotal = receiptItems.sumOf { it.price }
    LaunchedEffect(receiptTotal, selectedTabIndex) {
        if (selectedTabIndex == 3 && receiptTotal > 0.0) {
            amountText = String.format(Locale.US, "%.2f", receiptTotal)
        }
    }

    val darkTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    val isDark = MaterialTheme.colorScheme.background.let { it.red + it.green + it.blue < 1.5f }
    val contrastGreen = if (isDark) Color(0xFF4CAF50) else Color(0xFF1B5E20)
    val contrastYellow = if (isDark) Color(0xFFFFEB3B) else Color(0xFFE65100)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (selectedSpace == null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Please select a space below to associate this expense.",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = if (editingExpense != null) "Edit Bill or Expense" else "Add Bill or Expense",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Horizontal scrollable Wallet Selector
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select Funding Wallet",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_selector")
                ) {
                    lazyItems(wallets) { wallet ->
                        val isSelected = wallet.id == selectedWalletId
                        val cardBgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        val cardBorderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        val icon = when (wallet.type.lowercase()) {
                            "cash" -> Icons.Default.Payments
                            "credit card" -> Icons.Default.CreditCard
                            else -> Icons.Default.AccountBalance
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                            border = BorderStroke(1.dp, cardBorderColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .width(145.dp)
                                .clickable { selectedWalletId = wallet.id }
                                .testTag("wallet_chip_${wallet.name}")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = wallet.type,
                                    tint = textColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = wallet.name,
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹${String.format(Locale.US, "%.2f", wallet.balance)}",
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Description Input Form Field
        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("What was this for? (e.g., Pizza, Fuel)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_description"),
                colors = darkTextFieldColors,
                singleLine = true
            )
        }

        // Amount Input Field
        item {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Total Amount (₹)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_input"),
                colors = darkTextFieldColors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                leadingIcon = {
                    Text(
                        text = "₹",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showCalculatorDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Open Calculator",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }

        // Date and Time selection
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val calendar = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                calendar.set(Calendar.YEAR, year)
                                calendar.set(Calendar.MONTH, month)
                                calendar.set(Calendar.DAY_OF_MONTH, day)
                                
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                                        calendar.set(Calendar.MINUTE, minute)
                                        selectedTimestamp = calendar.timeInMillis
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .testTag("date_picker_trigger")
            ) {
                OutlinedTextField(
                    value = sdf.format(Date(selectedTimestamp)),
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Date & Time") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Select Date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledContainerColor = Color.Transparent,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // Attach Bill & PhotoPicker section (Max 5 items)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attach Bills / Receipts (${attachedUris.size}/5)",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (attachedUris.size < 5) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gallery button
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("attach_bill_trigger")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Gallery",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gallery", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }

                        // Camera button
                        OutlinedButton(
                            onClick = {
                                if (hasCameraPermission) {
                                    launchCameraDirectly()
                                } else {
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("camera_bill_trigger")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Camera",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                
                if (attachedUris.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable {
                                if (attachedUris.size < 5) {
                                    showAttachmentSourceDialog = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Upload", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tap here to attach image bills", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        lazyItems(attachedUris) { uriStr ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                    .clickable { zoomTargetUri = uriStr }
                            ) {
                                AsyncImage(
                                    model = uriStr,
                                    contentDescription = "Receipt Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Remove button trigger
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .clickable { attachedUris.remove(uriStr) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Deselect",
                                        tint = Color.Red,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (zoomTargetUri != null) {
                    ZoomableImageDialog(
                        uriStr = zoomTargetUri!!,
                        onDismiss = { zoomTargetUri = null }
                    )
                }

                if (showAttachmentSourceDialog) {
                    AlertDialog(
                        onDismissRequest = { showAttachmentSourceDialog = false },
                        title = { Text("Attach Bill / Receipt") },
                        text = { Text("Choose how you want to add the receipt attachment:") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showAttachmentSourceDialog = false
                                    if (hasCameraPermission) {
                                        launchCameraDirectly()
                                    } else {
                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                }
                            ) {
                                Text("Use Camera")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showAttachmentSourceDialog = false
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Text("From Gallery")
                            }
                        }
                    )
                }
            }
        }

        // Space selector details (Interactive dropdown when activeSpace == null)
        item {
            Text(
                text = "Settle Space:",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (activeSpace == null && editingExpense == null) {
                ExposedDropdownMenuBox(
                    expanded = spaceExpanded,
                    onExpandedChange = { spaceExpanded = !spaceExpanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedSpace?.name ?: "Select a Space",
                        onValueChange = {},
                        label = { Text("Group/Space") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = spaceExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("space_selector"),
                        colors = darkTextFieldColors
                    )
                    ExposedDropdownMenu(
                        expanded = spaceExpanded,
                        onDismissRequest = { spaceExpanded = false }
                    ) {
                        spaces.forEach { spaceItem ->
                            DropdownMenuItem(
                                text = { Text(spaceItem.name) },
                                onClick = {
                                    selectedSpace = spaceItem
                                    spaceExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    readOnly = true,
                    value = selectedSpace?.name ?: "No Space Selected",
                    onValueChange = {},
                    label = { Text("Group/Space") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("space_selector"),
                    colors = darkTextFieldColors
                )
            }
        }

        // Payer selector details
        if (selectedSpace != null && spaceMembers.isNotEmpty()) {
            item {
                Text(
                    text = "Who paid?",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = payerExpanded,
                    onExpandedChange = { payerExpanded = !payerExpanded }
                ) {
                    val payerName = spaceMembers.find { it.id == selectedPayerId }?.name ?: "Select Payer"
                    OutlinedTextField(
                        readOnly = true,
                        value = payerName,
                        onValueChange = {},
                        label = { Text("Paid By") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payerExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("payer_selector"),
                        colors = darkTextFieldColors
                    )
                    ExposedDropdownMenu(
                        expanded = payerExpanded,
                        onDismissRequest = { payerExpanded = false }
                    ) {
                        spaceMembers.forEach { member ->
                            DropdownMenuItem(
                                text = { Text(member.name) },
                                onClick = {
                                    selectedPayerId = member.id
                                    payerExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Custom Category Chips
        item {
            Text(
                text = "Category (Parent ➔ Sub-category)",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            val displayedCategories = if (categoriesDb.isNotEmpty()) {
                categoriesDb.map { cat ->
                    val parent = categoriesDb.find { it.id == cat.parentId }
                    val label = if (parent != null) "${parent.name} ➔ ${cat.name}" else cat.name
                    cat.name to label
                }
            } else {
                listOf("Groceries", "Transport", "Bills", "General").map { it to it }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                lazyItems(displayedCategories) { (name, label) ->
                    val selected = category == name
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clickable { category = name }
                            .testTag("category_chip_$name")
                    ) {
                        Text(
                            text = label,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Navigation Tabs for split choices
        if (selectedSpace != null && spaceMembers.isNotEmpty()) {
            item {
                Text(
                    text = "Split Options",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }

            // Split engine view content based on Tab select
            item {
                when (selectedTabIndex) {
                    0 -> {
                        // Split Equally
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Who participates in equal split?",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    spaceMembers.forEach { member ->
                                        val isChecked = checkedMembers[member.id] ?: true
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { checkedMembers[member.id] = !isChecked }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = null,
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = MaterialTheme.colorScheme.primary,
                                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                modifier = Modifier.testTag("equal_checkbox_${member.id}")
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                            ) {
                                                Text(
                                                    text = member.name.take(1).uppercase(),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(text = member.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.weight(1f))

                                            val checkedCount = checkedMembers.values.count { it }
                                            val amountVal = amountText.toDoubleOrNull() ?: 0.0
                                            val share = if (isChecked && checkedCount > 0) amountVal / checkedCount else 0.0
                                            Text(
                                                text = "₹${String.format(Locale.US, "%.2f", share)}",
                                                color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Exact Amount
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Assign Custom Dollar Amounts:",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                TextButton(
                                    onClick = {
                                        val amountVal = amountText.toDoubleOrNull() ?: 0.0
                                        if (spaceMembers.isNotEmpty()) {
                                            val share = amountVal / spaceMembers.size
                                            spaceMembers.forEach { m ->
                                                exactAmounts[m.id] = String.format(Locale.US, "%.2f", share)
                                            }
                                        }
                                    }
                                ) {
                                    Text("Distribute Equally", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    spaceMembers.forEach { member ->
                                        val currentVal = exactAmounts[member.id] ?: ""
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                            ) {
                                                Text(
                                                    text = member.name.take(1).uppercase(),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(text = member.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))

                                            OutlinedTextField(
                                                value = currentVal,
                                                onValueChange = { exactAmounts[member.id] = it },
                                                placeholder = { Text("0.00", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                colors = darkTextFieldColors,
                                                singleLine = true,
                                                modifier = Modifier.width(110.dp).testTag("exact_${member.id}"),
                                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                            )
                                        }
                                    }

                                    val sumPaid = exactAmounts.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
                                    val totalEntered = amountText.toDoubleOrNull() ?: 0.0
                                    val difference = totalEntered - sumPaid
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Total Assigned: ₹${String.format(Locale.US, "%.2f", sumPaid)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (Math.abs(difference) > 0.02) {
                                            Text(
                                                text = "Unassigned: ₹${String.format(Locale.US, "%.2f", difference)}",
                                                fontSize = 12.sp,
                                                color = if (difference > 0) contrastYellow else Color.Red,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else {
                                            Text(text = "Matches Total!", fontSize = 12.sp, color = contrastGreen, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Percentage
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Assign Custom Percentages:",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                TextButton(
                                    onClick = {
                                        if (spaceMembers.isNotEmpty()) {
                                            val pct = 100.0 / spaceMembers.size
                                            spaceMembers.forEach { m ->
                                                percentages[m.id] = String.format(Locale.US, "%.1f", pct)
                                            }
                                        }
                                    }
                                ) {
                                    Text("Distribute Equally", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    spaceMembers.forEach { member ->
                                        val currentPct = percentages[member.id] ?: ""
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                            ) {
                                                Text(
                                                    text = member.name.take(1).uppercase(),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(text = member.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))

                                            val totalEntered = amountText.toDoubleOrNull() ?: 0.0
                                            val mPct = currentPct.toDoubleOrNull() ?: 0.0
                                            val calculatedShare = totalEntered * (mPct / 100.0)
                                            Text(
                                                text = "₹${String.format(Locale.US, "%.2f", calculatedShare)}",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )

                                            OutlinedTextField(
                                                value = currentPct,
                                                onValueChange = { percentages[member.id] = it },
                                                placeholder = { Text("0 %", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                colors = darkTextFieldColors,
                                                singleLine = true,
                                                modifier = Modifier.width(90.dp).testTag("pct_${member.id}"),
                                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                            )
                                        }
                                    }

                                    val sumPct = percentages.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
                                    val diffPct = 100.0 - sumPct
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Total Percent: ${String.format(Locale.US, "%.1f", sumPct)}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (Math.abs(diffPct) > 0.05) {
                                            Text(
                                                text = "Remaining: ${String.format(Locale.US, "%.1f", diffPct)}%",
                                                fontSize = 12.sp,
                                                color = if (diffPct > 0) contrastYellow else Color.Red,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else {
                                            Text(text = "Matches 100%!", fontSize = 12.sp, color = contrastGreen, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        // Itemized Receipt
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Add Item to Receipt:",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = nextItemName,
                                    onValueChange = { nextItemName = it },
                                    label = { Text("Item (e.g., Burger)", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1.5f).testTag("receipt_item_name_input"),
                                    colors = darkTextFieldColors,
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = nextItemPrice,
                                    onValueChange = { nextItemPrice = it },
                                    label = { Text("Price (₹)", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1.2f).testTag("receipt_item_price_input"),
                                    colors = darkTextFieldColors,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        val price = nextItemPrice.toDoubleOrNull()
                                        if (nextItemName.isNotBlank() && price != null && price > 0.0) {
                                            receiptItems.add(
                                                ReceiptItem(
                                                    name = nextItemName,
                                                    price = price,
                                                    memberIds = spaceMembers.map { it.id } // assign all by default
                                                )
                                            )
                                            nextItemName = ""
                                            nextItemPrice = ""
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier
                                        .height(56.dp)
                                        .testTag("add_receipt_item_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                                }
                            }

                            if (receiptItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Receipt Items (Tap avatar to toggle assignment):",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                receiptItems.forEachIndexed { idx, item ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = item.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    val perPerson = if (item.memberIds.isNotEmpty()) item.price / item.memberIds.size else 0.0
                                                    Text(
                                                        text = "Price: ₹${String.format(Locale.US, "%.2f", item.price)} (${item.memberIds.size} split: ₹${String.format(Locale.US, "%.2f", perPerson)} each)",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { receiptItems.removeAt(idx) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Member avatar selection for item
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                spaceMembers.forEach { member ->
                                                    val isAssigned = item.memberIds.contains(member.id)
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isAssigned) MaterialTheme.colorScheme.primary 
                                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                                            )
                                                            .clickable {
                                                                val curSelected = item.memberIds.toMutableList()
                                                                if (isAssigned) {
                                                                    curSelected.remove(member.id)
                                                                } else {
                                                                    curSelected.add(member.id)
                                                                }
                                                                receiptItems[idx] = item.copy(memberIds = curSelected)
                                                            }
                                                            .border(
                                                                1.dp,
                                                                if (isAssigned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                                CircleShape
                                                            )
                                                            .testTag("receipt_item_${item.id}_avatar_${member.id}")
                                                    ) {
                                                        Text(
                                                            text = member.name.take(1).uppercase(),
                                                            color = if (isAssigned) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                            fontSize = 11.sp,
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
            }
        }

        // Action Trigger Button
        item {
            Button(
                onClick = {
                    val finalAmount = amountText.toDoubleOrNull() ?: 0.0
                    val sId = selectedSpace?.id
                    val pId = selectedPayerId
                    if (description.isNotBlank() && finalAmount > 0.0 && sId != null && pId != null) {
                        val finalUris = attachedUris.toList()
                        val expenseId = editingExpense?.id ?: 0L
                        when (selectedTabIndex) {
                            0 -> {
                                val participatingIds = checkedMembers.filter { it.value }.map { it.key }
                                val targetParticipantIds = if (participatingIds.isEmpty()) spaceMembers.map { it.id } else participatingIds
                                viewModel.addExpenseEqualSplit(
                                    spaceId = sId,
                                    paidById = pId,
                                    description = description,
                                    amount = finalAmount,
                                    category = category,
                                    participantIds = targetParticipantIds,
                                    date = selectedTimestamp,
                                    walletId = selectedWalletId,
                                    attachmentUris = finalUris,
                                    expenseId = expenseId
                                )
                            }
                            1 -> {
                                val splitsMap = mutableMapOf<Long, Double>()
                                spaceMembers.forEach { m ->
                                    splitsMap[m.id] = exactAmounts[m.id]?.toDoubleOrNull() ?: 0.0
                                }
                                viewModel.addExpenseExactSplit(
                                    spaceId = sId,
                                    paidById = pId,
                                    description = description,
                                    amount = finalAmount,
                                    category = category,
                                    splitsMap = splitsMap,
                                    date = selectedTimestamp,
                                    walletId = selectedWalletId,
                                    attachmentUris = finalUris,
                                    expenseId = expenseId
                                )
                            }
                            2 -> {
                                val splitsMap = mutableMapOf<Long, Double>()
                                spaceMembers.forEach { m ->
                                    val pct = percentages[m.id]?.toDoubleOrNull() ?: 0.0
                                    splitsMap[m.id] = finalAmount * (pct / 100.0)
                                }
                                viewModel.addExpenseExactSplit(
                                    spaceId = sId,
                                    paidById = pId,
                                    description = description,
                                    amount = finalAmount,
                                    category = category,
                                    splitsMap = splitsMap,
                                    date = selectedTimestamp,
                                    walletId = selectedWalletId,
                                    attachmentUris = finalUris,
                                    expenseId = expenseId
                                )
                            }
                            3 -> {
                                val splitsMap = mutableMapOf<Long, Double>()
                                spaceMembers.forEach { m -> splitsMap[m.id] = 0.0 }
                                receiptItems.forEach { item ->
                                    if (item.memberIds.isNotEmpty()) {
                                        val share = item.price / item.memberIds.size
                                        item.memberIds.forEach { mId ->
                                            val currentVal = splitsMap[mId] ?: 0.0
                                            splitsMap[mId] = currentVal + share
                                        }
                                    }
                                }
                                viewModel.addExpenseExactSplit(
                                    spaceId = sId,
                                    paidById = pId,
                                    description = description,
                                    amount = finalAmount,
                                    category = category,
                                    splitsMap = splitsMap,
                                    date = selectedTimestamp,
                                    walletId = selectedWalletId,
                                    attachmentUris = finalUris,
                                    expenseId = expenseId
                                )
                            }
                        }
                        viewModel.clearEditingExpense()
                        onSaved()
                    }
                },
                enabled = description.isNotBlank() && amountText.toDoubleOrNull() != null && selectedSpace != null && spaceMembers.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_expense_button")
            ) {
                Text(
                    text = if (editingExpense != null) "Update Expense & Split" else "Save Expense & Split",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// --- PROFILE SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ExpenseViewModel,
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    onNavigateToCategoryManager: () -> Unit,
    onNavigateToBudgetManager: () -> Unit,
    onNavigateToSubscriptionManager: () -> Unit,
    onNavigateToWalletManager: () -> Unit
) {
    val activeUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameText by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                activeUser?.let { user ->
                    viewModel.updateUserAvatar(user.id, it.toString())
                }
            }
        }
    )

    if (showEditNameDialog && activeUser != null) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Profile Name") },
            text = {
                OutlinedTextField(
                    value = editNameText,
                    onValueChange = { editNameText = it },
                    singleLine = true,
                    label = { Text("Profile Name") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editNameText.isNotBlank()) {
                            viewModel.updateUserProfile(activeUser!!.id, editNameText.trim())
                            showEditNameDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Manage Profiles & Users",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Active Profile details Card (visual delight)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        mediaType = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                    ) {
                        if (activeUser?.avatarUrl?.isNotBlank() == true) {
                            coil.compose.AsyncImage(
                                model = activeUser!!.avatarUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = (activeUser?.name ?: "Guest").take(1).uppercase(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "CURRENT ACTIVE PROFILE",
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeUser?.name ?: "Loading...",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    editNameText = activeUser?.name ?: ""
                                    showEditNameDialog = true
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Name",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = activeUser?.email ?: "",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Display Sizing & Font Accessibility Settings Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextFormat,
                            contentDescription = "Font Scaling",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Text Size Settings",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Adjust text size multiplier across the entire application interface.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "A-",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = fontScale,
                            onValueChange = onFontScaleChange,
                            valueRange = 0.8f..1.5f,
                            steps = 6, // 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5
                            modifier = Modifier.weight(1f).testTag("font_scale_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )
                        Text(
                            text = "A+",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(fontScale * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(42.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }

        // Settings Wallet Auditing & Ledger Exports Card
        item {
            val context = LocalContext.current
            val wallets by viewModel.allWallets.collectAsStateWithLifecycle(emptyList())
            val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle(emptyList())
            var selectedWalletIndex by remember { mutableStateOf(0) }
            var dropdownExpanded by remember { mutableStateOf(false) }

            if (wallets.isNotEmpty()) {
                val currentWallet = wallets.getOrNull(selectedWalletIndex) ?: wallets.first()
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Wallet Auditing",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Wallet Auditing & Exports",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select a wallet funding channel to audit and export formatted ledger spreadsheets or report files.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Wallet dropdown selector
                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                        ) {
                            OutlinedTextField(
                                readOnly = true,
                                value = "${currentWallet.name} (${currentWallet.type})",
                                onValueChange = {},
                                label = { Text("Select Wallet") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .testTag("audit_wallet_selector"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                wallets.forEachIndexed { index, w ->
                                    DropdownMenuItem(
                                        text = { Text("${w.name} (${w.type})") },
                                        onClick = {
                                            selectedWalletIndex = index
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val excelFile = com.example.utils.ExportEngine.exportWalletToExcel(
                                        context = context,
                                        wallet = currentWallet,
                                        expenses = allExpenses,
                                        members = allUsers
                                    )
                                    if (excelFile != null) {
                                        shareFile(context, excelFile, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                    } else {
                                        Toast.makeText(context, "Failed to compile wallet Excel sheet", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("export_wallet_excel")
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = "", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export XLSX", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val csvFile = com.example.utils.ExportEngine.exportWalletToCSV(
                                        context = context,
                                        wallet = currentWallet,
                                        expenses = allExpenses,
                                        members = allUsers
                                    )
                                    if (csvFile != null) {
                                        shareFile(context, csvFile, "text/csv")
                                    } else {
                                        Toast.makeText(context, "Failed to compile wallet CSV", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("export_wallet_csv")
                            ) {
                                Icon(Icons.Default.Description, contentDescription = "", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CSV", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val pdfFile = com.example.utils.ExportEngine.generateWalletReportPDF(
                                        context = context,
                                        wallet = currentWallet,
                                        expenses = allExpenses,
                                        members = allUsers
                                    )
                                    if (pdfFile != null) {
                                        shareFile(context, pdfFile, "application/pdf")
                                    } else {
                                        Toast.makeText(context, "Failed to compile wallet PDF", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.2f).height(38.dp).testTag("export_wallet_pdf")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF Report", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Settings Wallet Manager Navigation Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToWalletManager() }
                    .testTag("manage_wallets_nav_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Wallets",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Wallet Manager",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Add, edit, or delete funding wallets",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Navigate",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Settings Category Manager Navigation Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToCategoryManager() }
                    .testTag("manage_categories_nav_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = "Categories",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Category Manager",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Add, edit, or nest expense categories",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Navigate",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Settings Budget Manager Navigation Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToBudgetManager() }
                    .testTag("manage_budgets_nav_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = "Budgets",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Budget Manager",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Set global or per-category monthly limits",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Navigate",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Settings Subscription Manager Navigation Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSubscriptionManager() }
                    .testTag("manage_subscriptions_nav_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "Subscriptions",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Subscription Manager",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Manage automated recurring expenses & bills",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Navigate",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// --- PEER-TO-PEER QR SHARING CODEC HELPERS ---
val sharingJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun compressString(input: String): String {
    val arr = input.toByteArray(Charsets.UTF_8)
    val compressor = java.util.zip.Deflater()
    compressor.setInput(arr)
    compressor.finish()
    val bos = java.io.ByteArrayOutputStream(arr.size)
    val buf = ByteArray(1024)
    while (!compressor.finished()) {
        val count = compressor.deflate(buf)
        bos.write(buf, 0, count)
    }
    bos.close()
    return android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP)
}

fun decompressString(compressedBase64: String): String {
    val arr = android.util.Base64.decode(compressedBase64, android.util.Base64.NO_WRAP)
    val decompressor = java.util.zip.Inflater()
    decompressor.setInput(arr)
    val bos = java.io.ByteArrayOutputStream(arr.size)
    val buf = ByteArray(1024)
    while (!decompressor.finished()) {
        val count = decompressor.inflate(buf)
        bos.write(buf, 0, count)
    }
    bos.close()
    return bos.toString("UTF-8")
}

fun generateQRCodeBitmap(content: String, size: Int = 512): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun saveBitmapToGallery(context: Context, bitmap: Bitmap, spaceName: String): Boolean {
    val filename = "TravelSplit_Space_${spaceName.replace(" ", "_")}_${System.currentTimeMillis()}.png"
    val contentValues = android.content.ContentValues().apply {
        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/TravelSplit")
            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return false

    return try {
        resolver.openOutputStream(imageUri).use { outputStream ->
            if (outputStream == null) return false
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(imageUri, contentValues, null, null)
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        resolver.delete(imageUri, null, null)
        false
    }
}

class QRCodeAnalyzer(
    private val onQRCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient()

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { rawValue ->
                            onQRCodeDetected(rawValue)
                        }
                    }
                }
                .addOnFailureListener {
                    it.printStackTrace()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}

@Composable
fun CameraScannerView(
    onQRCodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val executor = ContextCompat.getMainExecutor(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build().apply {
                            setAnalyzer(executor, QRCodeAnalyzer { code ->
                                onQRCodeScanned(code)
                            })
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, executor)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay with scanner target frame and Close button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Scanner target frame
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    .background(Color.Transparent)
            )

            // Close button at top-right
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Camera",
                    tint = Color.White
                )
            }

            Text(
                text = "Align QR code inside the frame to import space",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun ShareSpaceDialog(
    spaceId: Long,
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var spaceName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(spaceId) {
        try {
            val payload = viewModel.getSharePayloadForSpace(spaceId)
            if (payload != null) {
                spaceName = payload.spaceName
                val jsonStr = sharingJson.encodeToString(SharedSpacePayload.serializer(), payload)
                val compressedStr = compressString(jsonStr)
                val bitmap = generateQRCodeBitmap(compressedStr)
                if (bitmap != null) {
                    qrBitmap = bitmap
                } else {
                    errorMsg = "Failed to generate QR Code image"
                }
            } else {
                errorMsg = "Space not found"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMsg = e.message ?: "An unexpected error occurred"
        } finally {
            isLoading = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Share Space",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )

                Text(
                    text = "A friend can scan this QR code to join this space and import all associated bills instantly.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else if (errorMsg != null) {
                        Text(text = errorMsg!!, color = Color.Red, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    } else if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val bitmap = qrBitmap
                            if (bitmap != null) {
                                val success = saveBitmapToGallery(context, bitmap, spaceName)
                                if (success) {
                                    Toast.makeText(context, "QR Code downloaded to Pictures/TravelSplit", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = qrBitmap != null,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download QR",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Download", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Close", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ZoomableImageDialog(
    uriStr: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Receipt Attachment (Pinch/Drag)",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) {
                                    val maxOffsetX = (size.width * (scale - 1)) / 2
                                    val maxOffsetY = (size.height * (scale - 1)) / 2
                                    offset = Offset(
                                        (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                        (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                    )
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = uriStr,
                        contentDescription = "Receipt Detail",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                }
            }
        }
    }
}

/**
 * Copy visual media files to local storage to maintain persistence and prevent dynamic URI permission expiration
 */
private fun copyUriToLocal(context: Context, uri: Uri): String? {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "bill_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}.jpg"
        val file = java.io.File(context.filesDir, fileName)
        file.outputStream().use { outputStream ->
            inputStream.use { it.copyTo(outputStream) }
        }
        return Uri.fromFile(file).toString()
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

/**
 * Share any generated CSV, Excel, or PDF report securely via system chooser
 */
private fun shareFile(context: Context, file: java.io.File, mimeType: String) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            putExtra(Intent.EXTRA_TEXT, "Here is the requested ledger audit file generated securely by Travel Split.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Export File"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Sharing failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Interactive fullscreen slideshow on how to use Settle Split with dynamic Compose illustrations.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onUserCreated: (String) -> Unit
) {
    var currentSlide by remember { mutableStateOf(0) }
    var userName by remember { mutableStateOf("") }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val totalSlides = 4

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
            .systemBarsPadding()
            .padding(24.dp)
            .imePadding()
    ) {
        // Skip Button on top right
        if (currentSlide < totalSlides - 1) {
            TextButton(
                onClick = { currentSlide = totalSlides - 1 },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .testTag("onboarding_skip_button")
            ) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val isKeyboardOpen = WindowInsets.isImeVisible
        val scrollState = rememberScrollState()

        // Slide Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .verticalScroll(scrollState)
                .padding(top = 48.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Dynamic Illustration Card
            if (!isKeyboardOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (currentSlide) {
                        0 -> OnboardingIllustrations.Welcome()
                        1 -> OnboardingIllustrations.SpacesAndWallets()
                        2 -> OnboardingIllustrations.ToolsAndAudits()
                        3 -> OnboardingIllustrations.UserSetup()
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Slide Title & Description
            when (currentSlide) {
                0 -> {
                    Text(
                        text = "Travel Split Ledger",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Welcome to Travel Split. Manage bills, track local wallets, dynamic compound investments, and divide shares among group members seamlessly in Indian Rupees (₹).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
                1 -> {
                    Text(
                        text = "Separate Spaces",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Create custom group 'Spaces' for roommate rentals, long tours, picnics, or projects. Fund them from separate Ledger Wallets (Cash, Cards, Banks) with zero chaos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
                2 -> {
                    Text(
                        text = "Smart Financial Tools",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Explore powerful tools: EMI calculators, compounding savings targets, GST tax adjusters, and export perfect ledger spreadsheets (Excel, CSV, PDF reports) instantly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
                3 -> {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Enter your username to initialize your workspace. You can optionally list multiple initial users separated by commas (e.g. Rohan, Amit, Sunil) to setup your split ledger circles.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Input Box for Username
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        placeholder = { Text("Your name (or comma-separated, e.g. Rohan, Amit)", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_username_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        }

        // Bottom Controls Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dots indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalSlides) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == currentSlide) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == currentSlide) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button
            Button(
                onClick = {
                    if (currentSlide < totalSlides - 1) {
                        currentSlide++
                    } else {
                        if (userName.isNotBlank()) {
                            focusManager.clearFocus()
                            onUserCreated(userName.trim())
                        }
                    }
                },
                enabled = currentSlide < totalSlides - 1 || userName.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("onboarding_action_button")
            ) {
                Text(
                    text = if (currentSlide < totalSlides - 1) "Next" else "Finish & Create Workspace",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (currentSlide < totalSlides - 1 || userName.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Beautiful vector/canvas designs representing onboarding screens
 */
object OnboardingIllustrations {
    @Composable
    fun Welcome() {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = this.center
                
                // Draw clean concentric glowing circles
                drawCircle(
                    color = androidx.compose.ui.graphics.Color(0xFF2E6FF3).copy(alpha = 0.1f),
                    radius = 110f,
                    center = centerOffset
                )
                drawCircle(
                    color = androidx.compose.ui.graphics.Color(0xFF2E6FF3).copy(alpha = 0.2f),
                    radius = 75f,
                    center = centerOffset
                )
                
                // Draw shiny glowing core
                drawCircle(
                    color = androidx.compose.ui.graphics.Color(0xFF2E6FF3),
                    radius = 45f,
                    center = centerOffset
                )
            }
            
            Text(
                text = "₹",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }

    @Composable
    fun SpacesAndWallets() {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Draw sample visual cards for "Spaces"
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF26A69A).copy(alpha = 0.15f))
                    .border(1.5.dp, Color(0xFF26A69A), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Group, contentDescription = "", tint = Color(0xFF26A69A), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Spaces", color = Color(0xFF26A69A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Icon(
                Icons.Default.ArrowForward,
                contentDescription = "",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )

            // Draw card for "Wallets"
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFAB47BC).copy(alpha = 0.15f))
                    .border(1.5.dp, Color(0xFFAB47BC), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = "", tint = Color(0xFFAB47BC), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Wallets", color = Color(0xFFAB47BC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    @Composable
    fun ToolsAndAudits() {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.size(140.dp)) {
                // Drawing nice background ring
                drawCircle(
                    color = androidx.compose.ui.graphics.Color(0xFFFF9800).copy(alpha = 0.12f),
                    radius = 90f
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = "",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Reports",
                    color = Color(0xFFFF9800),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }

    @Composable
    fun UserSetup() {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Create Profile",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun evaluateExpression(expr: String): Double {
    return ExpressionParser(expr).parse()
}

class ExpressionParser(private val expr: String) {
    private var pos = 0
    private val sanitized: String

    init {
        var s = expr.replace(" ", "")
        s = s.replace("×", "*")
        s = s.replace("÷", "/")
        sanitized = s
    }

    private fun peek(): Char? {
        return if (pos < sanitized.length) sanitized[pos] else null
    }

    private fun consume(char: Char): Boolean {
        val p = peek()
        if (p != null && p == char) {
            pos++
            return true
        }
        return false
    }

    fun parse(): Double {
        try {
            val result = parseExpression()
            if (pos >= sanitized.length) {
                return result
            }
            throw IllegalArgumentException("Unexpected character at $pos")
        } catch (e: Exception) {
            return Double.NaN
        }
    }

    private fun parseExpression(): Double {
        var left = parseTerm()
        while (true) {
            if (consume('+')) {
                left += parseTerm()
            } else if (consume('-')) {
                left -= parseTerm()
            } else {
                return left
            }
        }
    }

    private fun parseTerm(): Double {
        var left = parseFactor()
        while (true) {
            if (consume('*')) {
                left *= parseFactor()
            } else if (consume('/')) {
                val right = parseFactor()
                if (right == 0.0) {
                    throw ArithmeticException("Division by zero")
                }
                left /= right
            } else {
                return left
            }
        }
    }

    private fun parseFactor(): Double {
        if (consume('(')) {
            val result = parseExpression()
            if (!consume(')')) {
                throw IllegalArgumentException("Mismatched parentheses")
            }
            return result
        }
        var sign = 1.0
        if (consume('-')) {
            sign = -1.0
        } else {
            consume('+')
        }
        val start = pos
        while (pos < sanitized.length && (sanitized[pos].isDigit() || sanitized[pos] == '.')) {
            pos++
        }
        if (start == pos) {
            throw IllegalArgumentException("Unexpected character at $pos")
        }
        val token = sanitized.substring(start, pos)
        val value = token.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number: $token")
        return sign * value
    }
}

@Composable
fun CalculatorDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var expression by remember { mutableStateOf(initialValue) }

    val resultText = remember(expression) {
        val parser = ExpressionParser(expression)
        val result = parser.parse()
        if (!result.isNaN() && !result.isInfinite()) {
            val formatted = String.format(Locale.US, "%.2f", result)
            if (formatted.endsWith(".00")) {
                formatted.substring(0, formatted.length - 3)
            } else {
                formatted
            }
        } else {
            "Error"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Calculator", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = expression.ifEmpty { "0" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "= $resultText",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        text = {
            val buttons = listOf(
                listOf("7", "8", "9", "÷"),
                listOf("4", "5", "6", "×"),
                listOf("1", "2", "3", "-"),
                listOf("C", "0", ".", "+")
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                buttons.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { char ->
                            Button(
                                onClick = {
                                    when (char) {
                                        "C" -> expression = ""
                                        "×" -> expression += "*"
                                        "÷" -> expression += "/"
                                        else -> expression += char
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (char in listOf("+", "-", "×", "÷")) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else if (char == "C") {
                                        MaterialTheme.colorScheme.errorContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    contentColor = if (char in listOf("+", "-", "×", "÷")) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else if (char == "C") {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(char, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            if (expression.isNotEmpty()) {
                                expression = expression.dropLast(1)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("⌫", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Button(
                        onClick = {
                            val parser = ExpressionParser(expression)
                            val res = parser.parse()
                            if (!res.isNaN() && !res.isInfinite()) {
                                val formatted = String.format(Locale.US, "%.2f", res)
                                expression = if (formatted.endsWith(".00")) {
                                    formatted.substring(0, formatted.length - 3)
                                } else {
                                    formatted
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("=", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parser = ExpressionParser(expression)
                    val res = parser.parse()
                    if (!res.isNaN() && !res.isInfinite()) {
                        val formatted = String.format(Locale.US, "%.2f", res)
                        onConfirm(formatted)
                    } else {
                        onConfirm(expression)
                    }
                    onDismiss()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}




