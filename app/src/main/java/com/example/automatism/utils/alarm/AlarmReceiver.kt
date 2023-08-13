package com.example.automatism.utils.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
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

        try {
            if (action == true) {
                scope.launch(Dispatchers.IO) {
                    // val deviceInfo = deviceDao.getDeviceById(deviceID!!)
                    SMSManager.sendSMS(telephone!!, messageOn!!)
                    deviceDao.updateDeviceStatus(deviceID!!,true)
                    if(myPreferences.getBoolean("USER_ACTIVE",false)){
                        val change_status = RetrofitInstance.api.setDeviceStatus(
                            deviceID,
                            myPreferences.getString("jwt","")!!,
                            mapOf(
                                "status" to true
                            )
                        )
                    }
                    Log.d(
                        "MainActivity2",
                        "${action.toString()} Alarm Triggered: $messageOn => $telephone"
                    )
                }
            } else {
                scope.launch(Dispatchers.IO) {
                    // val deviceInfo = deviceDao.getDeviceById(deviceID!!)
                    SMSManager.sendSMS(telephone!!, messageOff!!)
                    deviceDao.updateDeviceStatus(deviceID!!,false)
                    if(myPreferences.getBoolean("USER_ACTIVE",false)){
                        val change_status = RetrofitInstance.api.setDeviceStatus(
                            deviceID,
                            myPreferences.getString("jwt","")!!,
                            mapOf(
                                "status" to false
                            )
                        )
                    }
                    Log.d(
                        "MainActivity2",
                        "${action.toString()} Alarm Triggered: $messageOn => $telephone"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity2","Some Error $e")
        }

        Log.i("MainActivity2","Reschudling... $telephone => action(${action})")
        // If the frequency is null, then the alarm will not repeat.
        scope.launch(Dispatchers.IO) {
            if(frequency == -1) {
                /*if(action == false) {
                    var schedule1 = scheduleDao.getScheduleById(scheduleId!!)
                    val modified_Device = schedule1
                    modified_Device.activated = false
                    scheduleDao.updateSchedule(modified_Device)
                }*/
                return@launch
            }
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
                userId = userID!!
            ),
            isInitial = false
        )
        Log.i("MainActivity2","Reschudeled Successfuly $telephone => action(${action})")
    }
}