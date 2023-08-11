package com.example.automatism.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.automatism.database.models.Device

@Dao
interface DeviceDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(device: Device)

    @Upsert
    suspend fun upsert(device: Device)

    @Update
    suspend fun update(device: Device)

    @Delete
    suspend fun delete(device: Device)

    @Query("SELECT * FROM devices")
    suspend fun getAllDevices(): List<Device>

    @Query("SELECT * FROM devices WHERE id = :deviceId")
    suspend fun getDeviceById(deviceId: Long): Device

    @Query("SELECT status FROM devices WHERE id = :deviceId LIMIT 1")
    suspend fun getDeviceStatusById(deviceId: Long): Boolean?

    @Query("UPDATE devices SET status = :newStatus WHERE id = :deviceId")
    suspend fun updateDeviceStatus(deviceId: Long, newStatus: Boolean)

    @Query("DELETE FROM devices WHERE id NOT IN (:idList)")
    suspend fun deleteDevicesNotInListById(idList: List<Long?>?)
}
