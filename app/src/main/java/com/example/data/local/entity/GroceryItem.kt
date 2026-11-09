package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an item in the user's grocery shopping list.
 */
@Entity(tableName = "grocery_items")
data class GroceryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val category: String = "Produce",
    val isBought: Boolean = false,
    val notes: String = "",
    val collaboratorName: String = "",
    val brand: String = "",
    val price: Double = 0.0,
    val priority: String = "Normal", // Low, Normal, Urgent
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
