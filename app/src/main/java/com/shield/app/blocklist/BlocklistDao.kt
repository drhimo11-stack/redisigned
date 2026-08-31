package com.shield.app.blocklist

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlocklistDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: BlocklistItem): Long

    @Query("SELECT pattern FROM blocklist_items ORDER BY createdAt DESC")
    suspend fun getAllPatterns(): List<String>

    @Query("SELECT * FROM blocklist_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BlocklistItem>>

    @Query("DELETE FROM blocklist_items WHERE pattern = :pattern")
    suspend fun deleteByPattern(pattern: String)
}

@Dao
interface WhitelistDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: WhitelistItem): Long

    @Query("SELECT pattern FROM whitelist_items ORDER BY createdAt DESC")
    suspend fun getAllPatterns(): List<String>

    @Query("SELECT * FROM whitelist_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WhitelistItem>>

    @Query("DELETE FROM whitelist_items WHERE pattern = :pattern")
    suspend fun deleteByPattern(pattern: String)
}
