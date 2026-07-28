package com.example.data

import kotlinx.coroutines.flow.Flow

class ItemRepository(private val itemDao: ItemDao) {
    val allItems: Flow<List<Item>> = itemDao.getAllItems()

    fun getItemsByType(type: ItemType): Flow<List<Item>> = itemDao.getItemsByType(type)

    suspend fun getItemById(id: Int): Item? = itemDao.getItemById(id)

    suspend fun insertItem(item: Item): Long = itemDao.insertItem(item)

    suspend fun updateItem(item: Item) = itemDao.updateItem(item)

    suspend fun deleteItemById(id: Int) = itemDao.deleteItemById(id)
}
