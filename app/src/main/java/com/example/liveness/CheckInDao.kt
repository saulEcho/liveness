package com.example.liveness

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CheckInDao {
    @Query("SELECT * FROM EmployeeCheckIn")
    fun getAll(): List<EmployeeCheckIn>

    @Insert
    fun insert(checkIn: EmployeeCheckIn)

    @Delete
    fun delete(checkIn: EmployeeCheckIn)
}