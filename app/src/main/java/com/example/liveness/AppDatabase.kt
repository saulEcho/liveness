package com.example.liveness

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [EmployeeCheckIn::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun checkInDao(): CheckInDao
}