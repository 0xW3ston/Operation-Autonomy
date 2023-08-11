package com.example.automatism.database.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedules",
    foreignKeys = [ForeignKey(
        entity = Device::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("device"),
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )])
data class Schedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val minute_on: Int,
    val hour_on: Int,
    val minute_off: Int,
    val hour_off: Int,
    val frequency: Int,
    val device: Long
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "minute_on" to minute_on,
            "hour_on" to hour_on,
            "minute_off" to minute_off,
            "hour_off" to hour_off,
            "frequency" to frequency,
            "device" to device
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): Schedule {
            return Schedule(
                id = map["id"] as Long,
                name = map["name"] as String,
                minute_on = map["minute_on"] as Int,
                hour_on = map["hour_on"] as Int,
                minute_off = map["minute_off"] as Int,
                hour_off = map["hour_off"] as Int,
                frequency = map["frequency"] as Int,
                device = map["device"] as Long
            )
        }
    }
}