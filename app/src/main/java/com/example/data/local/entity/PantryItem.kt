package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an item stored in the pantry, fridge, or freezer
 * with shelf-life tracking and expiration dates.
 */
@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val category: String = "Produce",
    val addedDate: Long = System.currentTimeMillis(),
    val expiryDate: Long = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000,
    val storageLocation: String = "Pantry", // Fridge, Freezer, Pantry
    val lowStockThreshold: Double = 1.0
)
