package com.example.liveness

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    var gson = Gson()

    @TypeConverter
    fun fromDate(value: Date?): Long? {
        return value?.time
    }

    @TypeConverter
    fun toDate(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun fromString(value: String?): List<String>? {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>?): String? {
        val gson = Gson()
        return gson.toJson(list)
    }
}
