package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Item
import com.example.data.ItemRepository
import com.example.data.ItemType
import com.example.data.OpenAIService
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder { RECENT, IMPORTANT }

class SiftViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ItemRepository
    
    private val prefs = application.getSharedPreferences("sift_prefs", Context.MODE_PRIVATE)
    
    private val _filter = MutableStateFlow<ItemType?>(null)
    val filter: StateFlow<ItemType?> = _filter

    private val _sortOrder = MutableStateFlow(
        SortOrder.valueOf(prefs.getString("sort_order", SortOrder.RECENT.name) ?: SortOrder.RECENT.name)
    )
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    val uiState: StateFlow<List<Item>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ItemRepository(database.itemDao())

        uiState = combine(repository.allItems, _filter, _sortOrder) { items, currentFilter, currentSort ->
            var filtered = if (currentFilter == null) items else items.filter { it.type == currentFilter }
            if (currentSort == SortOrder.IMPORTANT) {
                filtered = filtered.sortedWith(compareByDescending<Item> { it.effectiveScore }.thenByDescending { it.createdAt })
            } else {
                filtered = filtered.sortedByDescending { it.createdAt } // Ensure Recent is sorted by createdAt
            }
            filtered
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun setFilter(type: ItemType?) {
        _filter.value = type
    }
    
    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        prefs.edit().putString("sort_order", order.name).apply()
    }

    suspend fun getItemById(id: Int): Item? {
        return repository.getItemById(id)
    }

    fun saveItem(id: Int, type: ItemType, content: String, dueAt: Long?, isDone: Boolean): kotlinx.coroutines.Job {
        return viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            
            // Fast save with default score, then update score in background
            if (id == 0) {
                // Insert
                val initialItem = Item(type = type, content = content, dueAt = dueAt, isDone = isDone)
                val newId = repository.insertItem(initialItem).toInt()
                if (type == ItemType.TODO && dueAt != null) {
                    NotificationHelper.scheduleTodoNotification(context, newId, content, dueAt)
                }
                
                // Fetch score in background
                launch {
                    val score = OpenAIService.scoreItem(context, content, type, dueAt)
                    val scoredAt = if (score != 5) System.currentTimeMillis() else null
                    repository.getItemById(newId)?.let { current ->
                        repository.updateItem(current.copy(importanceScore = score, scoredAt = scoredAt))
                    }
                }
            } else {
                // Update
                val existingItem = repository.getItemById(id)
                val item = Item(
                    id = id,
                    type = type,
                    content = content,
                    createdAt = existingItem?.createdAt ?: System.currentTimeMillis(),
                    dueAt = dueAt,
                    isDone = isDone,
                    importanceScore = existingItem?.importanceScore ?: 5,
                    scoredAt = existingItem?.scoredAt,
                    dismissed = existingItem?.dismissed ?: false,
                    lastNudgedAt = existingItem?.lastNudgedAt
                )
                repository.updateItem(item)
                
                // Re-score if content or due date changed
                if (existingItem?.content != content || existingItem?.dueAt != dueAt) {
                     launch {
                        val score = OpenAIService.scoreItem(context, content, type, dueAt)
                        val scoredAt = if (score != 5) System.currentTimeMillis() else null
                        repository.getItemById(id)?.let { current ->
                            repository.updateItem(current.copy(importanceScore = score, scoredAt = scoredAt))
                        }
                    }
                }
                
                // Update notification
                if (type == ItemType.TODO && dueAt != null && !isDone) {
                    NotificationHelper.scheduleTodoNotification(context, id, content, dueAt)
                } else {
                    NotificationHelper.cancelTodoNotification(context, id)
                }
            }
        }
    }

    fun toggleDone(item: Item) {
        viewModelScope.launch {
            val updated = item.copy(isDone = !item.isDone)
            repository.updateItem(updated)
            
            val context = getApplication<Application>().applicationContext
            if (updated.isDone) {
                NotificationHelper.cancelTodoNotification(context, item.id)
            } else if (updated.dueAt != null) {
                NotificationHelper.scheduleTodoNotification(context, item.id, item.content, updated.dueAt)
            }
        }
    }
    
    fun dismissItem(item: Item) {
        viewModelScope.launch {
            repository.updateItem(item.copy(dismissed = true))
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            repository.deleteItemById(item.id)
            val context = getApplication<Application>().applicationContext
            if (item.type == ItemType.TODO) {
                NotificationHelper.cancelTodoNotification(context, item.id)
            }
        }
    }
}
