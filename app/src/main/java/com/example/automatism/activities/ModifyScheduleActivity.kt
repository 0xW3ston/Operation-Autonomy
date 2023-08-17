package com.example.automatism.activities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.automatism.database.AppDatabase
import com.example.automatism.database.models.Device
import com.example.automatism.database.models.Schedule
import com.example.automatism.database.models.ScheduleDevice
import com.example.automatism.databinding.ScheduleEditActivityBinding
import com.example.automatism.utils.RetrofitInstance
import com.example.automatism.utils.TimeCalculationClass
import com.example.automatism.utils.alarm.AlarmItem
import com.example.automatism.utils.alarm.AndroidAlarmScheduler
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ModifyScheduleActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var devicesList: List<Device>
    private lateinit var scheduleDevice: ScheduleDevice
    private lateinit var myPreferences: SharedPreferences
    private var timecalc: TimeCalculationClass = TimeCalculationClass
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        myPreferences = this.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)

        try {
            // Use data binding to set up the layout
            val binding: ScheduleEditActivityBinding =
                ScheduleEditActivityBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val Scheduler = AndroidAlarmScheduler(this)

            // Database
            database = AppDatabase.getInstance(this)
            val deviceDao = database.deviceDao()
            val scheduleDao = database.scheduleDao()

            // Set up time input validation
            setupTimeInputValidation(
                binding.modifyHourOnInputLayout,
                binding.modifyEditTextHourOn,
                0,
                23
            )
            setupTimeInputValidation(
                binding.modifyHourOffInputLayout,
                binding.modifyEditTextHourOff,
                0,
                23
            )
            setupTimeInputValidation(
                binding.modifyMinuteOnInputLayout,
                binding.modifyEditTextMinuteOn,
                0,
                59
            )
            setupTimeInputValidation(
                binding.modifyMinuteOffInputLayout,
                binding.modifyEditTextMinuteOff,
                0,
                59
            )

            // Set up frequency slider visibility based on checkbox
            /* binding.modifyCheckboxFrequency.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    binding.modifyFrequencyLabel.visibility = View.GONE
                    binding.modifyFrequencySlider.visibility = View.GONE
                } else {
                    binding.modifyFrequencyLabel.visibility = View.VISIBLE
                    binding.modifyFrequencySlider.visibility = View.VISIBLE
                }
            } */

            // Set up frequency slider label
            /* binding.modifyFrequencySlider.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    binding.modifyFrequencyLabel.text = "Frequency (in hours): $progress"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }) */

            lifecycleScope.launch(Dispatchers.IO) {
                // Populate deviceComboBox with device names (replace with actual data)
                devicesList = deviceDao.getAllDevices()
                val deviceNames = devicesList.map { it.name }
                val adapter = ArrayAdapter(
                    this@ModifyScheduleActivity,
                    android.R.layout.simple_spinner_item,
                    deviceNames
                )
                binding.modifyDeviceComboBox.adapter = adapter
            }

            val scheduleId = intent.getLongExtra("schedule_id", -1L)
            if (scheduleId != -1L) {
                lifecycleScope.launch(Dispatchers.IO) {
                    scheduleDevice = scheduleDao.getScheduleAndDeviceByScheduleId(scheduleId)
                    val schedule = scheduleDevice.schedule

                    runOnUiThread {
                        if(schedule.frequency == null) {
                            binding.modifyCheckboxFrequency.isChecked = false
                            // binding.modifyFrequencyLabel.visibility = View.GONE
                            // binding.modifyFrequencySlider.visibility = View.GONE
                        } else if(schedule.frequency == 24) {
                            binding.modifyCheckboxFrequency.isChecked = true
                        }

                        binding.modifyEditTextName.setText(schedule.name)
                        val selectedDeviceIndex =
                            devicesList.indexOfFirst { it.id == schedule.device }
                        if (selectedDeviceIndex != -1) {
                            binding.modifyDeviceComboBox.setSelection(selectedDeviceIndex)
                        }
                        binding.modifyEditTextHourOn.setText(schedule.hour_on.toString())
                        binding.modifyEditTextMinuteOn.setText(schedule.minute_on.toString())
                        binding.modifyEditTextHourOff.setText(schedule.hour_off.toString())
                        binding.modifyEditTextMinuteOff.setText(schedule.minute_off.toString())
                        // TODO("Fix This, The Frequency can be set to null or it can be an Int")
                        // binding.modifyFrequencySlider.progress = schedule.frequency
                    }
                }
            }

            // Implement the logic to update the schedule and handle the submit button click event here.
            binding.modifyBtnSubmit.setOnClickListener {
                // Check the state of the checkbox
                val isPeriodic = binding.modifyCheckboxFrequency.isChecked

                val schedule = scheduleDevice.schedule
                val device = scheduleDevice.device
                val name = binding.modifyEditTextName.text.toString()
                val deviceId = devicesList[(binding.modifyDeviceComboBox.selectedItemId).toInt()].id
                val hourOn = binding.modifyEditTextHourOn.text.toString().toInt()
                val minuteOn = binding.modifyEditTextMinuteOn.text.toString().toInt()
                val hourOff = binding.modifyEditTextHourOff.text.toString().toInt()
                val minuteOff = binding.modifyEditTextMinuteOff.text.toString().toInt()
                val frequency = if (isPeriodic) 24 else null

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        var updatedSchedule = Schedule(
                            id = schedule.id,
                            name = name,
                            hour_on = hourOn,
                            minute_on = minuteOn,
                            hour_off = hourOff,
                            minute_off = minuteOff,
                            frequency = frequency,
                            device = deviceId,
                            date_initial = timecalc.calculateInitialDelay(hourOn, minuteOn)
                        )
                        if(myPreferences.getBoolean("USER_ACTIVE",false)){
                            var authToken = myPreferences.getString("jwt","")

                            var forFetchModifiedSchedule = updatedSchedule
                            forFetchModifiedSchedule.frequency = if (frequency == null) 0 else frequency

                            var api = RetrofitInstance.api.modifierSchedule(
                                scheduleId = schedule.id,
                                authToken = authToken!!,
                                requestBody = updatedSchedule
                            )

                            if(api.isSuccessful && api.code() == 200) {
                                forFetchModifiedSchedule.frequency = frequency
                                scheduleDao.updateSchedule(updatedSchedule)
                            } else {
                                throw Exception("Schedule Modification Failed: Server Error")
                            }
                        } else {
                            scheduleDao.updateSchedule(updatedSchedule)
                        }
                        Log.w("MainActivity2", "After Update")
                        // Schedule Cancellation:

                        Scheduler.cancel(
                            AlarmItem(
                                time = mapOf(
                                    "hour" to schedule.hour_on,
                                    "minute" to schedule.minute_on
                                ),
                                frequency = schedule.frequency,
                                telephone = device.telephone,
                                messageOn = device.msg_on,
                                messageOff = device.msg_off,
                                action = true,
                                deviceId = device.id,
                                userId = device.user_id,
                                scheduleId = scheduleId
                            )
                        )

                        Scheduler.cancel(
                            AlarmItem(
                                time = mapOf(
                                    "hour" to schedule.hour_off,
                                    "minute" to schedule.minute_off
                                ),
                                frequency = schedule.frequency,
                                telephone = device.telephone,
                                messageOn = device.msg_on,
                                messageOff = device.msg_off,
                                action = false,
                                deviceId = device.id,
                                userId = device.user_id,
                                scheduleId = scheduleId
                            )
                        )

                        // Schedule Reschudeling:

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
                                userId = device.user_id,
                                scheduleId = scheduleId
                            ),
                            true
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
                                userId = device.user_id,
                                scheduleId = scheduleId
                            ),
                            true
                        )

                        Log.w("MainActivity2", "After Reschudeling Good:")

                        runOnUiThread {
                            Toast.makeText(
                                this@ModifyScheduleActivity,
                                "Schedule Updated",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        finish()
                    } catch (e: Exception) {
                        Log.e("ModifyScheduleActivity", "Schedule Error: $e")
                        runOnUiThread {
                            Toast.makeText(
                                this@ModifyScheduleActivity,
                                "Error Updating Schedule: $e",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity2","Error On create edit schedule: $e")
        }
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
