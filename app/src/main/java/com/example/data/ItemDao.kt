package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<Item>>
    
    @Query("SELECT * FROM items ORDER BY createdAt DESC")
    suspend fun getAllItemsSync(): List<Item>
    
    @Query("SELECT * FROM items WHERE type = :type ORDER BY createdAt DESC")
    fun getItemsByType(type: ItemType): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Int): Item?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item): Long

    @Update
    suspend fun updateItem(item: Item)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItemById(id: Int)
}
