package com.example.automatism.database.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

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
    @SerializedName("id")
    var id: Long = 0,
    @SerializedName("name")
    var name: String,
    @SerializedName("minute_on")
    var minute_on: Int? = null,
    @SerializedName("hour_on")
    var hour_on: Int? = null,
    @SerializedName("minute_off")
    var minute_off: Int? = null,
    @SerializedName("hour_off")
    var hour_off: Int? = null,
    @SerializedName("frequency")
    var frequency: Int?,
    @SerializedName("dateInitial")
    var date_initial: Long? = null,
    @SerializedName("statusOn")
    var status_on: Boolean = false,
    @SerializedName("statusOff")
    var status_off: Boolean = false,
    @SerializedName("device")
    var device: Long,
    @SerializedName("activated")
    var activated: Boolean = true
) {
    fun toMap(): Map<String, Any?> {
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
        fun fromMap(map: Map<String, Any?>): Schedule {
            return Schedule(
                id = map["id"] as Long,
                name = map["name"] as String,
                minute_on = map["minute_on"] as Int?,
                hour_on = map["hour_on"] as Int?,
                minute_off = map["minute_off"] as Int?,
                hour_off = map["hour_off"] as Int?,
                frequency = map["frequency"] as Int?,
                device = map["device"] as Long,
                activated = map["activated"] as Boolean
            )
        }
    }
}