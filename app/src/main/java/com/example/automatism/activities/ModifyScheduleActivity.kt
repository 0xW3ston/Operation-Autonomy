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
                binding.modifyHourInputLayout,
                binding.modifyEditTextHour,
                0,
                23
            )
            setupTimeInputValidation(
                binding.modifyMinuteInputLayout,
                binding.modifyEditTextMinute,
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
                // Create an array of action options and their corresponding values
                val actionOptions = arrayOf("Turn Off", "Turn On")
                val actionValues = intArrayOf(0, 1)

                // Create an ArrayAdapter for the actionSpinner
                val actionAdapter = ArrayAdapter(this@ModifyScheduleActivity , android.R.layout.simple_spinner_item, actionOptions)

                // Set the adapter for the actionSpinner
                binding.modifyActionSpinner.adapter = actionAdapter
            }

            val scheduleId = intent.getLongExtra("schedule_id", -1L)
            if (scheduleId != -1L) {
                lifecycleScope.launch(Dispatchers.IO) {
                    scheduleDevice = scheduleDao.getScheduleAndDeviceByScheduleId(scheduleId)
                    val schedule = scheduleDevice.schedule

                    runOnUiThread {
                        if(schedule.frequency == null) {
                            binding.modifyNoRepeatRadioButton.isChecked = true
                        } else if(schedule.frequency != 24) {
                            binding.modifyOneHourRepeatRadioButton.isChecked = true
                        } else if(schedule.frequency == 24) {
                            binding.modifyTwentyFourHourRepeatRadioButton.isChecked = true
                        }

                        binding.modifyEditTextName.setText(schedule.name)
                        val selectedDeviceIndex =
                            devicesList.indexOfFirst { it.id == schedule.device }
                        if (selectedDeviceIndex != -1) {
                            binding.modifyDeviceComboBox.setSelection(selectedDeviceIndex)
                        }
                        binding.modifyEditTextHour.setText(if (schedule.hour_on != null) schedule.hour_on.toString() else schedule.hour_off.toString())
                        binding.modifyEditTextMinute.setText(if (schedule.minute_on != null) schedule.minute_on.toString() else schedule.minute_off.toString())
                        // TODO("Fix This, The Frequency can be set to null or it can be an Int")
                        // binding.modifyFrequencySlider.progress = schedule.frequency
                    }
                }
            }

            // Implement the logic to update the schedule and handle the submit button click event here.
            binding.modifyBtnSubmit.setOnClickListener {
                // Check the state of the checkbox

                val frequency = if (binding.modifyNoRepeatRadioButton.isChecked){
                    null
                } else if(binding.modifyOneHourRepeatRadioButton.isChecked == true) {
                    1
                } else if(binding.modifyTwentyFourHourRepeatRadioButton.isChecked == true) {
                    24
                } else {
                    null
                }

                val schedule = scheduleDevice.schedule
                val device = scheduleDevice.device
                val name = binding.modifyEditTextName.text.toString()
                val deviceId = devicesList[(binding.modifyDeviceComboBox.selectedItemId).toInt()].id
                val hour = binding.modifyEditTextHour.text.toString().toInt()
                val minute = binding.modifyEditTextMinute.text.toString().toInt()

                val actionOptions = arrayOf("Turn Off", "Turn On")
                val actionValues = intArrayOf(0, 1)
                val selectedItemPosition = binding.modifyActionSpinner.selectedItemPosition
                val selectedValue = actionValues[selectedItemPosition]

                val initial_date = timecalc.calculateInitialDelay(hour, minute)

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        var updatedSchedule = Schedule(
                            id = schedule.id,
                            name = name,
                            hour_on = if (selectedItemPosition == 1) hour else null,
                            minute_on = if (selectedItemPosition == 1) minute else null,
                            hour_off = if (selectedItemPosition == 0) hour else null,
                            minute_off = if (selectedItemPosition == 0) minute else null,
                            frequency = frequency,
                            device = deviceId,
                            date_initial = initial_date
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

                        if (schedule.hour_on != null && schedule.minute_on != null){
                            Scheduler.cancel(
                                AlarmItem(
                                    time = mapOf(
                                        "hour" to schedule.hour_on!!,
                                        "minute" to schedule.minute_on!!
                                    ),
                                    frequency = schedule.frequency,
                                    telephone = device.telephone,
                                    messageOn = device.msg_on,
                                    messageOff = device.msg_off,
                                    action = true,
                                    deviceId = device.id,
                                    userId = device.user_id,
                                    scheduleId = scheduleId,
                                    dateInitial = initial_date
                                )
                            )
                        }

                        if (schedule.hour_off != null && schedule.minute_off != null){
                            Scheduler.cancel(
                                AlarmItem(
                                    time = mapOf(
                                        "hour" to schedule.hour_off!!,
                                        "minute" to schedule.minute_off!!
                                    ),
                                    frequency = schedule.frequency,
                                    telephone = device.telephone,
                                    messageOn = device.msg_on,
                                    messageOff = device.msg_off,
                                    action = false,
                                    deviceId = device.id,
                                    userId = device.user_id,
                                    scheduleId = scheduleId,
                                    dateInitial = initial_date
                                )
                            )
                        }

                        // Schedule Reschudeling:

                        if (selectedItemPosition == 1){
                            Scheduler.schedule(
                                AlarmItem(
                                    time = mapOf(
                                        "hour" to hour,
                                        "minute" to minute
                                    ),
                                    frequency = frequency,
                                    telephone = device.telephone,
                                    messageOn = device.msg_on,
                                    messageOff = device.msg_off,
                                    action = true,
                                    deviceId = deviceId,
                                    userId = device.user_id,
                                    scheduleId = scheduleId,
                                    dateInitial = initial_date
                                ),
                                true
                            )
                        }

                        if (selectedItemPosition == 0){
                            Scheduler.schedule(
                                AlarmItem(
                                    time = mapOf(
                                        "hour" to hour,
                                        "minute" to minute
                                    ),
                                    frequency = frequency,
                                    telephone = device.telephone,
                                    messageOn = device.msg_on,
                                    messageOff = device.msg_off,
                                    action = false,
                                    deviceId = deviceId,
                                    userId = device.user_id,
                                    scheduleId = scheduleId,
                                    dateInitial = initial_date
                                ),
                                true
                            )
                        }

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
