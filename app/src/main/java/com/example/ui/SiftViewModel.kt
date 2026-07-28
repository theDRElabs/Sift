package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Item
import com.example.data.ItemRepository
import com.example.data.ItemType
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SiftViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ItemRepository
    
    private val _filter = MutableStateFlow<ItemType?>(null)
    val filter: StateFlow<ItemType?> = _filter

    val uiState: StateFlow<List<Item>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ItemRepository(database.itemDao())

        uiState = combine(repository.allItems, _filter) { items, currentFilter ->
            if (currentFilter == null) items else items.filter { it.type == currentFilter }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun setFilter(type: ItemType?) {
        _filter.value = type
    }

    suspend fun getItemById(id: Int): Item? {
        return repository.getItemById(id)
    }

    fun saveItem(id: Int, type: ItemType, content: String, dueAt: Long?, isDone: Boolean): kotlinx.coroutines.Job {
        return viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            if (id == 0) {
                // Insert
                val item = Item(type = type, content = content, dueAt = dueAt, isDone = isDone)
                val newId = repository.insertItem(item).toInt()
                if (type == ItemType.TODO && dueAt != null) {
                    NotificationHelper.scheduleTodoNotification(context, newId, content, dueAt)
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
                    isDone = isDone
                )
                repository.updateItem(item)
                
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
