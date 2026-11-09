package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddCollaboratorDialog
import com.example.ui.components.AddGroceryDialog
import com.example.ui.components.AddPantryDialog
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.ExpressiveFloatingNavBar
import com.example.ui.components.pressScale
import com.example.ui.screens.CollaboratorsScreen
import com.example.ui.screens.GroceriesScreen
import com.example.ui.screens.PantryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.FreshCartTheme
import com.example.ui.viewmodel.FreshCartViewModel
import com.example.ui.viewmodel.NavTab
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    private val viewModel: FreshCartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val colorPalette by viewModel.colorPalette.collectAsStateWithLifecycle()
            val fontFamily by viewModel.fontFamily.collectAsStateWithLifecycle()

            FreshCartTheme(
                themeMode = themeMode,
                colorPalette = colorPalette,
                fontFamily = fontFamily
            ) {
                FreshCartApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreshCartApp(viewModel: FreshCartViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val collaborators by viewModel.collaborators.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val fabInteractionSource = remember { MutableInteractionSource() }

    var showAddGroceryDialog by remember { mutableStateOf(false) }
    var showAddPantryDialog by remember { mutableStateOf(false) }
    var showAddCollaboratorDialog by remember { mutableStateOf(false) }
    var showBarcodeScannerDialog by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    // System Back Handler: return from settings or return to Groceries tab
    BackHandler(enabled = isSettingsOpen || currentTab != NavTab.GROCERIES) {
        if (isSettingsOpen) {
            isSettingsOpen = false
        } else {
            viewModel.selectTab(NavTab.GROCERIES)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.checkExpiringItemsNotification(context)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.checkExpiringItemsNotification(context)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackBarEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (isSettingsOpen) {
        SettingsScreen(
            viewModel = viewModel,
            onNavigateBack = { isSettingsOpen = false }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.ShoppingCart,
                                            contentDescription = "FreshCart Logo",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "FreshCart",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = when (currentTab) {
                                            NavTab.GROCERIES -> "Grocery Checklist"
                                            NavTab.PANTRY -> "Pantry & Expiration"
                                            NavTab.COLLABORATORS -> "Contact Sharing"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        actions = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                // Scanner icon with circle shape
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    IconButton(
                                        onClick = { showBarcodeScannerDialog = true },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("top_bar_scan_barcode")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.QrCodeScanner,
                                            contentDescription = "Scan barcode",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Share icon with circle shape
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.shareGroceryList(context) },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("top_bar_share")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Share,
                                            contentDescription = "Share",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Settings icon with circle shape
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    IconButton(
                                        onClick = { isSettingsOpen = true },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("top_bar_settings")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Settings,
                                            contentDescription = "Settings",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { innerPadding ->
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        val springSpec = spring<Float>(
                            dampingRatio = 0.78f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                        if (targetState.ordinal > initialState.ordinal) {
                            (slideInHorizontally(animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)) { width -> width } + fadeIn(animationSpec = springSpec))
                                .togetherWith(slideOutHorizontally(animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)) { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally(animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)) { width -> -width } + fadeIn(animationSpec = springSpec))
                                .togetherWith(slideOutHorizontally(animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)) { width -> width } + fadeOut())
                        }
                    },
                    label = "tabTransition",
                    modifier = Modifier.padding(innerPadding)
                ) { targetTab ->
                    when (targetTab) {
                        NavTab.GROCERIES -> GroceriesScreen(
                            viewModel = viewModel,
                            onOpenAddDialog = { showAddGroceryDialog = true },
                            onOpenBarcodeScanner = { showBarcodeScannerDialog = true }
                        )
                        NavTab.PANTRY -> PantryScreen(
                            viewModel = viewModel,
                            onOpenAddDialog = { showAddPantryDialog = true },
                            onOpenBarcodeScanner = { showBarcodeScannerDialog = true }
                        )
                        NavTab.COLLABORATORS -> CollaboratorsScreen(
                            viewModel = viewModel,
                            onOpenAddDialog = { showAddCollaboratorDialog = true }
                        )
                    }
                }
            }

            // Bottom Controls Layer: Standalone XL 24dp Rounded Rectangle FAB 9dp above Floating Bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.End
            ) {
                // XL 24dp rounded rectangle FAB (88dp size)
                Surface(
                    modifier = Modifier
                        .padding(end = 24.dp)
                        .size(88.dp)
                        .pressScale(fabInteractionSource)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(interactionSource = fabInteractionSource, indication = null) {
                            when (currentTab) {
                                NavTab.GROCERIES -> showAddGroceryDialog = true
                                NavTab.PANTRY -> showAddPantryDialog = true
                                NavTab.COLLABORATORS -> showAddCollaboratorDialog = true
                            }
                        }
                        .testTag("corner_fab_add_button"),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 10.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = when (currentTab) {
                                NavTab.GROCERIES -> "Add Grocery Item"
                                NavTab.PANTRY -> "Stock Pantry Item"
                                NavTab.COLLABORATORS -> "Add Collaborator"
                            },
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                // Exactly 9dp distance between FAB and the floating navigation bar
                Spacer(modifier = Modifier.height(9.dp))

                // 3-Tab Floating Pill Navigation Bar
                ExpressiveFloatingNavBar(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.selectTab(it) },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }

    if (showAddGroceryDialog) {
        AddGroceryDialog(
            collaborators = collaborators,
            onDismiss = { showAddGroceryDialog = false },
            onConfirm = { name, qty, unit, cat, notes, collab, brand, price, priority, isFavorite ->
                viewModel.addGroceryItem(
                    name = name,
                    quantity = qty,
                    unit = unit,
                    category = cat,
                    notes = notes,
                    collaborator = collab,
                    brand = brand,
                    price = price,
                    priority = priority,
                    isFavorite = isFavorite
                )
            }
        )
    }

    if (showAddPantryDialog) {
        AddPantryDialog(
            onDismiss = { showAddPantryDialog = false },
            onConfirm = { name, qty, unit, cat, days, loc ->
                viewModel.addPantryItem(name, qty, unit, cat, days, loc)
            }
        )
    }

    if (showAddCollaboratorDialog) {
        AddCollaboratorDialog(
            onDismiss = { showAddCollaboratorDialog = false },
            onConfirm = { name, contact, role, color ->
                viewModel.addCollaborator(name, contact, role, color)
            }
        )
    }

    if (showBarcodeScannerDialog) {
        BarcodeScannerDialog(
            onDismiss = { showBarcodeScannerDialog = false },
            onAddGrocery = { name, qty, unit, cat, notes ->
                viewModel.addGroceryItem(name, qty, unit, cat, notes)
            },
            onAddPantry = { name, qty, unit, cat, days, loc ->
                viewModel.addPantryItem(name, qty, unit, cat, days, loc)
            },
            onImportChecklist = { rawText ->
                viewModel.importGroceriesFromText(rawText)
            }
        )
    }
}
