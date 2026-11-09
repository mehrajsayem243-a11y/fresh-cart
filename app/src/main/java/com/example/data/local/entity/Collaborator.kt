package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a household member or contact collaborating on shopping lists.
 */
@Entity(tableName = "collaborators")
data class Collaborator(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val contactInfo: String = "",
    val colorHex: String = "#0B57D0",
    val role: String = "Editor",
    val addedDate: Long = System.currentTimeMillis()
)
