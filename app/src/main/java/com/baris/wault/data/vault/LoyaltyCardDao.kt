package com.baris.wault.data.vault

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LoyaltyCardDao {

    @Query("SELECT * FROM loyalty_cards ORDER BY sortOrder ASC, createdAt DESC")
    fun observeAllCards(): Flow<List<LoyaltyCardEntity>>

    @Query("SELECT * FROM loyalty_cards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: Long): LoyaltyCardEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM loyalty_cards")
    suspend fun getNextSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: LoyaltyCardEntity): Long

    @Update
    suspend fun updateCard(card: LoyaltyCardEntity)

    @Update
    suspend fun updateCards(cards: List<LoyaltyCardEntity>)

    @Delete
    suspend fun deleteCard(card: LoyaltyCardEntity)

    @Query("DELETE FROM loyalty_cards WHERE id = :cardId")
    suspend fun deleteCardById(cardId: Long)
}
