package com.axis.app.data.database

import androidx.room.*
import com.axis.app.data.model.ActivatedService
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivatedServiceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(service: ActivatedService)

    @Query("SELECT * FROM activated_services WHERE isActive = 1")
    fun getActiveServices(): Flow<List<ActivatedService>>

    @Query("SELECT * FROM activated_services WHERE isActive = 1")
    suspend fun getActiveServicesList(): List<ActivatedService>

    @Query("SELECT keyword FROM activated_services WHERE isActive = 1")
    suspend fun getActiveKeywords(): List<String>

    @Delete
    suspend fun delete(service: ActivatedService)

    @Query("DELETE FROM activated_services")
    suspend fun deleteAll()
}
