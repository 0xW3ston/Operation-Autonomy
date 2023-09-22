package com.example.automatism.utils.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.example.automatism.database.AppDatabase
import com.example.automatism.utils.TimeCalculationClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AndroidAlarmScheduler(
    private val context: Context
) : AlarmScheduler {

    private val alarmManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        context.getSystemService(AlarmManager::class.java)
    } else {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val scope = if (context is androidx.lifecycle.LifecycleOwner) {
        context.lifecycleScope
    } else {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
    }

    private var timecalc: TimeCalculationClass = TimeCalculationClass
    override fun initialize() {
        try {
            val database = AppDatabase.getInstance(context)
            scope.launch(Dispatchers.IO) {
                val scheduleDao = database.scheduleDao()
                val schedulesDevices = scheduleDao.getAllScheduleAndDevices()
                for (item in schedulesDevices) {
                    if (item.schedule.activated) {
                        if (item.schedule.hour_on != null && item.schedule.minute_on != null){
                            val alarmItemOn = AlarmItem(
                                time = mapOf(
                                    "hour" to item.schedule.hour_on!!,
                                    "minute" to item.schedule.minute_on!!
                                ),
                                frequency = item.schedule.frequency,
                                telephone = item.device.telephone,
                                messageOn = item.device.msg_on,
                                messageOff = item.device.msg_off,
                                action = true,
                                deviceId = item.device.id,
                                userId = item.device.user_id,
                                scheduleId = item.schedule.id,
                                dateInitial = item.schedule.date_initial
                            )
                            schedule(alarmItemOn, true)
                        }
                        if (item.schedule.hour_off != null && item.schedule.minute_off != null){
                            val alarmItemOff = AlarmItem(
                                time = mapOf(
                                    "hour" to item.schedule.hour_off!!,
                                    "minute" to item.schedule.minute_off!!
                                ),
                                frequency = item.schedule.frequency,
                                telephone = item.device.telephone,
                                messageOn = item.device.msg_on,
                                messageOff = item.device.msg_off,
                                action = false,
                                deviceId = item.device.id,
                                userId = item.device.user_id,
                                scheduleId = item.schedule.id,
                                dateInitial = item.schedule.date_initial
                            )
                            schedule(alarmItemOff, true)
                        }
                    }
                }
                Log.w("MainActivity2","Initialize")
            }
        } catch (e: Exception) {
            Log.e("MainActivity2","Problem Initialize!!!")
        }
    }
    override fun deinitialize() {
        try {
            val database = AppDatabase.getInstance(context)
            scope.launch {
                val scheduleDao = database.scheduleDao()
                val schedulesDevices = scheduleDao.getAllScheduleAndDevices()
                for (item in schedulesDevices) {
                    if (item.schedule.activated) {
                        if (item.schedule.hour_on != null && item.schedule.minute_on != null){
                            val alarmItemOn = AlarmItem(
                                time = mapOf(
                                    "hour" to item.schedule.hour_on!!,
                                    "minute" to item.schedule.minute_on!!
                                ),
                                frequency = item.schedule.frequency,
                                telephone = item.device.telephone,
                                messageOn = item.device.msg_on,
                                messageOff = item.device.msg_off,
                                action = true,
                                deviceId = item.device.id,
                                userId = item.device.user_id,
                                scheduleId = item.schedule.id,
                                dateInitial = item.schedule.date_initial
                            )
                            cancel(alarmItemOn)
                        }
                        if (item.schedule.hour_off != null && item.schedule.minute_off != null){
                            val alarmItemOff = AlarmItem(
                                time = mapOf(
                                    "hour" to item.schedule.hour_off!!,
                                    "minute" to item.schedule.minute_off!!
                                ),
                                frequency = item.schedule.frequency,
                                telephone = item.device.telephone,
                                messageOn = item.device.msg_on,
                                messageOff = item.device.msg_off,
                                action = false,
                                deviceId = item.device.id,
                                userId = item.device.user_id,
                                scheduleId = item.schedule.id,
                                dateInitial = item.schedule.date_initial
                            )
                            cancel(alarmItemOff)
                        }
                    }
                }
                Log.e("MainActivity2","Deinitialize")
            }
        } catch (e: Exception) {
            Log.e("MainActivity2","Problem Deinitialize")
        }
    }
    override fun initialize(userId: Long) {
        val database = AppDatabase.getInstance(context)
        scope.launch {
            val scheduleDao = database.scheduleDao()
            val schedulesDevices = scheduleDao.getAllScheduleAndDevicesByUserId(userId)
            for(item in schedulesDevices) {
                if (item.schedule.activated) {
                    if (item.schedule.hour_on != null && item.schedule.minute_on != null){
                        val alarmItemOn = AlarmItem(
                            time = mapOf(
                                "hour" to item.schedule.hour_on!!,
                                "minute" to item.schedule.minute_on!!
                            ),
                            frequency = item.schedule.frequency,
                            telephone = item.device.telephone,
                            messageOn = item.device.msg_on,
                            messageOff = item.device.msg_off,
                            action = true,
                            deviceId = item.device.id,
                            userId = item.device.user_id,
                            scheduleId = item.schedule.id,
                            dateInitial = item.schedule.date_initial
                        )
                        schedule(alarmItemOn, true)
                    }
                    if (item.schedule.hour_off != null && item.schedule.minute_off != null){
                        val alarmItemOff = AlarmItem(
                            time = mapOf(
                                "hour" to item.schedule.hour_off!!,
                                "minute" to item.schedule.minute_off!!
                            ),
                            frequency = item.schedule.frequency,
                            telephone = item.device.telephone,
                            messageOn = item.device.msg_on,
                            messageOff = item.device.msg_off,
                            action = false,
                            deviceId = item.device.id,
                            userId = item.device.user_id,
                            scheduleId = item.schedule.id,
                            dateInitial = item.schedule.date_initial
                        )
                        schedule(alarmItemOff, true)
                    }
                }
                Log.w("MainActivity2","Initialized [USER-SPECIFIC]: $userId")
            }
        }
    }
    override fun deinitialize(userId: Long) {
        val database = AppDatabase.getInstance(context)
        scope.launch {
            val scheduleDao = database.scheduleDao()
            val schedulesDevices = scheduleDao.getAllScheduleAndDevicesByUserId(userId)
            for(item in schedulesDevices) {
                if (item.schedule.activated) {
                    if (item.schedule.hour_on != null && item.schedule.minute_on != null){
                        val alarmItemOn = AlarmItem(
                            time = mapOf(
                                "hour" to item.schedule.hour_on!!,
                                "minute" to item.schedule.minute_on!!
                            ),
                            frequency = item.schedule.frequency,
                            telephone = item.device.telephone,
                            messageOn = item.device.msg_on,
                            messageOff = item.device.msg_off,
                            action = true,
                            deviceId = item.device.id,
                            userId = item.device.user_id,
                            scheduleId = item.schedule.id,
                            dateInitial = item.schedule.date_initial
                        )
                        cancel(alarmItemOn)
                    }
                    if (item.schedule.hour_off != null && item.schedule.minute_off != null){
                        val alarmItemOff = AlarmItem(
                            time = mapOf(
                                "hour" to item.schedule.hour_off!!,
                                "minute" to item.schedule.minute_off!!
                            ),
                            frequency = item.schedule.frequency,
                            telephone = item.device.telephone,
                            messageOn = item.device.msg_on,
                            messageOff = item.device.msg_off,
                            action = false,
                            deviceId = item.device.id,
                            userId = item.device.user_id,
                            scheduleId = item.schedule.id,
                            dateInitial = item.schedule.date_initial
                        )
                        cancel(alarmItemOff)
                    }
                }
            }
            Log.e("MainActivity2","Deinitialized [USER-SPECIFIC]: $userId")
        }
    }
    override fun schedule(item: AlarmItem, isInitial: Boolean) {

        Log.e("MainActivity2","New Schedule: ${item.action} => hashCode: ${item.hashCode()}")

        var initialTime = -1L
        var nextTime = -1L

        initialTime = timecalc.calculateInitialDelay(item.time["hour"] as Int,item.time["minute"] as Int)

        if(item.frequency != null){
            if (item.frequency == 24)
            {
                nextTime = timecalc.calculateNextFrequency(System.currentTimeMillis(), item.frequency)
                Log.d("MainActivity2", "[1H => INITIAL]: ${nextTime}")
            }
            else
            {
                if (item.dateInitial != null){
                    if (System.currentTimeMillis() > item.dateInitial){
                        // initialTime = timecalc.calculateNextSchedule(item.time["hour"] as Int, item.time["minute"] as Int, item.frequency)
                        initialTime = timecalc.calculateNextScheduleWithDateInitial(item.dateInitial, item.frequency)
                        nextTime = timecalc.calculateNextScheduleWithDateInitial(item.dateInitial, item.frequency)
                        Log.d("MainActivity2", "Already Initiated: ${initialTime}")
                    } else {
                        // nextTime = timecalc.calculateNextFrequency(System.currentTimeMillis(), item.frequency)
                        initialTime = timecalc.calculateInitialDelay(item.time["hour"] as Int,item.time["minute"] as Int)
                        nextTime = timecalc.calculateNextScheduleWithDateInitial(item.dateInitial, item.frequency)
                        Log.d("MainActivity2", "Not yet initiated: ${initialTime}")
                    }
                }
            }
            //val nextTime = calculateNextSchedule(item.time["hour"] as Int, item.time["minute"] as Int, item.frequency)
        }

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
            putExtra("SCHEDULE_ID", item.scheduleId)
            putExtra("INITIAL_DATE", item.dateInitial)
        }

        Log.i("MainActivity2","${item.toString()}:${isInitial.toString()}")

        var timeToSet = if (isInitial == true) initialTime else nextTime
        Log.w("MainActivity2","Time of next Execution (EPOX): ${timeToSet.toString()} (${if (isInitial) "INITIAL" else "INTERVALED"})")

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

    /* override fun runExpired() {
        val database = AppDatabase.getInstance(context)
        scope.launch {
            try {
                val timeNow = System.currentTimeMillis()
                val scheduleDao = database.scheduleDao()
                val schedulesDevices = scheduleDao.getAllScheduleAndDevices()

                for (item in schedulesDevices) {
                    // By timeDateInitial I mean "the datetime when "On" alarm should execute
                    // val timeDateInitial: Long? = item.schedule.date_initial
                    val expectedTimeOn = timecalc.calculateInitialDelay(
                        item.schedule.hour_on,
                        item.schedule.minute_on
                    )
                    val expectedTimeOff = timecalc.calculateInitialDelay(
                        item.schedule.hour_off,
                        item.schedule.minute_off
                    )
                    val timeDifference = Math.abs(expectedTimeOff - expectedTimeOn)
                    val differenceFromNowToOff = expectedTimeOff - timeNow
                    val differenceFromNowToOn = expectedTimeOn - timeNow

                    var eligibleForLateAlarm = false
                    Log.i("MainActivity2","expected On: ${expectedTimeOn}, expected Off: ${expectedTimeOff}, diff from now to off: ${differenceFromNowToOff}, timediff: ${timeDifference}")
                    // timeNow >= timeDateInitial!! &&
                    if (item.schedule.activated == true) {
                        // Case of: Finished or Days since last execution
                        if (item.schedule.status_on == false && item.schedule.status_off == false) {
                            if (differenceFromNowToOn > timeDifference && differenceFromNowToOff > timeDifference) {
                                continue
                            } else if (differenceFromNowToOn > timeDifference) {
                                eligibleForLateAlarm = true
                                val intent = Intent(context, AlarmReceiver::class.java).apply {
                                    putExtra("TELEPHONE", item.device.telephone)
                                    putExtra("MESSAGE_ON", item.device.msg_on)
                                    putExtra("MESSAGE_OFF", item.device.msg_off)
                                    putExtra("ACTION", true)
                                    putExtra("TIME_H", item.schedule.hour_off)
                                    putExtra("TIME_M", item.schedule.minute_off)
                                    putExtra("DEVICE_ID", item.device.id)
                                    putExtra("USER_ID", item.device.user_id)
                                    putExtra("SCHEDULE_ID", item.schedule.id)
                                    putExtra("IS_MISSED", true)
                                }
                                alarmManager.setAlarmClock(
                                    AlarmManager.AlarmClockInfo(
                                        -1,
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
                                Log.i("MainActivity2","[EXECUTING LATE ALARM]: ${item.device.telephone} => ${item.device.msg_off}")
                            }
                        } else if (item.schedule.status_on == true && item.schedule.status_off == false) {
                            if (differenceFromNowToOff > timeDifference ) {
                                eligibleForLateAlarm = true
                                val intent = Intent(context, AlarmReceiver::class.java).apply {
                                    putExtra("TELEPHONE", item.device.telephone)
                                    putExtra("MESSAGE_ON", item.device.msg_on)
                                    putExtra("MESSAGE_OFF", item.device.msg_off)
                                    putExtra("ACTION", false)
                                    putExtra("TIME_H", item.schedule.hour_off)
                                    putExtra("TIME_M", item.schedule.minute_off)
                                    putExtra("DEVICE_ID", item.device.id)
                                    putExtra("USER_ID", item.device.user_id)
                                    putExtra("SCHEDULE_ID", item.schedule.id)
                                    putExtra("IS_MISSED", true)
                                }
                                alarmManager.setAlarmClock(
                                    AlarmManager.AlarmClockInfo(
                                        -1,
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
                                Log.i("MainActivity2","[EXECUTING LATE ALARM]: ${item.device.telephone} => ${item.device.msg_off}")
                            }
                        }
                    }
                }
                Log.e("MainActivity2", "Initialized [RUN-EXPIRED]")
            } catch (e: Exception) {
                Log.e("MainActivity2", "error: [RUN-EXPIRED] : $e")
            }
        }
    } */
    override fun runExpired() {

    }

}