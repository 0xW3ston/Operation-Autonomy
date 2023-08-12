package com.example.automatism.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.automatism.R
import com.example.automatism.utils.AuthHelper
import com.example.automatism.utils.SMSManager
import com.example.automatism.utils.alarm.AlarmItem
import com.example.automatism.utils.alarm.AndroidAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SplashActivity : AppCompatActivity() {
    private val splashDuration = 5500L // 3 seconds
    // private lateinit var myPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_activity)

        // myPreferences = this.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)

        val Scheduler = AndroidAlarmScheduler(this)

        val authHelper = AuthHelper(this)

        //var current_user_id = myPreferences.getLong("CURRENT_USER_ID", -1L)
        lifecycleScope.launch(Dispatchers.IO){
            Scheduler.deinitialize()
            Scheduler.initialize()
            // To display the splash screen for a certain duration before redirecting
            Handler(Looper.getMainLooper()).postDelayed({
                // Check if the user is logged in (you can use SharedPreferences or any other authentication mechanism)
                // val sharedPreferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE)
                // val isAdmin = sharedPreferences.getBoolean("admin", false)

                // Decide which activity to redirect the user to based on the login status
                val redirectActivityClass = if (authHelper.isLoggedIn()) {
                    DevicesActivity::class.java
                } else {
                    LoginActivity::class.java
                }

                // Redirect the user to the appropriate activity
                val intent = Intent(this@SplashActivity, redirectActivityClass)
                startActivity(intent)
                finish()
            }, splashDuration)
        }


        /*
        Scheduler.schedule(
            AlarmItem(
                time = mapOf(
                    "hour" to 1,
                    "minute" to 40
                ),
                frequency = 5,
                telephone = "061212121212",
                messageOn = "msg-on",
                messageOff = "msg-off",
                action = true
            ),
            true
        )

        Scheduler.schedule(
            AlarmItem(
                time = mapOf(
                    "hour" to 1,
                    "minute" to 50
                ),
                frequency = 5,
                telephone = "061212121212",
                messageOn = "msg-on",
                messageOff = "msg-off",
                action = true
            ),
            true
        )


         */

        /*for(i in listOf(0,1,2,3,4,5,6,7,8,9,10,11,12,13)) {
            Scheduler.schedule(
                AlarmItem(
                    time = mapOf(
                        "hour" to 3,
                        "minute" to (41 + i)
                    ),
                    frequency = 24,
                    telephone = "061212121212$i",
                    messageOn = "msg-on",
                    messageOff = "msg-off",
                    action = true
                )
            )

            Scheduler.schedule(
                AlarmItem(
                    time = mapOf(
                        "hour" to 3,
                        "minute" to (41 + i + 1)
                    ),
                    frequency = 24,
                    telephone = "061212121212$i",
                    messageOn = "msg-on",
                    messageOff = "msg-off",
                    action = false
                )
            )
        } */
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu?.clear()
        return super.onPrepareOptionsMenu(menu)
    }
    /*
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu,menu)
        return true
    }*/

    /*
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.oRefresh -> Toast.makeText(this,"You click on Refresh", Toast.LENGTH_SHORT).show()
            R.id.oQuit -> finish()
        }
        return true
    }
     */
}