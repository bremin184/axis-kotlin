package com.axis.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.axis.app.data.model.CategorizationRule

@Dao
interface CategorizationRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CategorizationRule)

    @Query("SELECT * FROM categorization_rules")
    suspend fun getAllRules(): List<CategorizationRule>

    @Query("DELETE FROM categorization_rules WHERE id = :ruleId")
    suspend fun deleteRule(ruleId: Long)

    @Query("SELECT * FROM categorization_rules WHERE recipientIdentifier = :recipient LIMIT 1")
    suspend fun findByRecipient(recipient: String): CategorizationRule?

    @Query("DELETE FROM categorization_rules")
    suspend fun deleteAll()
}
