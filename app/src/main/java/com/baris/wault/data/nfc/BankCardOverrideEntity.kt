package com.baris.wault.data.nfc

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

enum class BankPaymentNetwork(
    val code: String,
    val label: String
) {
    AUTO("AUTO", "Otomatik"),
    VISA("VISA", "VISA"),
    MASTERCARD("MASTERCARD", "Mastercard"),
    TROY("TROY", "TROY"),
    AMEX("AMEX", "Amex");

    companion object {
        fun fromCode(code: String?): BankPaymentNetwork {
            if (code.isNullOrBlank()) return AUTO
            return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) } ?: AUTO
        }
    }
}

@Entity(tableName = "bank_card_overrides")
data class BankCardOverrideEntity(
    @PrimaryKey
    val componentFlatten: String,
    val customTitle: String? = null,
    val cardNumberSuffix: String? = null,
    val customColorHex: Long? = null,
    val customImagePath: String? = null,
    val paymentNetwork: String? = null,
    val isPrimary: Boolean = false,
    val isHidden: Boolean = false,
    val sortOrder: Int = 0
)

@Dao
interface BankCardOverrideDao {

    @Query("SELECT * FROM bank_card_overrides ORDER BY sortOrder ASC")
    fun observeAllOverrides(): Flow<List<BankCardOverrideEntity>>

    @Query("SELECT * FROM bank_card_overrides ORDER BY sortOrder ASC")
    suspend fun getAllOverrides(): List<BankCardOverrideEntity>

    @Query("SELECT * FROM bank_card_overrides WHERE componentFlatten = :componentFlatten LIMIT 1")
    suspend fun getOverride(componentFlatten: String): BankCardOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverride(override: BankCardOverrideEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverrides(overrides: List<BankCardOverrideEntity>)

    @Query("UPDATE bank_card_overrides SET isPrimary = 0 WHERE componentFlatten != :componentFlatten")
    suspend fun clearOtherPrimaryFlags(componentFlatten: String)

    @Query("UPDATE bank_card_overrides SET isPrimary = 0")
    suspend fun clearAllPrimaryFlags()
}
