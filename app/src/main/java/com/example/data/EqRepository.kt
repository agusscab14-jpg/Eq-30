package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class EqRepository(private val eqProfileDao: EqProfileDao) {
    val allProfiles: Flow<List<EqProfile>> = eqProfileDao.getAllProfiles()

    val currentLevels = MutableStateFlow<List<Float>>(List(30) { 0f })
    
    val currentFrequencies = MutableStateFlow<List<Float>>(listOf(
        25f, 31f, 40f, 50f, 63f, 80f, 100f, 125f, 160f, 200f, 250f, 315f, 400f, 500f, 630f, 800f,
        1000f, 1250f, 1600f, 2000f, 2500f, 3150f, 4000f, 5000f, 6300f, 8000f, 10000f, 12500f, 16000f, 20000f
    ))

    suspend fun insertProfile(profile: EqProfile): Long {
        return eqProfileDao.insertProfile(profile)
    }

    suspend fun deleteProfileById(id: Int) {
        eqProfileDao.deleteProfileById(id)
    }
}
