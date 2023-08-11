package com.example.automatism.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("devices")
data class Device(
    @PrimaryKey(autoGenerate = false)
    val id: Long = 0,
    val name: String,
    val description: String,
    val telephone: String,
    val msg_on: String,
    val msg_off: String,
    val status: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "description" to description,
            "telephone" to telephone,
            "msg_on" to msg_on,
            "msg_off" to msg_off,
            "status" to status
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): Device {
            return Device(
                id = (map["id"] as Double).toLong(),
                name = map["name"] as String,
                description = map["description"] as String,
                telephone = map["telephone"] as String,
                msg_on = map["msg_on"] as String,
                msg_off = map["msg_off"] as String,
                status = (if (map["status"] == null) false else map["status"]) as Boolean
            )
        }
    }
}