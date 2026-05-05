package com.flutter.stepstonesflt.data.local

import androidx.room.TypeConverter
import com.flutter.stepstonesflt.data.local.entity.MediaType

class Converters {
    @TypeConverter
    fun fromMediaType(type: MediaType): String = type.name

    @TypeConverter
    fun toMediaType(value: String): MediaType = MediaType.valueOf(value)
}
