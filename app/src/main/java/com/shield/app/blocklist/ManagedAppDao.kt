package com.shield.app.blocklist

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ManagedAppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ManagedAppItem)

    @Query("SELECT * FROM managed_apps ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<ManagedAppItem>>

    @Query("SELECT packageName FROM managed_apps WHERE blocked = 1")
    suspend fun getBlockedPackageNames(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM managed_apps WHERE packageName = :packageName)")
    suspend fun exists(packageName: String): Boolean

    @Query("UPDATE managed_apps SET blocked = :blocked WHERE packageName = :packageName")
    suspend fun setBlocked(packageName: String, blocked: Boolean)

    @Query("DELETE FROM managed_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
