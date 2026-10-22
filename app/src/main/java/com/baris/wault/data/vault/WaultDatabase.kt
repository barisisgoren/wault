package com.baris.wault.data.vault

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.baris.wault.data.nfc.BankCardOverrideDao
import com.baris.wault.data.nfc.BankCardOverrideEntity

class WaultTypeConverters {
    @TypeConverter
    fun fromBarcodeFormat(format: WaultBarcodeFormat): String = format.name

    @TypeConverter
    fun toBarcodeFormat(value: String): WaultBarcodeFormat =
        WaultBarcodeFormat.fromNameOrDefault(value)
}

@Database(
    entities = [
        LoyaltyCardEntity::class,
        BankCardOverrideEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(WaultTypeConverters::class)
abstract class WaultDatabase : RoomDatabase() {

    abstract fun loyaltyCardDao(): LoyaltyCardDao

    abstract fun bankCardOverrideDao(): BankCardOverrideDao

    companion object {
        private const val DATABASE_NAME = "wault_vault.db"

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                runCatching {
                    db.execSQL("ALTER TABLE bank_card_overrides ADD COLUMN paymentNetwork TEXT")
                }
            }
        }

        @Volatile
        private var INSTANCE: WaultDatabase? = null

        fun getInstance(context: Context): WaultDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WaultDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
