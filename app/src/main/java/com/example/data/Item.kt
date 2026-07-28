package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ItemType {
    IDEA, INFO, TODO
}

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: ItemType,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val dueAt: Long? = null,
    val isDone: Boolean = false
)
