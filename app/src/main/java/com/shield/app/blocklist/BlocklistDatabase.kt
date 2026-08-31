package com.shield.app.blocklist

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BlocklistItem::class, WhitelistItem::class, ManagedAppItem::class],
    version = 2,
    exportSchema = false
)
abstract class BlocklistDatabase : RoomDatabase() {

    abstract fun blocklistDao(): BlocklistDao
    abstract fun whitelistDao(): WhitelistDao
    abstract fun managedAppDao(): ManagedAppDao

    companion object {
        @Volatile private var instance: BlocklistDatabase? = null

        // Adds the managed_apps table for version 2. Written explicitly
        // (rather than a destructive fallback) so upgrading never wipes a
        // user's existing custom blocklist/whitelist entries.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `managed_apps` (
                        `packageName` TEXT NOT NULL,
                        `appLabel` TEXT NOT NULL,
                        `blocked` INTEGER NOT NULL,
                        `autoDetected` INTEGER NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`packageName`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun get(context: Context): BlocklistDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BlocklistDatabase::class.java,
                    "shield_blocklist.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    // Safety net for the (unexpected) case a device is
                    // somehow missing a version this migration doesn't
                    // cover — only reached if addMigrations() above can't
                    // handle the jump, so normal upgrades keep their data.
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { instance = it }
            }
    }
}
