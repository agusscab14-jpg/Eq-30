package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "eq_profiles")
@TypeConverters(EqProfileConverters::class)
data class EqProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isCustom: Boolean = true,
    val levels: List<Float> // Must be 30 floats
)

class EqProfileConverters {
    @TypeConverter
    fun fromList(list: List<Float>): String {
        return list.joinToString(",")
    }

    @TypeConverter
    fun toList(data: String): List<Float> {
        if (data.isBlank()) return emptyList()
        return data.split(",").mapNotNull { it.toFloatOrNull() }
    }
}
