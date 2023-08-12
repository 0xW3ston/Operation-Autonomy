package com.example.automatism.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("users")
data class User (
    @PrimaryKey(autoGenerate = false)
    val id: Long = 0,
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): User {
            return User(
                id = map["id"] as Long
            )
        }
    }
}