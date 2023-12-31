package com.example.automatism.utils.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class BootCompletedReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("MainActivity2","[BOOT COMPLETE]: Is context null => ${context == null}")
            context ?: return
            val user_id = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE).getLong("CURRENT_USER_ID", -1L)
            // Use lifecycleScope if available, otherwise use a standard coroutine scope
            val scope = if (context is androidx.lifecycle.LifecycleOwner) {
                context.lifecycleScope
            } else {
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
            }

            scope.launch(Dispatchers.IO) {
                try {
                    val Scheduler = AndroidAlarmScheduler(context)
                    // TODO("Initialize ALL ALARMS OF ALL USERS ON THIS PHONE")
                    runBlocking {
                        Scheduler.runExpired()
                        Scheduler.initialize(user_id)
                    }
                    Log.i("MainActivity2","[BOOT COMPLETE]: Successfully Initiated all Schedules")
                } catch (e: Exception) {
                    Log.i("MainActivity2","[BOOT COMPLETE]: Error => $e")
                }
            }
        }
    }
}