package com.example.liveness

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.*

@Entity
data class EmployeeCheckIn(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Date,
    val images: List<String>,
    val employeeCode: String,
    val place: String,
    val checkInStatus: String,
    val name: String ,
    val idNumber: String
): Serializable