package com.example.data

import android.util.Log
import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromString(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            Json.decodeFromString<List<String>>(value)
        } catch (e: Exception) {
            Log.w("Converters", "Failed to decode attachment URIs from JSON", e)
            emptyList()
        }
    }

    @TypeConverter
    fun fromList(list: List<String>?): String {
        if (list.isNullOrEmpty()) return "[]"
        return Json.encodeToString(list)
    }
}
