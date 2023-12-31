package com.example.automatism.activities

import android.content.Context
import android.content.SharedPreferences
import com.example.automatism.database.models.Device
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.automatism.database.AppDatabase
import com.example.automatism.database.models.Schedule
import com.example.automatism.databinding.ScheduleAddActivityBinding
import com.example.automatism.utils.AuthHelper
import com.example.automatism.utils.RetrofitInstance
import com.example.automatism.utils.TimeCalculationClass
import com.example.automatism.utils.alarm.AlarmItem
import com.example.automatism.utils.alarm.AndroidAlarmScheduler
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AddScheduleActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var devicesList: List<Device>
    private lateinit var myPreferences: SharedPreferences
    private var timecalc: TimeCalculationClass = TimeCalculationClass
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        myPreferences = this.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)

        // Use data binding to set up the layout
        val binding: ScheduleAddActivityBinding =
            ScheduleAddActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val Scheduler = AndroidAlarmScheduler(this)

        // Database
        database = AppDatabase.getInstance(this)
        val deviceDao = database.deviceDao()
        val scheduleDao = database.scheduleDao()

        // Set up time input validation
        setupTimeInputValidation(binding.hourOnInputLayout, binding.editTextHourOn, 0, 23)
        setupTimeInputValidation(binding.hourOffInputLayout, binding.editTextHourOff, 0, 23)
        setupTimeInputValidation(binding.minuteOnInputLayout, binding.editTextMinuteOn, 0, 59)
        setupTimeInputValidation(binding.minuteOffInputLayout, binding.editTextMinuteOff, 0, 59)

        // Set up frequency slider label
        /* binding.frequencySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.frequencyLabel.text = "Frequency (in hours): $progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }) */

        /* binding.checkboxFrequency.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.frequencySlider.visibility = View.GONE
            } else {
                binding.frequencySlider.visibility = View.VISIBLE
            }
        } */

        // Implement the logic to save the schedule and handle the submit button click event here.
        val deviceId = intent.getLongExtra("device_id", -1L)
        binding.btnSubmit.setOnClickListener {
            // val id = binding.editTextId.text.toString().toLong()
            val name = binding.editTextName.text.toString()
            val hourOn = binding.editTextHourOn.text.toString().toInt()
            val minuteOn = binding.editTextMinuteOn.text.toString().toInt()
            val hourOff = binding.editTextHourOff.text.toString().toInt()
            val minuteOff = binding.editTextMinuteOff.text.toString().toInt()
            // val frequency = if (binding.checkboxFrequency.isChecked) null else binding.frequencySlider.progress.toInt()
            val frequency = if (binding.checkboxFrequency.isChecked) {
                24
            } else {
                null
            }
            val current_user_id = myPreferences.getLong("CURRENT_USER_ID", -1L)
            lifecycleScope.launch(Dispatchers.IO) {
                var scheduleId = -1L
                try {
                    val device = deviceDao.getDeviceById(deviceId)
                    val test = Schedule(
                        name = name,
                        hour_on = hourOn,
                        minute_on = minuteOn,
                        hour_off = hourOff,
                        minute_off = minuteOff,
                        frequency = frequency,
                        device = deviceId,
                        date_initial = timecalc.calculateInitialDelay(hourOn, minuteOn)
                    )
                    Log.d("MainActivity2","${test}")

                    if(myPreferences.getBoolean("USER_ACTIVE",false)){
                        val newSchedule = Schedule(
                            name = name,
                            hour_on = hourOn,
                            minute_on = minuteOn,
                            hour_off = hourOff,
                            minute_off = minuteOff,
                            frequency = frequency ?: 0,
                            device = deviceId,
                            date_initial = timecalc.calculateInitialDelay(hourOn, minuteOn)
                        )
                        var api = RetrofitInstance.api.addNewSchedule(
                            authToken = myPreferences.getString("jwt","")!!,
                            requestBody = newSchedule
                        )
                        Log.d("MainActivity2", "api successful or not ${api.isSuccessful}, if yes ${api.code()} and if yes ${api.body()}")
                        if(api.isSuccessful && api.code() == 200){
                            val insertedScheduleId = api.body()?.get("insertedId")
                            Log.d("MainActivity2","insertedId: ${insertedScheduleId}")
                            scheduleId = scheduleDao.insertSchedule(
                                Schedule(
                                    id = (insertedScheduleId as Double).toLong(),
                                    name = name,
                                    hour_on = hourOn,
                                    minute_on = minuteOn,
                                    hour_off = hourOff,
                                    minute_off = minuteOff,
                                    frequency = frequency,
                                    device = deviceId,
                                    date_initial = timecalc.calculateInitialDelay(hourOn, minuteOn)
                                )
                            )
                            Scheduler.schedule(
                                AlarmItem(
                                    time = mapOf(
                                        "hour" to hourOn,
                                        "minute" to minuteOn
                                    ),
                                    frequency = frequency,
                                    telephone = device.telephone,
                                    messageOn = device.msg_on,
                                    messageOff = device.msg_off,
                                    action = true,
                                    deviceId = deviceId,
                                    userId = current_user_id,
                                    scheduleId = scheduleId
                                ),
                                isInitial = true
                            )

                            Scheduler.schedule(
                                AlarmItem(
                                    time = mapOf(
                                        "hour" to hourOff,
                                        "minute" to minuteOff
                                    ),
                                    frequency = frequency,
                                    telephone = device.telephone,
                                    messageOn = device.msg_on,
                                    messageOff = device.msg_off,
                                    action = false,
                                    deviceId = deviceId,
                                    userId = current_user_id,
                                    scheduleId = scheduleId
                                ),
                                isInitial = true
                            )
                        } else {
                            throw Exception("Error Of Insertion: Server-Side")
                        }
                    } else {
                        scheduleId = scheduleDao.insertSchedule(
                            Schedule(
                                name = name,
                                hour_on = hourOn,
                                minute_on = minuteOn,
                                hour_off = hourOff,
                                minute_off = minuteOff,
                                frequency = frequency,
                                device = deviceId,
                                date_initial = timecalc.calculateInitialDelay(hourOn, minuteOn)
                            )
                        )

                        Scheduler.schedule(
                            AlarmItem(
                                time = mapOf(
                                    "hour" to hourOn,
                                    "minute" to minuteOn
                                ),
                                frequency = frequency,
                                telephone = device.telephone,
                                messageOn = device.msg_on,
                                messageOff = device.msg_off,
                                action = true,
                                deviceId = deviceId,
                                userId = current_user_id,
                                scheduleId = scheduleId
                            ),
                            isInitial = true
                        )

                        Scheduler.schedule(
                            AlarmItem(
                                time = mapOf(
                                    "hour" to hourOff,
                                    "minute" to minuteOff
                                ),
                                frequency = frequency,
                                telephone = device.telephone,
                                messageOn = device.msg_on,
                                messageOff = device.msg_off,
                                action = false,
                                deviceId = deviceId,
                                userId = current_user_id,
                                scheduleId = scheduleId
                            ),
                            isInitial = true
                        )
                    }

                    runOnUiThread {
                        Toast.makeText(
                            this@AddScheduleActivity,
                            "Schedule Saved",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    finish()
                } catch (e: Exception) {
                    Log.e("MainActivity2","Schedule Error: $e")
                    runOnUiThread {
                        Toast.makeText(
                            this@AddScheduleActivity,
                            "Error Schedule: $e",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun addSchedule(){
        TODO("Tired as hell")
    }
    // Time input validation function
    private fun setupTimeInputValidation(
        inputLayout: TextInputLayout,
        editText: TextInputEditText,
        minValue: Int,
        maxValue: Int
    ) {
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val timeValue = editText.text.toString().toIntOrNull()
                if (timeValue == null || timeValue < minValue || timeValue > maxValue) {
                    inputLayout.error = "Invalid time value (range $minValue-$maxValue)"
                } else {
                    inputLayout.error = null
                }
            }
        }
    }
}
