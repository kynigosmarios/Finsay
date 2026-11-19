package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ExpenseDao
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.data.entity.RecurringExpense
import com.example.data.entity.SavingsGoal

@Database(
    entities = [
        Expense::class,
        FamilyMember::class,
        ExpenseCategory::class,
        RecurringExpense::class,
        SavingsGoal::class,
        Income::class
    ],
    version = 14,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recurring_expenses ADD COLUMN reminderHour INTEGER NOT NULL DEFAULT 9")
                db.execSQL("ALTER TABLE recurring_expenses ADD COLUMN reminderMinute INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE family_members ADD COLUMN linkedUserUid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE recurring_expenses ADD COLUMN anchorMonth INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Copy legacy database file if present
                try {
                    val appContext = context.applicationContext
                    val newDb = appContext.getDatabasePath("finsay_database")
                    if (!newDb.exists()) {
                        val legacyName = listOf("fintel_database", "oikonomos_database")
                            .firstOrNull { appContext.getDatabasePath(it).exists() }
                        if (legacyName != null) {
                            appContext.getDatabasePath(legacyName).copyTo(newDb, overwrite = true)
                            val oldWal = appContext.getDatabasePath("$legacyName-wal")
                            val newWal = appContext.getDatabasePath("finsay_database-wal")
                            if (oldWal.exists()) oldWal.copyTo(newWal, overwrite = true)
                            val oldShm = appContext.getDatabasePath("$legacyName-shm")
                            val newShm = appContext.getDatabasePath("finsay_database-shm")
                            if (oldShm.exists()) oldShm.copyTo(newShm, overwrite = true)
                        }
                    }
                } catch (ignored: Exception) {}

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finsay_database"
                )
                    .addMigrations(MIGRATION_12_13, MIGRATION_13_14)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
