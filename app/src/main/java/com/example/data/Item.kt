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
    val isDone: Boolean = false,
    val importanceScore: Int = 5,
    val scoredAt: Long? = null,
    val dismissed: Boolean = false,
    val lastNudgedAt: Long? = null
) {
    val effectiveScore: Int
        get() {
            var score = importanceScore
            if (type == ItemType.TODO && dueAt != null && !isDone) {
                val now = System.currentTimeMillis()
                val diff = dueAt - now
                if (diff <= 24L * 60 * 60 * 1000) { // Within 24 hours
                    score += 3
                } else if (diff <= 7L * 24 * 60 * 60 * 1000) { // Within a week
                    score += 1
                }
            }
            return score.coerceIn(1, 10) // Keep it capped at 10 (or maybe it can exceed 10? The prompt says "produce the effective ranking score". I'll cap at 10 for neatness)
        }
}
