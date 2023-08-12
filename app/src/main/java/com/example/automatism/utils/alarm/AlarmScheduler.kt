package com.example.automatism.utils.alarm

interface AlarmScheduler {
    fun schedule(item: AlarmItem, isInitial: Boolean)
    fun cancel(item: AlarmItem)
    fun initialize()
    fun deinitialize()
    fun initialize(userId: Long)

    fun deinitialize(userId: Long)
}