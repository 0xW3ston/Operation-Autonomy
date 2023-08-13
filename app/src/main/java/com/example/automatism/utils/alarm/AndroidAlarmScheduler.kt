package com.example.automatism.utils.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.automatism.database.AppDatabase
import java.util.Calendar

class AndroidAlarmScheduler(
    private val context: Context
) : AlarmScheduler {

    private val alarmManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        context.getSystemService(AlarmManager::class.java)
    } else {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    override fun initialize() {
        val database = AppDatabase.getInstance(context)
        val scheduleDao = database.scheduleDao()
        val schedulesDevices = scheduleDao.getAllScheduleAndDevices()
        for(item in schedulesDevices) {
            val alarmItemOn = AlarmItem(
                time = mapOf(
                    "hour" to item.schedule.hour_on,
                    "minute" to item.schedule.minute_on
                ),
                frequency = item.schedule.frequency,
                telephone = item.device.telephone,
                messageOn = item.device.msg_on,
                messageOff = item.device.msg_off,
                action = true,
                deviceId = item.device.id,
                userId = item.device.user_id
            )
            val alarmItemOff = AlarmItem(
                time = mapOf(
                    "hour" to item.schedule.hour_off,
                    "minute" to item.schedule.minute_off
                ),
                frequency = item.schedule.frequency,
                telephone = item.device.telephone,
                messageOn = item.device.msg_on,
                messageOff = item.device.msg_off,
                action = false,
                deviceId = item.device.id,
                userId = item.device.user_id
            )
            schedule(alarmItemOn,true)
            schedule(alarmItemOff,true)
        }
        Log.w("MainActivity2","Initialize")
    }
    override fun deinitialize() {
        val database = AppDatabase.getInstance(context)
        val scheduleDao = database.scheduleDao()
        val schedulesDevices = scheduleDao.getAllScheduleAndDevices()
        for(item in schedulesDevices) {
            val alarmItemOn = AlarmItem(
                time = mapOf(
                    "hour" to item.schedule.hour_on,
                    "minute" to item.schedule.minute_on
                ),
                frequency = item.schedule.frequency,
                telephone = item.device.telephone,
                messageOn = item.device.msg_on,
                messageOff = item.device.msg_off,
                action = true,
                deviceId = item.device.id,
                userId = item.device.user_id
            )
            val alarmItemOff = AlarmItem(
                time = mapOf(
                    "hour" to item.schedule.hour_off,
                    "minute" to item.schedule.minute_off
                ),
                frequency = item.schedule.frequency,
                telephone = item.device.telephone,
                messageOn = item.device.msg_on,
                messageOff = item.device.msg_off,
                action = false,
                deviceId = item.device.id,
                userId = item.device.user_id
            )
            cancel(alarmItemOn)
            cancel(alarmItemOff)
        }
        Log.e("MainActivity2","Deinitialize")
    }
    override fun initialize(userId: Long) {
        val database = AppDatabase.getInstance(context)
        val scheduleDao = database.scheduleDao()
        val schedulesDevices = scheduleDao.getAllScheduleAndDevicesByUserId(userId)
        for(item in schedulesDevices) {
            if(item.schedule.activated){
                val alarmItemOn = AlarmItem(
                    time = mapOf(
                        "hour" to item.schedule.hour_on,
                        "minute" to item.schedule.minute_on
                    ),
                    frequency = item.schedule.frequency,
                    telephone = item.device.telephone,
                    messageOn = item.device.msg_on,
                    messageOff = item.device.msg_off,
                    action = true,
                    deviceId = item.device.id,
                    userId = item.device.user_id
                )
                val alarmItemOff = AlarmItem(
                    time = mapOf(
                        "hour" to item.schedule.hour_off,
                        "minute" to item.schedule.minute_off
                    ),
                    frequency = item.schedule.frequency,
                    telephone = item.device.telephone,
                    messageOn = item.device.msg_on,
                    messageOff = item.device.msg_off,
                    action = false,
                    deviceId = item.device.id,
                    userId = item.device.user_id
                )
                schedule(alarmItemOn,true)
                schedule(alarmItemOff,true)
            }
        }
        Log.w("MainActivity2","Initialized [USER-SPECIFIC]: $userId")
    }
    override fun deinitialize(userId: Long) {
        val database = AppDatabase.getInstance(context)
        val scheduleDao = database.scheduleDao()
        val schedulesDevices = scheduleDao.getAllScheduleAndDevicesByUserId(userId)
        for(item in schedulesDevices) {
            val alarmItemOn = AlarmItem(
                time = mapOf(
                    "hour" to item.schedule.hour_on,
                    "minute" to item.schedule.minute_on
                ),
                frequency = item.schedule.frequency,
                telephone = item.device.telephone,
                messageOn = item.device.msg_on,
                messageOff = item.device.msg_off,
                action = true,
                deviceId = item.device.id,
                userId = item.device.user_id
            )
            val alarmItemOff = AlarmItem(
                time = mapOf(
                    "hour" to item.schedule.hour_off,
                    "minute" to item.schedule.minute_off
                ),
                frequency = item.schedule.frequency,
                telephone = item.device.telephone,
                messageOn = item.device.msg_on,
                messageOff = item.device.msg_off,
                action = false,
                deviceId = item.device.id,
                userId = item.device.user_id
            )
            cancel(alarmItemOn)
            cancel(alarmItemOff)
        }
        Log.e("MainActivity2","Initialized [USER-SPECIFIC]: $userId")
    }
    override fun schedule(item: AlarmItem, isInitial: Boolean) {

        Log.e("MainActivity2","New Schedule: ${item.action} => hashCode: ${item.hashCode()}")

        var initialTime = -1L
        var nextTime = -1L

        if(item.frequency != null){
            initialTime = calculateInitialDelay(item.time["hour"] as Int,item.time["minute"] as Int)
            nextTime = calculateNextFrequency(System.currentTimeMillis(), item.frequency)
            //val nextTime = calculateNextSchedule(item.time["hour"] as Int, item.time["minute"] as Int, item.frequency)
        }

        initialTime = calculateInitialDelay(item.time["hour"] as Int,item.time["minute"] as Int)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("FREQUENCY", item.frequency)
            putExtra("TELEPHONE", item.telephone)
            putExtra("MESSAGE_ON", item.messageOn)
            putExtra("MESSAGE_OFF", item.messageOff)
            putExtra("ACTION", item.action)
            putExtra("TIME_H", item.time["hour"] as Int)
            putExtra("TIME_M", item.time["minute"] as Int)
            putExtra("DEVICE_ID", item.deviceId as Long)
            putExtra("USER_ID", item.userId)
        }

        Log.i("MainActivity2","${item.toString()}:${isInitial.toString()}")

        val timeToSet = if (isInitial == true) initialTime else nextTime
        Log.w("MainActivity2","Time of next Execution (EPOX): ${timeToSet.toString()}")

        /*alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timeToSet,
            PendingIntent.getBroadcast(
                context,
                item.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )*/

            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(
                    timeToSet,
                    PendingIntent.getBroadcast(
                        context,
                        item.hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                ),
                PendingIntent.getBroadcast(
                    context,
                    item.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )




        /*
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                timeToSet,
                PendingIntent.getBroadcast(
                    context,
                    item.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        */

        /*

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo()

        )

        */

        /*
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            initialTime,
            (item.frequency) * 3600 * 1000L,
            PendingIntent.getBroadcast(
                context,
                item.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        */
    }
    override fun cancel(item: AlarmItem) {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                item.hashCode(),
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        Log.w("MainActivity2","Cancelled successfully: ${item.toString()}")
        Log.w("MainActivity2","Cancelled alarm's hashcode: ${item.hashCode()}")
    }

    public fun calculateInitialDelay(hour: Int, minute: Int): Long {
        val now = System.currentTimeMillis()
        val specificTimeToday = getSpecificTimeMillis(hour, minute)

        if (specificTimeToday <= now) {
            // If the specific time has already passed today, schedule it for the same time tomorrow
            return getSpecificTimeMillis(hour, minute, 1)
        }
        // Log.d("MainActivity2","IDK TIME: ${specificTimeToday - now}")
        return specificTimeToday
    }
    public fun calculateNextSchedule(hour: Int, minute: Int, frequency: Int): Long {
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
    private fun calculateNextFrequency(timeInMillis: Long, frequency: Int): Long {
        return timeInMillis + (frequency * 60 * 60 * 1000)
    }
    private fun getSpecificTimeMillis(hour: Int, minute: Int, daysToAdd: Int = 0): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DATE, daysToAdd)
        return calendar.timeInMillis
    }
}