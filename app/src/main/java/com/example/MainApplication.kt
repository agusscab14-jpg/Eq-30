package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.EqRepository

class MainApplication : Application() {
    lateinit var repository: EqRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this)
        repository = EqRepository(database.eqProfileDao())
    }
}
