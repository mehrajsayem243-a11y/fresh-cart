package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiPantryService
import com.example.data.ai.PantrySuggestion
import com.example.data.local.AppDatabase
import com.example.data.local.entity.Collaborator
import com.example.data.local.entity.GroceryItem
import com.example.data.local.entity.PantryItem
import com.example.data.repository.FreshCartRepository
import com.example.ui.theme.AppColorPalette
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.net.Uri
import com.example.data.notification.PantryNotificationManager
import com.example.ui.theme.AppFontPreset
import com.example.ui.theme.CustomFontManager
import androidx.compose.ui.text.font.FontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class NavTab {
    GROCERIES,
    PANTRY,
    COLLABORATORS
}

enum class PantryFilter {
    ALL,
    EXPIRING_SOON,
    FRIDGE,
    PANTRY,
    FREEZER
}

class FreshCartViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = FreshCartRepository(
        database.groceryItemDao(),
        database.pantryItemDao(),
        database.collaboratorDao(),
        database.appSettingDao()
    )
    private val geminiPantryService = GeminiPantryService()

    private val prefs = application.getSharedPreferences("freshcart_prefs", Context.MODE_PRIVATE)

    // Gemini Pantry Analysis State
    private val _isAnalyzingPantry = MutableStateFlow(false)
    val isAnalyzingPantry: StateFlow<Boolean> = _isAnalyzingPantry.asStateFlow()

    private val _pantrySuggestions = MutableStateFlow<List<PantrySuggestion>>(emptyList())
    val pantrySuggestions: StateFlow<List<PantrySuggestion>> = _pantrySuggestions.asStateFlow()

    private val _showSuggestionsDialog = MutableStateFlow(false)
    val showSuggestionsDialog: StateFlow<Boolean> = _showSuggestionsDialog.asStateFlow()

    // Pull-to-Refresh State
    private val _isRefreshingGroceries = MutableStateFlow(false)
    val isRefreshingGroceries: StateFlow<Boolean> = _isRefreshingGroceries.asStateFlow()

    private val _isRefreshingPantry = MutableStateFlow(false)
    val isRefreshingPantry: StateFlow<Boolean> = _isRefreshingPantry.asStateFlow()

    // Current Navigation Tab
    private val _currentTab = MutableStateFlow(NavTab.GROCERIES)
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    // Theme & Color Customization (Persisted in Room)
    private val _themeMode = MutableStateFlow(
        try {
            prefs.getString("theme_mode", null)?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _colorPalette = MutableStateFlow(
        try {
            prefs.getString("color_palette", null)?.let { AppColorPalette.valueOf(it) } ?: AppColorPalette.DYNAMIC
        } catch (_: Exception) {
            AppColorPalette.DYNAMIC
        }
    )
    val colorPalette: StateFlow<AppColorPalette> = _colorPalette.asStateFlow()

    init {
        // Load and observe Room persistent settings for colors and theme
        viewModelScope.launch {
            repository.getSettingFlow("color_palette").collect { savedPaletteName ->
                if (!savedPaletteName.isNullOrBlank()) {
                    try {
                        val palette = AppColorPalette.valueOf(savedPaletteName)
                        if (_colorPalette.value != palette) {
                            _colorPalette.value = palette
                        }
                    } catch (_: Exception) {}
                }
            }
        }
        viewModelScope.launch {
            repository.getSettingFlow("theme_mode").collect { savedThemeName ->
                if (!savedThemeName.isNullOrBlank()) {
                    try {
                        val mode = ThemeMode.valueOf(savedThemeName)
                        if (_themeMode.value != mode) {
                            _themeMode.value = mode
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    // Font Customization
    private val _fontPreset = MutableStateFlow(
        if (CustomFontManager.hasImportedFont(application)) AppFontPreset.IMPORTED else AppFontPreset.SYSTEM
    )
    val fontPreset: StateFlow<AppFontPreset> = _fontPreset.asStateFlow()

    private val _fontFamily = MutableStateFlow(
        CustomFontManager.resolveFontFamily(application, _fontPreset.value)
    )
    val fontFamily: StateFlow<FontFamily> = _fontFamily.asStateFlow()

    // Grocery Search & Category Filter
    private val _grocerySearchQuery = MutableStateFlow("")
    val grocerySearchQuery: StateFlow<String> = _grocerySearchQuery.asStateFlow()

    private val _groceryCategoryFilter = MutableStateFlow("All")
    val groceryCategoryFilter: StateFlow<String> = _groceryCategoryFilter.asStateFlow()

    // Pantry Search & Expiry/Location Filter
    private val _pantrySearchQuery = MutableStateFlow("")
    val pantrySearchQuery: StateFlow<String> = _pantrySearchQuery.asStateFlow()

    private val _pantryFilter = MutableStateFlow(PantryFilter.ALL)
    val pantryFilter: StateFlow<PantryFilter> = _pantryFilter.asStateFlow()

    // UI Feedback Event
    private val _snackBarEvent = MutableSharedFlow<String>()
    val snackBarEvent: SharedFlow<String> = _snackBarEvent.asSharedFlow()

    // Groceries List Flow with real-time search & filter
    val groceryList: StateFlow<List<GroceryItem>> = combine(
        repository.allGroceries,
        _grocerySearchQuery,
        _groceryCategoryFilter
    ) { items, query, category ->
        items.filter { item ->
            val matchesQuery = query.isBlank() || item.name.contains(query, ignoreCase = true) ||
                    item.notes.contains(query, ignoreCase = true)
            val matchesCategory = category == "All" || item.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pantry List Flow with real-time search & filter
    val pantryList: StateFlow<List<PantryItem>> = combine(
        repository.allPantryItems,
        _pantrySearchQuery,
        _pantryFilter
    ) { items, query, filter ->
        val now = System.currentTimeMillis()
        val threeDaysLater = now + TimeUnit.DAYS.toMillis(3)

        items.filter { item ->
            val matchesQuery = query.isBlank() || item.name.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                PantryFilter.ALL -> true
                PantryFilter.EXPIRING_SOON -> item.expiryDate <= threeDaysLater
                PantryFilter.FRIDGE -> item.storageLocation.equals("Fridge", ignoreCase = true)
                PantryFilter.PANTRY -> item.storageLocation.equals("Pantry", ignoreCase = true)
                PantryFilter.FREEZER -> item.storageLocation.equals("Freezer", ignoreCase = true)
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Collaborators
    val collaborators: StateFlow<List<Collaborator>> = repository.allCollaborators
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: NavTab) {
        _currentTab.value = tab
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
        viewModelScope.launch {
            repository.saveSetting("theme_mode", mode.name)
        }
    }

    fun setColorPalette(palette: AppColorPalette) {
        _colorPalette.value = palette
        prefs.edit().putString("color_palette", palette.name).apply()
        viewModelScope.launch {
            repository.saveSetting("color_palette", palette.name)
            _snackBarEvent.emit("Applied ${palette.displayName} palette")
        }
    }

    fun setGrocerySearch(query: String) {
        _grocerySearchQuery.value = query
    }

    fun setGroceryCategory(category: String) {
        _groceryCategoryFilter.value = category
    }

    fun setPantrySearch(query: String) {
        _pantrySearchQuery.value = query
    }

    fun setPantryFilter(filter: PantryFilter) {
        _pantryFilter.value = filter
    }

    // Grocery Actions
    fun addGroceryItem(
        name: String,
        quantity: Double,
        unit: String,
        category: String,
        notes: String = "",
        collaborator: String = "",
        brand: String = "",
        price: Double = 0.0,
        priority: String = "Normal",
        isFavorite: Boolean = false
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val item = GroceryItem(
                name = name.trim(),
                quantity = if (quantity <= 0) 1.0 else quantity,
                unit = unit.ifBlank { "pcs" },
                category = category.ifBlank { "Produce" },
                notes = notes.trim(),
                collaboratorName = collaborator.trim(),
                brand = brand.trim(),
                price = price.coerceAtLeast(0.0),
                priority = priority.ifBlank { "Normal" },
                isFavorite = isFavorite
            )
            repository.addGrocery(item)
            _snackBarEvent.emit("Added ${item.name} to grocery list")
        }
    }

    fun toggleGroceryFavorite(item: GroceryItem) {
        viewModelScope.launch {
            repository.addGrocery(item.copy(isFavorite = !item.isFavorite))
        }
    }

    fun toggleGroceryBought(item: GroceryItem, autoMoveToPantry: Boolean = true) {
        viewModelScope.launch {
            val newBoughtState = !item.isBought
            repository.setGroceryBought(item, newBoughtState, autoMoveToPantry)
            if (newBoughtState && autoMoveToPantry) {
                _snackBarEvent.emit("Bought ${item.name}! Added to Pantry inventory")
            }
        }
    }

    fun deleteGrocery(item: GroceryItem) {
        viewModelScope.launch {
            repository.deleteGrocery(item)
            _snackBarEvent.emit("Removed ${item.name}")
        }
    }

    fun clearBoughtGroceries() {
        viewModelScope.launch {
            repository.clearBoughtGroceries()
            _snackBarEvent.emit("Cleared completed items")
        }
    }

    // Pantry Actions
    fun addPantryItem(
        name: String,
        quantity: Double,
        unit: String,
        category: String,
        expiryDays: Int,
        location: String
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val expiry = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(expiryDays.toLong().coerceAtLeast(1))
            val item = PantryItem(
                name = name.trim(),
                quantity = if (quantity <= 0) 1.0 else quantity,
                unit = unit.ifBlank { "pcs" },
                category = category.ifBlank { "Produce" },
                addedDate = System.currentTimeMillis(),
                expiryDate = expiry,
                storageLocation = location
            )
            repository.addPantryItem(item)
            _snackBarEvent.emit("Added ${item.name} to $location")
        }
    }

    fun consumePantryItem(item: PantryItem, amount: Double = 1.0) {
        viewModelScope.launch {
            val depleted = repository.consumePantryItem(item, amount)
            if (depleted) {
                _snackBarEvent.emit("${item.name} finished! Tap to add to Groceries")
            }
        }
    }

    fun restockPantryItemToGroceries(item: PantryItem) {
        viewModelScope.launch {
            repository.movePantryToGrocery(item)
            _snackBarEvent.emit("Added ${item.name} back to grocery list")
        }
    }

    fun deletePantryItem(item: PantryItem) {
        viewModelScope.launch {
            repository.deletePantryItem(item)
            _snackBarEvent.emit("Removed ${item.name} from pantry")
        }
    }

    // Collaborators
    fun addCollaborator(name: String, contactInfo: String, role: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val collaborator = Collaborator(
                name = name.trim(),
                contactInfo = contactInfo.trim(),
                role = role.ifBlank { "Editor" },
                colorHex = colorHex
            )
            repository.addCollaborator(collaborator)
            _snackBarEvent.emit("Added $name as $role")
        }
    }

    fun deleteCollaborator(collaborator: Collaborator) {
        viewModelScope.launch {
            repository.deleteCollaborator(collaborator)
            _snackBarEvent.emit("Removed collaborator ${collaborator.name}")
        }
    }

    // Sharing & Export
    fun shareGroceryList(context: Context) {
        val items = groceryList.value
        val sb = StringBuilder()
        sb.append("🛒 FreshCart Grocery List\n")
        sb.append("Generated on ").append(SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())).append("\n\n")

        val pending = items.filter { !it.isBought }
        val completed = items.filter { it.isBought }

        if (pending.isNotEmpty()) {
            sb.append("TO BUY:\n")
            pending.forEach {
                sb.append("• [ ] ${it.name} - ${formatQuantity(it.quantity)} ${it.unit} (${it.category})")
                if (it.collaboratorName.isNotBlank()) sb.append(" [@${it.collaboratorName}]")
                if (it.notes.isNotBlank()) sb.append(" (${it.notes})")
                sb.append("\n")
            }
        }

        if (completed.isNotEmpty()) {
            sb.append("\nBOUGHT:\n")
            completed.forEach {
                sb.append("• [✓] ${it.name} - ${formatQuantity(it.quantity)} ${it.unit}\n")
            }
        }

        sb.append("\n--- Shared via FreshCart Groceries ---")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            putExtra(Intent.EXTRA_TITLE, "FreshCart Grocery List")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Grocery List via")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    // Import parsed grocery items from text
    fun importGroceriesFromText(rawText: String) {
        if (rawText.isBlank()) return
        viewModelScope.launch {
            val lines = rawText.split("\n", ",")
            var count = 0
            for (line in lines) {
                var clean = line.trim()
                if (clean.isBlank()) continue
                // Strip common list bullet prefixes like •, -, *, [ ], [x]
                clean = clean.replace(Regex("^[-*•]\\s*(\\[[ xX✓]?\\])?\\s*"), "").trim()
                if (clean.isBlank() || clean.startsWith("TO BUY:") || clean.startsWith("BOUGHT:") || clean.startsWith("🛒")) continue

                // Check for quantity in parentheses or hyphen
                val name = clean.split("-", "(", "@").firstOrNull()?.trim() ?: clean
                if (name.isNotBlank()) {
                    val item = GroceryItem(
                        name = name,
                        quantity = 1.0,
                        unit = "pcs",
                        category = detectCategory(name)
                    )
                    repository.addGrocery(item)
                    count++
                }
            }
            if (count > 0) {
                _snackBarEvent.emit("Imported $count items successfully")
            }
        }
    }

    private fun detectCategory(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("milk") || lower.contains("cheese") || lower.contains("yogurt") ||
                    lower.contains("butter") || lower.contains("cream") -> "Dairy"
            lower.contains("apple") || lower.contains("banana") || lower.contains("berry") ||
                    lower.contains("tomato") || lower.contains("onion") || lower.contains("avocado") ||
                    lower.contains("salad") || lower.contains("spinach") || lower.contains("lettuce") ||
                    lower.contains("potato") || lower.contains("carrot") -> "Produce"
            lower.contains("bread") || lower.contains("bagel") || lower.contains("croissant") ||
                    lower.contains("sourdough") || lower.contains("muffin") -> "Bakery"
            lower.contains("chicken") || lower.contains("beef") || lower.contains("pork") ||
                    lower.contains("salmon") || lower.contains("tuna") || lower.contains("shrimp") -> "Meat & Seafood"
            lower.contains("rice") || lower.contains("pasta") || lower.contains("cereal") ||
                    lower.contains("oil") || lower.contains("flour") || lower.contains("beans") -> "Pantry"
            lower.contains("pizza") || lower.contains("ice cream") || lower.contains("frozen") -> "Frozen"
            lower.contains("coffee") || lower.contains("tea") || lower.contains("juice") ||
                    lower.contains("soda") || lower.contains("water") -> "Beverages"
            else -> "Other"
        }
    }

    private fun formatQuantity(qty: Double): String {
        return if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()
    }

    // Gemini-Powered Pantry Analysis Methods
    fun startPantryAnalysis() {
        viewModelScope.launch {
            _isAnalyzingPantry.value = true
            _showSuggestionsDialog.value = true
            try {
                val currentPantry = repository.allPantryItems.first()
                val currentGroceries = repository.allGroceries.first()
                val suggestions = geminiPantryService.analyzePantryUsage(currentPantry, currentGroceries)
                _pantrySuggestions.value = suggestions
            } catch (e: Exception) {
                _snackBarEvent.emit("Analysis failed: ${e.localizedMessage}")
            } finally {
                _isAnalyzingPantry.value = false
            }
        }
    }

    fun toggleSuggestionSelection(index: Int) {
        val current = _pantrySuggestions.value.toMutableList()
        if (index in current.indices) {
            val item = current[index]
            current[index] = item.copy(isSelected = !item.isSelected)
            _pantrySuggestions.value = current
        }
    }

    fun addSelectedSuggestionsToGroceries() {
        val selected = _pantrySuggestions.value.filter { it.isSelected }
        if (selected.isEmpty()) {
            _showSuggestionsDialog.value = false
            return
        }
        viewModelScope.launch {
            for (suggestion in selected) {
                val item = GroceryItem(
                    name = suggestion.name,
                    quantity = suggestion.quantity,
                    unit = suggestion.unit,
                    category = suggestion.category,
                    notes = suggestion.reason
                )
                repository.addGrocery(item)
            }
            _showSuggestionsDialog.value = false
            _snackBarEvent.emit("Added ${selected.size} Gemini suggestion(s) to grocery list")
        }
    }

    fun dismissSuggestionsDialog() {
        _showSuggestionsDialog.value = false
    }

    // Pull to Refresh Actions
    fun refreshGroceries() {
        viewModelScope.launch {
            _isRefreshingGroceries.value = true
            delay(750) // Allow shape morph animation to play smoothly
            _snackBarEvent.emit("Groceries list up to date")
            _isRefreshingGroceries.value = false
        }
    }

    fun refreshPantry() {
        viewModelScope.launch {
            _isRefreshingPantry.value = true
            delay(750) // Allow shape morph animation to play smoothly
            checkExpiringItemsNotification(getApplication())
            _snackBarEvent.emit("Pantry stock & expiry timers updated")
            _isRefreshingPantry.value = false
        }
    }

    // Font Customization Methods
    fun setFontPreset(preset: AppFontPreset) {
        _fontPreset.value = preset
        _fontFamily.value = CustomFontManager.resolveFontFamily(getApplication(), preset)
        viewModelScope.launch {
            _snackBarEvent.emit("Applied ${preset.displayName} font")
        }
    }

    fun importCustomFont(uri: Uri, fileName: String?): Boolean {
        val success = CustomFontManager.importFontFromUri(getApplication(), uri, fileName)
        if (success) {
            _fontPreset.value = AppFontPreset.IMPORTED
            _fontFamily.value = CustomFontManager.resolveFontFamily(getApplication(), AppFontPreset.IMPORTED)
            viewModelScope.launch {
                _snackBarEvent.emit("Imported and applied ${fileName ?: "custom font"} successfully!")
            }
        } else {
            viewModelScope.launch {
                _snackBarEvent.emit("Could not load font file. Please provide a valid TTF or OTF font.")
            }
        }
        return success
    }

    // Local Notification Methods
    fun sendTestNotification(context: Context) {
        val sent = PantryNotificationManager.sendTestNotification(context)
        viewModelScope.launch {
            if (sent) {
                _snackBarEvent.emit("Test expiration alert sent to notification drawer")
            } else {
                _snackBarEvent.emit("Please grant notification permission in system settings")
            }
        }
    }

    fun checkExpiringItemsNotification(context: Context) {
        viewModelScope.launch {
            val items = repository.allPantryItems.first()
            PantryNotificationManager.checkAndNotifyExpiringItems(context, items)
        }
    }
}
