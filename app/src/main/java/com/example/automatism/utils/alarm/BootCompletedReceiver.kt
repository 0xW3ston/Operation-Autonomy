package com.example.automatism.utils.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("MainActivity2","[BOOT COMPLETE]: Is context null => ${context == null}")
            context ?: return
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
                    Scheduler.runExpired()
                    Scheduler.initialize()
                    Log.i("MainActivity2","[BOOT COMPLETE]: Successfully Initiated all Schedules")
                } catch (e: Exception) {
                    Log.i("MainActivity2","[BOOT COMPLETE]: Error => $e")
                }
            }
        }
    }
}