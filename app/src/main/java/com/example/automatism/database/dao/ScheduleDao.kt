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
    suspend fun getAllSchedules(): List<Schedule>

    @Query("SELECT * FROM schedules WHERE id = :scheduleId")
    suspend fun getScheduleById(scheduleId: Long): Schedule

    @Query("SELECT * FROM schedules WHERE device = :deviceId")
    suspend fun getSchedulesByDeviceId(deviceId: Long): List<Schedule>

    @Query("SELECT * FROM schedules WHERE id = :scheduleId LIMIT 1")
    suspend fun getScheduleAndDeviceByScheduleId(scheduleId: Long) : ScheduleDevice

    @Query("SELECT * FROM schedules")
    suspend fun getAllScheduleAndDevices(): List<ScheduleDevice>

    @Query("SELECT activated FROM schedules WHERE id = :scheduleId LIMIT 1")
    suspend fun getIsActivatedStatusById(scheduleId: Long): Boolean?

    @Query("SELECT * FROM schedules WHERE device IN (SELECT id FROM devices WHERE user_id = :userId)")
    suspend fun getAllScheduleAndDevicesByUserId(userId: Long): List<ScheduleDevice>

    @Query("UPDATE schedules SET status_on = :statusOn, status_off = :statusOff WHERE id = :scheduleId")
    suspend fun updateScheduleStatuses(scheduleId: Long, statusOn: Boolean, statusOff: Boolean)

    @Insert
    suspend fun insertSchedule(schedule: Schedule): Long

    @Update
    suspend fun updateSchedule(schedule: Schedule)

    @Delete
    suspend fun deleteSchedule(schedule: Schedule)

    @Query("DELETE FROM schedules WHERE id NOT IN (:idList)")
    suspend fun deleteSchedulesByNotInIds(idList: List<Long?>?)

    @Upsert
    suspend fun upsertSchedule(schedule: Schedule): Long
}