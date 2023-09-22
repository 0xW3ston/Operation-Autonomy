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
        val deviceId = intent.getLongExtra("device_id", -1L)
        // Database
        database = AppDatabase.getInstance(this)
        val deviceDao = database.deviceDao()
        val scheduleDao = database.scheduleDao()

        // Set up time input validation
        setupTimeInputValidation(binding.hourInputLayout, binding.editTextHour, 0, 23)
        setupTimeInputValidation(binding.minuteInputLayout, binding.editTextMinute, 0, 59)

        binding.actionSpinner.setSelection(0);
        binding.noRepeatRadioButton.isChecked = true
        // Set up frequency slider label
        /* binding.frequencySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.frequencyLabel.text = "Frequency (in hours): $progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }) */

        lifecycleScope.launch(Dispatchers.IO){
            // Populate deviceComboBox with device names (replace with actual data)
            val current_user = myPreferences.getLong("CURRENT_USER_ID", -1L)

            // Create an array of action options and their corresponding values
            val actionOptions = arrayOf("Turn Off", "Turn On")
            val actionValues = intArrayOf(0, 1)

            // Create an ArrayAdapter for the actionSpinner
            val actionAdapter = ArrayAdapter(this@AddScheduleActivity , android.R.layout.simple_spinner_item, actionOptions)

            // Set the adapter for the actionSpinner
            binding.actionSpinner.adapter = actionAdapter
        }

        /* binding.checkboxFrequency.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.frequencySlider.visibility = View.GONE
            } else {
                binding.frequencySlider.visibility = View.VISIBLE
            }
        } */

        // Implement the logic to save the schedule and handle the submit button click event here.
        binding.btnSubmit.setOnClickListener {
            // val id = binding.editTextId.text.toString().toLong()
            val name = binding.editTextName.text.toString()
            val hourInput = binding.editTextHour.text.toString().toInt()
            val minuteInput = binding.editTextMinute.text.toString().toInt()

            val actionOptions = arrayOf("Turn Off", "Turn On")
            val actionValues = intArrayOf(0, 1)
            val selectedItemPosition = binding.actionSpinner.selectedItemPosition
            val selectedValue = actionValues[selectedItemPosition]

            // val frequency = if (binding.checkboxFrequency.isChecked) null else binding.frequencySlider.progress.toInt()
            val frequency = if (binding.noRepeatRadioButton.isChecked) {
                null
            } else if (binding.oneHourRepeatRadioButton.isChecked) {
                1
            } else if (binding.twentyFourHourRepeatRadioButton.isChecked) {
                24
            } else {
                null
            }

            val current_user_id = myPreferences.getLong("CURRENT_USER_ID", -1L)
            lifecycleScope.launch(Dispatchers.IO) {
                var scheduleId = -1L
                val initial_date = timecalc.calculateInitialDelay(hourInput, minuteInput)
                try {
                    val device = deviceDao.getDeviceById(deviceId)
                    val test = Schedule(
                        name = name,
                        hour_on = if (selectedItemPosition == 1) hourInput else null,
                        minute_on = if (selectedItemPosition == 1) minuteInput else null,
                        hour_off = if (selectedItemPosition == 0) hourInput else null,
                        minute_off = if (selectedItemPosition == 0) minuteInput else null,
                        frequency = frequency,
                        device = deviceId,
                        date_initial = initial_date
                    )
                    // TODO ("FIX INITIAL DELAY")
                    Log.d("MainActivity2","${test}")

                    if(myPreferences.getBoolean("USER_ACTIVE",false)){
                        val newSchedule = Schedule(
                            name = name,
                            hour_on = if (selectedItemPosition == 1) hourInput else null,
                            minute_on = if (selectedItemPosition == 1) minuteInput else null,
                            hour_off = if (selectedItemPosition == 0) hourInput else null,
                            minute_off = if (selectedItemPosition == 0) minuteInput else null,
                            frequency = frequency ?: 0,
                            device = deviceId,
                            date_initial = initial_date
                        )
                        // TODO ("FIX INITIAL DELAY")
                        Log.e("MainActivity2", "Neew: ${newSchedule}")
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
                                    hour_on = if (selectedItemPosition == 1) hourInput else null,
                                    minute_on = if (selectedItemPosition == 1) minuteInput else null,
                                    hour_off = if (selectedItemPosition == 0) hourInput else null,
                                    minute_off = if (selectedItemPosition == 0) minuteInput else null,
                                    frequency = frequency,
                                    device = deviceId,
                                    date_initial = initial_date
                                )
                                // TODO ("FIX INITIAL DELAY")
                            )

                            if (selectedItemPosition == 1){
                                Scheduler.schedule(
                                    AlarmItem(
                                        time = mapOf(
                                            "hour" to hourInput,
                                            "minute" to minuteInput
                                        ),
                                        frequency = frequency,
                                        telephone = device.telephone,
                                        messageOn = device.msg_on,
                                        messageOff = device.msg_off,
                                        action = true,
                                        deviceId = deviceId,
                                        userId = current_user_id,
                                        scheduleId = scheduleId,
                                        dateInitial = initial_date
                                    ),
                                    isInitial = true
                                )
                            }

                            if (selectedItemPosition == 0){
                                Scheduler.schedule(
                                    AlarmItem(
                                        time = mapOf(
                                            "hour" to hourInput,
                                            "minute" to minuteInput
                                        ),
                                        frequency = frequency,
                                        telephone = device.telephone,
                                        messageOn = device.msg_on,
                                        messageOff = device.msg_off,
                                        action = false,
                                        deviceId = deviceId,
                                        userId = current_user_id,
                                        scheduleId = scheduleId,
                                        dateInitial = initial_date
                                    ),
                                    isInitial = true
                                )
                            }

                        } else {
                            throw Exception("Error Of Insertion: Server-Side")
                        }
                    } else {
                        scheduleId = scheduleDao.insertSchedule(
                            Schedule(
                                name = name,
                                hour_on = if (selectedItemPosition == 1) hourInput else null,
                                minute_on = if (selectedItemPosition == 1) minuteInput else null,
                                hour_off = if (selectedItemPosition == 0) hourInput else null,
                                minute_off = if (selectedItemPosition == 0) minuteInput else null,
                                frequency = frequency,
                                device = deviceId,
                                date_initial = initial_date
                            )
                            // TODO ("FIX INITIAL DELAY")
                        )

                        if (selectedItemPosition == 1)
                        {
                            Scheduler.schedule(
                                AlarmItem(
                                    time = mapOf(
                                        "hour" to hourInput,
                                        "minute" to minuteInput
                                    ),
                                    frequency = frequency,
                                    telephone = device.telephone,
                                    messageOn = device.msg_on,
                                    messageOff = device.msg_off,
                                    action = true,
                                    deviceId = deviceId,
                                    userId = current_user_id,
                                    scheduleId = scheduleId,
                                    dateInitial = initial_date
                                ),
                                isInitial = true
                            )
                        }

                        if (selectedItemPosition == 0)
                        {
                            Scheduler.schedule(
                                AlarmItem(
                                    time = mapOf(
                                        "hour" to hourInput,
                                        "minute" to minuteInput
                                    ),
                                    frequency = frequency,
                                    telephone = device.telephone,
                                    messageOn = device.msg_on,
                                    messageOff = device.msg_off,
                                    action = false,
                                    deviceId = deviceId,
                                    userId = current_user_id,
                                    scheduleId = scheduleId,
                                    dateInitial = initial_date
                                ),
                                isInitial = true
                            )
                        }
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
