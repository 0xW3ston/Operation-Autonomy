package com.example.automatism.utils

import android.util.Log
import java.util.Calendar

object TimeCalculationClass {
    fun calculateInitialDelay(hour: Int, minute: Int): Long {
        val now = System.currentTimeMillis()
        val specificTimeToday = getSpecificTimeMillis(hour, minute)

        if (specificTimeToday <= now) {
            // If the specific time has already passed today, schedule it for the same time tomorrow
            return getSpecificTimeMillis(hour, minute, 1)
        }
        // Log.d("MainActivity2","IDK TIME: ${specificTimeToday - now}")
        return specificTimeToday
    }
    fun getSpecificTimeMillis(hour: Int, minute: Int, daysToAdd: Int = 0): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DATE, daysToAdd)
        return calendar.timeInMillis
    }

    fun calculateNextScheduleWithDateInitial(dateInitial: Long, frequency: Int): Long {
        val now = System.currentTimeMillis()
        val specificTime = dateInitial

        var nextScheduledTime = specificTime

        while (nextScheduledTime <= now) {
            nextScheduledTime += (frequency * 60 * 60 * 1000)
        }

        return nextScheduledTime
    }
    fun calculateNextSchedule(hour: Int, minute: Int, frequency: Int): Long {
        val now = System.currentTimeMillis()
        val specificTimeToday = getSpecificTimeMillis(hour, minute)

        // Calculate the next scheduled time
        var nextScheduledTime = specificTimeToday

        while (nextScheduledTime <= now) {
            nextScheduledTime += (frequency * 60 * 60 * 1000)
        }

        return nextScheduledTime
    }
    fun calculateNextFrequency(timeInMillis: Long, frequency: Int): Long {
        return timeInMillis + (frequency * 60 * 60 * 1000)
    }
}