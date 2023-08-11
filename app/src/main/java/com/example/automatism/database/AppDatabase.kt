package com.example.automatism.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.example.automatism.database.dao.DeviceDao
import com.example.automatism.database.dao.ScheduleDao

import com.example.automatism.database.models.Device
import com.example.automatism.database.models.Schedule

@Database(
    entities = [Device::class, Schedule::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        private const val DATABASE_NAME = "myDatabase"
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
