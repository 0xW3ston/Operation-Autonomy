package com.example.automatism.utils


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.automatism.database.models.Device
import kotlinx.coroutines.delay

object SMSManager {

    fun sendSMS(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isSMSPermissionGranted(context: Context): Boolean {
        Log.d("MainActivity2","In Get Permission")
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestSMSPermission(activity: AppCompatActivity) {
        val SMS_PERMISSION_REQUEST_CODE = 123

        Log.d("MainActivity2","In Get Permission")
        if (!isSMSPermissionGranted(activity)) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_REQUEST_CODE
            )
        }
    }

     suspend fun sendConfigSMS(device: Device) {
        val config = device.config
        val smsData = config.split("/")
        for (smsInfo in smsData) {
            val (message, delayInSeconds) = smsInfo.split(":")
            try {
                sendSMS(device.telephone, message)
                Log.i("MainActivity2","[CONFIG MESSAGE]: \"${message}\", [SENT AT]: ${System.currentTimeMillis() / 1000} ")
                val delayInMillis = delayInSeconds.toLong() * 1000
                delay(delayInMillis) // Delay for the specified time
            } catch (e: Exception) {
                Log.e("DevicesActivity", "Error sending SMS: $e")
            }
        }
    }

}
