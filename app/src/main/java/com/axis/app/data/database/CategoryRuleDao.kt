package com.axis.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.axis.app.data.model.CategoryRuleEntity

@Dao
interface CategoryRuleDao {

    @Query("SELECT * FROM category_rules")
    suspend fun getAllRules(): List<CategoryRuleEntity>

    @Query("""
        SELECT * FROM category_rules 
        WHERE :message LIKE '%' || keyword || '%' 
        LIMIT 1
    """)
    suspend fun findMatchingRule(message: String): CategoryRuleEntity?
    
    @Query("SELECT * FROM category_rules WHERE category = :category")
    suspend fun getRulesForCategory(category: String): List<CategoryRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CategoryRuleEntity)

    @Delete
    suspend fun deleteRule(rule: CategoryRuleEntity)
    
    @Query("DELETE FROM category_rules WHERE category = :categoryName")
    suspend fun deleteRulesForCategory(categoryName: String)
}
