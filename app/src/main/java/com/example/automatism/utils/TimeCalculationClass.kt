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
    fun calculateNextSchedule(hour: Int, minute: Int, frequency: Int): Long {
        val now = System.currentTimeMillis()
        val specificTimeToday = getSpecificTimeMillis(hour, minute)

        Log.d("MainActivity2","SpecificTimeToday: ${specificTimeToday}|CalculatedNextFrequency: ${calculateNextFrequency(specificTimeToday, frequency)}")

        if(
            calculateNextFrequency(specificTimeToday, frequency) - now < 700000
        ) {
            val nextDaySchedule = getSpecificTimeMillis(hour, minute, 1)
            return nextDaySchedule
        }

        return calculateNextFrequency(specificTimeToday, frequency)
        /*
        if (specificTimeToday <= now) {
            // If the specific time has already passed today, schedule it for the same time tomorrow
            val nextDay = getSpecificTimeMillis(hour, minute, 1)
            return calculateNextFrequency(nextDay, frequency)
        } else {
            return calculateNextFrequency(specificTimeToday, frequency)
        }
        */
    }
    fun calculateNextFrequency(timeInMillis: Long, frequency: Int): Long {
        return timeInMillis + (frequency * 60 * 60 * 1000)
    }
}