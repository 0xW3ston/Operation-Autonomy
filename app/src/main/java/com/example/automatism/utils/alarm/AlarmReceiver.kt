package com.example.automatism.utils.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.automatism.database.AppDatabase
import com.example.automatism.utils.RetrofitInstance
import com.example.automatism.utils.SMSManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("MainActivity2","Is It (Context) Null Lol? => ${(context == null).toString()}")
        context ?: return

        val myPreferences: SharedPreferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)

        val scope = if (context is androidx.lifecycle.LifecycleOwner) {
            context.lifecycleScope
        } else {
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
        }
        val database = AppDatabase.getInstance(context)
        val deviceDao = database.deviceDao()
        val scheduleDao = database.scheduleDao()

        val Scheduler = AndroidAlarmScheduler(context)

        val frequency = intent?.getIntExtra("FREQUENCY",-1)
        val telephone = intent?.getStringExtra("TELEPHONE")
        val messageOn = intent?.getStringExtra("MESSAGE_ON")
        val messageOff = intent?.getStringExtra("MESSAGE_OFF")
        val action = intent?.getBooleanExtra("ACTION",false)
        val timeH = intent?.getIntExtra("TIME_H",-1)
        val timeM = intent?.getIntExtra("TIME_M",-1)
        val deviceID = intent?.getLongExtra("DEVICE_ID",-1L)
        val userID = intent?.getLongExtra("USER_ID",-1L)
        val scheduleId = intent?.getLongExtra("SCHEDULE_ID", -1L)
        val isMissed = intent?.getBooleanExtra("IS_MISSED", false)

        try {
            Log.d("MainActivity2","$scheduleId")
            if (action == true) {
                scope.launch(Dispatchers.IO) {
                    // val deviceInfo = deviceDao.getDeviceById(deviceID!!)
                    SMSManager.sendSMS(telephone!!, messageOn!!)
                    scheduleDao.updateScheduleStatuses(scheduleId!!, statusOn = true, statusOff = false)
                    deviceDao.updateDeviceStatus(deviceID!!,true)
                    if(myPreferences.getBoolean("USER_ACTIVE",false) == true && myPreferences.getLong("CURRENT_USER_ID",-1L) == userID){
                        val change_status = RetrofitInstance.api.setDeviceStatus(
                            deviceID,
                            myPreferences.getString("jwt","")!!,
                            mapOf(
                                "status" to true
                            )
                        )
                    }
                }
            } else {
                scope.launch(Dispatchers.IO) {
                    // val deviceInfo = deviceDao.getDeviceById(deviceID!!)
                    SMSManager.sendSMS(telephone!!, messageOff!!)
                    scheduleDao.updateScheduleStatuses(scheduleId!!, statusOn = false, statusOff = false)
                    deviceDao.updateDeviceStatus(deviceID!!,false)
                    Log.d(
                        "MainActivity2",
                        "${action.toString()} Alarm Triggered: $messageOn => $telephone"
                    )
                    if(myPreferences.getBoolean("USER_ACTIVE",false) == true && myPreferences.getLong("CURRENT_USER_ID",-1L) == userID){
                        val change_status = RetrofitInstance.api.setDeviceStatus(
                            deviceID,
                            myPreferences.getString("jwt","")!!,
                            mapOf(
                                "status" to false
                            )
                        )
                        if(frequency == null || frequency == -1) {
                            val change_is_activated = RetrofitInstance.api.setIsActivated(
                                reglageId = scheduleId,
                                authToken = myPreferences.getString("jwt","")!!,
                                requestBody = mapOf(
                                    "isActivated" to false
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity2","Some Error $e")
        }

        Log.i("MainActivity2","Reschudling... $telephone => action(${action})")
        // If the frequency is null, then the alarm will not repeat.
        var shouldExit = false

        if(isMissed != null && isMissed == true) {
            scope.launch(Dispatchers.IO) {
                if(action == false ){
                    scheduleDao.updateScheduleStatuses(scheduleId!!, statusOn = false, statusOff = false)
                    if(frequency == -1) {
                        var schedule1 = scheduleDao.getScheduleById(scheduleId!!)
                        var modified_Device = schedule1
                        modified_Device.activated = false
                        scheduleDao.updateSchedule(modified_Device)
                    }
                } else {
                    scheduleDao.updateScheduleStatuses(scheduleId!!, statusOn = true, statusOff = false)
                }
            }
            shouldExit = true
            return
        }

        if(frequency == -1 || frequency == null) {
            if(action == false) {
                scope.launch(Dispatchers.IO) {
                    var schedule1 = scheduleDao.getScheduleById(scheduleId!!)
                    var modified_Device = schedule1
                    modified_Device.activated = false
                    Log.d("MainActivity2","Updating One-Time Row")
                    scheduleDao.updateSchedule(modified_Device)
                }
            }
            shouldExit = true
        }


        if(shouldExit) {
            return
        }

        Scheduler.schedule(
            AlarmItem(
                time = mapOf(
                    "hour" to timeH!!,
                    "minute" to timeM!!
                ),
                frequency = frequency!!,
                telephone = telephone!!,
                messageOn = messageOn!!,
                messageOff = messageOff!!,
                action = action!!,
                deviceId = deviceID!!,
                userId = userID!!,
                scheduleId = scheduleId!!
            ),
            isInitial = false
        )
        Log.i("MainActivity2","Reschudeled Successfuly $telephone => action(${action})")
    }
}