package com.example.automatism.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.automatism.database.models.Schedule
import com.example.automatism.database.models.ScheduleDevice

@Dao
interface ScheduleDao {
    @Query("SELECT *,telephone FROM schedules INNER JOIN devices ON schedules.device = devices.id")
    fun getAllSchedules(): List<Schedule>

    @Query("SELECT * FROM schedules WHERE id = :scheduleId")
    fun getScheduleById(scheduleId: Long): Schedule

    @Query("SELECT * FROM schedules WHERE device = :deviceId")
    fun getSchedulesByDeviceId(deviceId: Long): List<Schedule>

    @Query("SELECT * FROM schedules WHERE id = :scheduleId LIMIT 1")
    fun getScheduleAndDeviceByScheduleId(scheduleId: Long) : ScheduleDevice

    @Query("SELECT * FROM schedules")
    fun getAllScheduleAndDevices(): List<ScheduleDevice>

    @Query("SELECT * FROM schedules WHERE device IN (SELECT id FROM devices WHERE user_id = :userId)")
    fun getAllScheduleAndDevicesByUserId(userId: Long): List<ScheduleDevice>

    @Insert
    fun insertSchedule(schedule: Schedule): Long

    @Update
    fun updateSchedule(schedule: Schedule)

    @Delete
    fun deleteSchedule(schedule: Schedule)

    @Query("DELETE FROM schedules WHERE id NOT IN (:idList)")
    suspend fun deleteSchedulesByNotInIds(idList: List<Long?>?)

    @Upsert
    fun upsertSchedule(schedule: Schedule): Long
}