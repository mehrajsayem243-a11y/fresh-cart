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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ExpressivePullRefreshBox
import com.example.ui.components.ExpressiveSearchBar
import com.example.ui.components.GeminiSuggestionsDialog
import com.example.ui.components.PantryItemCard
import com.example.ui.components.PillButton
import com.example.ui.theme.CardShape
import com.example.ui.theme.PillShape
import com.example.ui.viewmodel.FreshCartViewModel
import com.example.ui.viewmodel.PantryFilter
import java.util.concurrent.TimeUnit

@Composable
fun PantryScreen(
    viewModel: FreshCartViewModel,
    onOpenAddDialog: () -> Unit,
    onOpenBarcodeScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pantryList by viewModel.pantryList.collectAsStateWithLifecycle()
    val searchQuery by viewModel.pantrySearchQuery.collectAsStateWithLifecycle()
    val filter by viewModel.pantryFilter.collectAsStateWithLifecycle()

    val isAnalyzing by viewModel.isAnalyzingPantry.collectAsStateWithLifecycle()
    val pantrySuggestions by viewModel.pantrySuggestions.collectAsStateWithLifecycle()
    val showSuggestionsDialog by viewModel.showSuggestionsDialog.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshingPantry.collectAsStateWithLifecycle()

    val now = System.currentTimeMillis()
    val threeDays = now + TimeUnit.DAYS.toMillis(3)
    val urgentItemsCount = remember(pantryList) {
        pantryList.count { it.expiryDate <= threeDays }
    }

    ExpressivePullRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshPantry() },
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
                onQueryChange = { viewModel.setPantrySearch(it) },
                placeholder = "Search pantry items...",
                testTag = "pantry_search_bar",
                onScanBarcodeClick = onOpenBarcodeScanner
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Gemini Pantry Assistant Banner
            Card(
                shape = CardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gemini_assistant_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = PillShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Gemini AI",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gemini Restock Assistant",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Analyze low stock & get smart grocery ideas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    PillButton(
                        text = "Analyze",
                        onClick = { viewModel.startPantryAnalysis() },
                        icon = Icons.Rounded.AutoAwesome,
                        testTag = "analyze_pantry_button",
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == PantryFilter.ALL,
                    onClick = { viewModel.setPantryFilter(PantryFilter.ALL) },
                    label = { Text("All (${pantryList.size})") },
                    shape = PillShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                FilterChip(
                    selected = filter == PantryFilter.EXPIRING_SOON,
                    onClick = { viewModel.setPantryFilter(PantryFilter.EXPIRING_SOON) },
                    label = { Text("⚠️ Urgent Expiry") },
                    shape = PillShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                )

                FilterChip(
                    selected = filter == PantryFilter.FRIDGE,
                    onClick = { viewModel.setPantryFilter(PantryFilter.FRIDGE) },
                    label = { Text("Fridge") },
                    shape = PillShape
                )

                FilterChip(
                    selected = filter == PantryFilter.PANTRY,
                    onClick = { viewModel.setPantryFilter(PantryFilter.PANTRY) },
                    label = { Text("Pantry Shelf") },
                    shape = PillShape
                )

                FilterChip(
                    selected = filter == PantryFilter.FREEZER,
                    onClick = { viewModel.setPantryFilter(PantryFilter.FREEZER) },
                    label = { Text("Freezer") },
                    shape = PillShape
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Urgent Alert Banner if any items need attention
            if (urgentItemsCount > 0 && filter != PantryFilter.EXPIRING_SOON) {
                Card(
                    shape = CardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$urgentItemsCount item(s) expiring soon!",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Use them up soon to prevent food waste.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            // Pantry List or Empty State
            if (pantryList.isEmpty()) {
                EmptyStateCard(
                    title = "Your Pantry is Empty",
                    description = "Track what you have at home! When you buy groceries, they are automatically added here with shelf-life timers.",
                    icon = Icons.Rounded.Kitchen,
                    buttonText = "Stock an Item",
                    onButtonClick = onOpenAddDialog
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 150.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = pantryList, key = { it.id }) { item ->
                        PantryItemCard(
                            item = item,
                            onConsume = { viewModel.consumePantryItem(item, 1.0) },
                            onRestockToGroceries = { viewModel.restockPantryItemToGroceries(item) },
                            onDelete = { viewModel.deletePantryItem(item) }
                        )
                    }
                }
            }
        }
    }

    if (showSuggestionsDialog) {
        GeminiSuggestionsDialog(
            isLoading = isAnalyzing,
            suggestions = pantrySuggestions,
            onToggleSelect = { viewModel.toggleSuggestionSelection(it) },
            onAddSelected = { viewModel.addSelectedSuggestionsToGroceries() },
            onDismiss = { viewModel.dismissSuggestionsDialog() }
        )
    }
}
