package com.example.automatism.database.models

import androidx.room.Embedded
import androidx.room.Relation

data class ScheduleDevice (
    @Embedded
    val schedule: Schedule,
    @Relation(
        parentColumn = "device",
        entityColumn = "id"
    )
    val device: Device
)