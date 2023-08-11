package com.example.automatism.activities

import com.example.automatism.database.models.Device
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.automatism.database.AppDatabase
import com.example.automatism.database.models.Schedule
import com.example.automatism.databinding.ScheduleAddActivityBinding
import com.example.automatism.utils.alarm.AlarmItem
import com.example.automatism.utils.alarm.AndroidAlarmScheduler
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddScheduleActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var devicesList: List<Device>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        binding.frequencySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.frequencyLabel.text = "Frequency (in hours): $progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        lifecycleScope.launch(Dispatchers.IO){
            // Populate deviceComboBox with device names (replace with actual data)
            devicesList = deviceDao.getAllDevices()
            val deviceNames = devicesList.map { it.name }
            val adapter = ArrayAdapter(this@AddScheduleActivity, android.R.layout.simple_spinner_item, deviceNames)
            binding.deviceComboBox.adapter = adapter
        }

        // Implement the logic to save the schedule and handle the submit button click event here.
        binding.btnSubmit.setOnClickListener {
            // val id = binding.editTextId.text.toString().toLong()
            val name = binding.editTextName.text.toString()
            val deviceId = devicesList[(binding.deviceComboBox.selectedItemId).toInt()].id // The selected device ID
            val hourOn = binding.editTextHourOn.text.toString().toInt()
            val minuteOn = binding.editTextMinuteOn.text.toString().toInt()
            val hourOff = binding.editTextHourOff.text.toString().toInt()
            val minuteOff = binding.editTextMinuteOff.text.toString().toInt()
            val frequency = binding.frequencySlider.progress.toInt()
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val device = deviceDao.getDeviceById(deviceId)

                    scheduleDao.insertSchedule(
                        Schedule(
                            name = name,
                            hour_on = hourOn,
                            minute_on = minuteOn,
                            hour_off = hourOff,
                            minute_off = minuteOff,
                            frequency = frequency,
                            device = deviceId
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
                            deviceId = deviceId
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
                            deviceId = deviceId
                        ),
                        isInitial = true
                    )

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
