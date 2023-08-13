package com.example.automatism.utils.alarm

data class AlarmItem(
    val time: Map<String,Int>,
    val frequency: Int?,
    val telephone: String,
    val messageOn: String,
    val messageOff: String,
    val action: Boolean,
    val deviceId: Long,
    val userId: Long
)
