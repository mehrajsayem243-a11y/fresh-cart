package com.example.data.repository

import com.example.data.local.dao.AppSettingDao
import com.example.data.local.dao.CollaboratorDao
import com.example.data.local.dao.GroceryItemDao
import com.example.data.local.dao.PantryItemDao
import com.example.data.local.entity.AppSetting
import com.example.data.local.entity.Collaborator
import com.example.data.local.entity.GroceryItem
import com.example.data.local.entity.PantryItem
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class FreshCartRepository(
    private val groceryDao: GroceryItemDao,
    private val pantryDao: PantryItemDao,
    private val collaboratorDao: CollaboratorDao,
    private val appSettingDao: AppSettingDao? = null
) {
    val allGroceries: Flow<List<GroceryItem>> = groceryDao.getAllGroceries()
    val allPantryItems: Flow<List<PantryItem>> = pantryDao.getAllPantryItems()
    val allCollaborators: Flow<List<Collaborator>> = collaboratorDao.getAllCollaborators()

    // Settings Operations (Room Persistence)
    suspend fun getSetting(key: String): String? = appSettingDao?.getSetting(key)
    fun getSettingFlow(key: String): Flow<String?> = appSettingDao?.getSettingFlow(key) ?: kotlinx.coroutines.flow.emptyFlow()
    suspend fun saveSetting(key: String, value: String) {
        appSettingDao?.setSetting(AppSetting(key, value))
    }

    // Grocery Operations
    suspend fun addGrocery(item: GroceryItem): Long = groceryDao.insert(item)
    suspend fun updateGrocery(item: GroceryItem) = groceryDao.update(item)
    suspend fun deleteGrocery(item: GroceryItem) = groceryDao.delete(item)
    suspend fun deleteGroceryById(id: Long) = groceryDao.deleteById(id)
    suspend fun clearBoughtGroceries() = groceryDao.clearBought()

    suspend fun setGroceryBought(item: GroceryItem, isBought: Boolean, autoTransferToPantry: Boolean = true) {
        groceryDao.setBoughtStatus(item.id, isBought)
        if (isBought && autoTransferToPantry) {
            // Calculate estimated shelf life based on category
            val shelfLifeDays = getEstimatedShelfLifeDays(item.category)
            val expiry = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(shelfLifeDays.toLong())
            val location = getDefaultStorageLocation(item.category)

            val pantryItem = PantryItem(
                name = item.name,
                quantity = item.quantity,
                unit = item.unit,
                category = item.category,
                addedDate = System.currentTimeMillis(),
                expiryDate = expiry,
                storageLocation = location,
                lowStockThreshold = 1.0
            )
            pantryDao.insert(pantryItem)
        }
    }

    // Pantry Operations
    suspend fun addPantryItem(item: PantryItem): Long = pantryDao.insert(item)
    suspend fun updatePantryItem(item: PantryItem) = pantryDao.update(item)
    suspend fun deletePantryItem(item: PantryItem) = pantryDao.delete(item)
    suspend fun deletePantryItemById(id: Long) = pantryDao.deleteById(id)

    suspend fun consumePantryItem(item: PantryItem, amount: Double = 1.0): Boolean {
        val newQuantity = (item.quantity - amount).coerceAtLeast(0.0)
        if (newQuantity <= 0) {
            pantryDao.delete(item)
            return true // Item depleted
        } else {
            pantryDao.updateQuantity(item.id, newQuantity)
            return false
        }
    }

    suspend fun movePantryToGrocery(pantryItem: PantryItem) {
        val groceryItem = GroceryItem(
            name = pantryItem.name,
            quantity = if (pantryItem.quantity > 0) pantryItem.quantity else 1.0,
            unit = pantryItem.unit,
            category = pantryItem.category,
            isBought = false,
            notes = "Restock from pantry"
        )
        groceryDao.insert(groceryItem)
    }

    // Collaborators
    suspend fun addCollaborator(collaborator: Collaborator): Long = collaboratorDao.insert(collaborator)
    suspend fun updateCollaborator(collaborator: Collaborator) = collaboratorDao.update(collaborator)
    suspend fun deleteCollaborator(collaborator: Collaborator) = collaboratorDao.delete(collaborator)

    // Category smart defaults
    companion object {
        fun getEstimatedShelfLifeDays(category: String): Int {
            return when (category.lowercase()) {
                "dairy" -> 7
                "meat & seafood", "meat", "seafood" -> 3
                "produce", "fruits & vegetables", "fruits", "vegetables" -> 5
                "bakery", "bread" -> 4
                "pantry", "dry goods", "canned" -> 60
                "frozen" -> 90
                "beverages", "drinks" -> 14
                "snacks" -> 30
                else -> 7
            }
        }

        fun getDefaultStorageLocation(category: String): String {
            return when (category.lowercase()) {
                "dairy", "meat & seafood", "meat", "seafood" -> "Fridge"
                "produce", "fruits & vegetables" -> "Fridge"
                "frozen" -> "Freezer"
                else -> "Pantry"
            }
        }
    }
}
