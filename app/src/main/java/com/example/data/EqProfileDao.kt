package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EqProfileDao {
    @Query("SELECT * FROM eq_profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<EqProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: EqProfile): Long

    @Query("DELETE FROM eq_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Int)
}
