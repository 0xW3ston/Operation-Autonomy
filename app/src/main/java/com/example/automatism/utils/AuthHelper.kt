package com.example.automatism.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.example.automatism.utils.alarm.AndroidAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class AuthHelper(private val context: Context) {

    private val customScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

    companion object {
        private const val PREF_NAME = "MyPreferences"
        private const val KEY_LOGIN_TIMESTAMP = "LOGIN_TIMESTAMP"
        private const val KEY_AUTH_TOKEN = "jwt"
    }

    private val sharedPref: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isLoggedIn(): Boolean {
        val loginTimestamp = sharedPref.getLong(KEY_LOGIN_TIMESTAMP, -1)
        val jwt_token = sharedPref.getString(KEY_AUTH_TOKEN,"")

        if (loginTimestamp == -1L || jwt_token == "") {
            return false
        }

        val daysSinceLogin = getDaysSinceLogin(loginTimestamp)
        val maxDaysLoggedIn = 7
        return (daysSinceLogin < maxDaysLoggedIn && jwt_token != "")
    }

    suspend fun login(identifiant: String, password: String): Boolean {
        val credentials = mapOf<String,String>(
            "username" to identifiant,
            "password" to password
        )
        val editor = sharedPref.edit()
        try{
            var api = RetrofitInstance.api.loginUser(credentials)
            if(api.code() == 200) {
                val loginTimestamp = System.currentTimeMillis()
                val jwt_token = api.body()?.get("jwt")
                editor.putLong(KEY_LOGIN_TIMESTAMP, loginTimestamp)
                editor.putString(KEY_AUTH_TOKEN, jwt_token)
                editor.apply()
                return true
            } else {
                Log.e("MainActivity2", "Status Code: ${api.code()}")
                return false
            }
        } catch (e: Exception) {
            Log.e("MainActivity2", "Error fetching auth token: $e")
            return false
        }
    }

    fun logout() {
        var Scheduler = AndroidAlarmScheduler(context)
        val scope = if (context is androidx.lifecycle.LifecycleOwner) {
            context.lifecycleScope
        } else {
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
        }
        scope.launch(Dispatchers.IO) {
            Scheduler.deinitialize()
            val editor = sharedPref.edit()
            editor.remove(KEY_LOGIN_TIMESTAMP)
            editor.remove(KEY_AUTH_TOKEN)
            editor.apply()
        }
    }

    private fun getDaysSinceLogin(loginTimestamp: Long): Long {
        val currentTime = System.currentTimeMillis()
        return (currentTime - loginTimestamp) / (24 * 60 * 60 * 1000) // Milliseconds to days conversion
    }
}
