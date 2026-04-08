package com.axis.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.axis.app.data.model.*

@Database(
    entities = [
        Transaction::class,
        Budget::class,
        SavingsGoal::class,
        ActivatedService::class,
        Category::class,
        CategorizationRule::class,
        SavingsAccount::class,
        AccountEntity::class,
        CategoryRuleEntity::class
    ],
    version = 8,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun activatedServiceDao(): ActivatedServiceDao
    abstract fun categoryDao(): CategoryDao
    abstract fun categorizationRuleDao(): CategorizationRuleDao
    abstract fun savingsAccountDao(): SavingsAccountDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryRuleDao(): CategoryRuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "axis_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Initial migration placeholder
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categorization_rules ADD COLUMN categoryName TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create savings_accounts table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `savings_accounts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `type` TEXT NOT NULL, 
                        `balance` REAL NOT NULL
                    )
                """.trimIndent())
                
                // Add columns to savings_goals
                db.execSQL("ALTER TABLE savings_goals ADD COLUMN savingsAccountId INTEGER")
                db.execSQL("ALTER TABLE savings_goals ADD COLUMN allocationPercentage REAL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `accounts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `type` TEXT NOT NULL, 
                        `isEnabled` INTEGER NOT NULL, 
                        `balance` REAL NOT NULL, 
                        `lastSyncedAt` INTEGER NOT NULL, 
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add accountId column
                db.execSQL("ALTER TABLE transactions ADD COLUMN accountId INTEGER NOT NULL DEFAULT 0")
                
                // 2. Rename timestamp to transactionDateTime
                // Note: RENAME COLUMN is supported in SQLite 3.25.0+ (Android 10+)
                // For maximum compatibility, we use the old-school approach if needed, 
                // but here we'll use the direct command as per the plan.
                db.execSQL("ALTER TABLE transactions RENAME COLUMN timestamp TO transactionDateTime")
                
                // 3. Add Index
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_account_datetime ON transactions(accountId, transactionDateTime DESC)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `category_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `keyword` TEXT NOT NULL, 
                        `category` TEXT NOT NULL, 
                        `isIncomeRule` INTEGER NOT NULL DEFAULT 0, 
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}

/**
 * Type converters for Room to handle enums.
 */
class Converters {
    @TypeConverter
    fun fromFundType(fundType: FundType): String = fundType.name

    @TypeConverter
    fun toFundType(value: String): FundType = FundType.valueOf(value)

    @TypeConverter
    fun fromCategoryType(categoryType: CategoryType): String = categoryType.name

    @TypeConverter
    fun toCategoryType(value: String): CategoryType = CategoryType.valueOf(value)

    @TypeConverter
    fun fromAccountType(accountType: AccountType): String = accountType.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)
}
