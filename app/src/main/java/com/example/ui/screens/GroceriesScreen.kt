package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ExpressivePullRefreshBox
import com.example.ui.components.ExpressiveSearchBar
import com.example.ui.components.GroceryItemCard
import com.example.ui.components.ImportListDialog
import com.example.ui.components.groceryCategories
import com.example.ui.theme.PillShape
import com.example.ui.viewmodel.FreshCartViewModel

@Composable
fun GroceriesScreen(
    viewModel: FreshCartViewModel,
    onOpenAddDialog: () -> Unit,
    onOpenBarcodeScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val groceryList by viewModel.groceryList.collectAsStateWithLifecycle()
    val searchQuery by viewModel.grocerySearchQuery.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.groceryCategoryFilter.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshingGroceries.collectAsStateWithLifecycle()

    var showImportDialog by remember { mutableStateOf(false) }

    val pendingCount = groceryList.count { !it.isBought }
    val completedCount = groceryList.count { it.isBought }

    ExpressivePullRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshGroceries() },
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            ExpressiveSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setGrocerySearch(it) },
                placeholder = "Search groceries or notes...",
                onScanBarcodeClick = onOpenBarcodeScanner
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = categoryFilter == "All",
                    onClick = { viewModel.setGroceryCategory("All") },
                    label = { Text("All (${groceryList.size})") },
                    shape = PillShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                groceryCategories.forEach { cat ->
                    FilterChip(
                        selected = categoryFilter == cat,
                        onClick = { viewModel.setGroceryCategory(cat) },
                        label = { Text(cat) },
                        shape = PillShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary & Actions Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$pendingCount to buy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (completedCount > 0) {
                        Text(
                            text = " • $completedCount bought",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.shareGroceryList(context) },
                        modifier = Modifier.testTag("share_list_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share list",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.testTag("import_list_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileDownload,
                            contentDescription = "Import items",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (completedCount > 0) {
                        IconButton(
                            onClick = { viewModel.clearBoughtGroceries() },
                            modifier = Modifier.testTag("clear_completed_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = "Clear completed",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }



            Spacer(modifier = Modifier.height(8.dp))

            // Main List or Empty State
            if (groceryList.isEmpty()) {
                EmptyStateCard(
                    title = "Your Grocery Cart is Empty",
                    description = "Add groceries to plan your shopping trip. When you check them off, they'll automatically move to your Pantry!",
                    icon = Icons.Rounded.ShoppingCart,
                    buttonText = "Add First Item",
                    onButtonClick = onOpenAddDialog
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 150.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = groceryList, key = { it.id }) { item ->
                        GroceryItemCard(
                            item = item,
                            onToggleBought = { bought ->
                                viewModel.toggleGroceryBought(item, autoMoveToPantry = true)
                            },
                            onDelete = { viewModel.deleteGrocery(item) },
                            onToggleFavorite = { viewModel.toggleGroceryFavorite(item) }
                        )
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        ImportListDialog(
            onDismiss = { showImportDialog = false },
            onConfirm = { rawText ->
                viewModel.importGroceriesFromText(rawText)
            }
        )
    }
}
