package com.example.automatism.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.automatism.R
import com.example.automatism.database.AppDatabase
import com.example.automatism.database.models.Device
import com.example.automatism.database.models.Schedule
import com.example.automatism.databinding.SchedulesActivityBinding
import com.example.automatism.utils.RetrofitInstance
import com.example.automatism.utils.alarm.AlarmItem
import com.example.automatism.utils.alarm.AndroidAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SchedulesActivity : AppCompatActivity() {

    private lateinit var binding: SchedulesActivityBinding
    private lateinit var scheduleAdapter: ScheduleAdapter
    private var dataList: MutableList<Schedule> = mutableListOf()
    private lateinit var database: AppDatabase
    private lateinit var Scheduler: AndroidAlarmScheduler
    private lateinit var myPreferences: SharedPreferences


    override fun onResume() {
        super.onResume()
        Log.d("MainActivity2","Activated On Resume for schedules")
        scheduleAdapter.updateData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SchedulesActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.i("MainActivity2","${intent.getLongExtra("device_id", -1L)}")

        myPreferences = this.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)

        binding.fab.setOnClickListener {
            onAddButton()
        }

        // Database Configuration:
        database = AppDatabase.getInstance(this)
        val scheduleDao = database.scheduleDao()

        // Alarm Scheduler Initialization:
        Scheduler = AndroidAlarmScheduler(this)

        // Data Set (for first boot)
        scheduleAdapter = ScheduleAdapter(this, dataList)
        binding.schedulesListview.adapter = scheduleAdapter

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if(myPreferences.getBoolean("USER_ACTIVE",false)){
                    fetchNewSchedules()
                }
                    runOnUiThread {
                        Log.e("MainActivity2","Good Initialization")
                        scheduleAdapter.updateData()
                    }
            } catch (e: Exception) {
                Log.e("MainActivity2","error in onCreate: $e")
            }
        }
    }

    fun onAddButton() {
        val deviceId = intent.getLongExtra("device_id", -1L)
        val intent = Intent(this, AddScheduleActivity::class.java)
        intent.putExtra("device_id", deviceId)
        startActivity(intent)
    }

    /* private suspend fun fetchSchedules() {
        try {
            val scheduleDao = database.scheduleDao()
            val schedules = scheduleDao.getAllSchedules()
            scheduleAdapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e("SchedulesActivity", "Error fetching schedules: $e")
        }
    } */

    private suspend fun fetchNewSchedules() {
        try {
            val authToken = myPreferences.getString("jwt", "")
            val deviceId = intent.getLongExtra("device_id", -1L)
            if (authToken.isNullOrEmpty()) {
                return
            }
            var responseOfHttp = RetrofitInstance.api.getSchedulesForDeviceId(
                deviceId = deviceId,
                authToken = authToken
            )
            if (responseOfHttp.code() == 200) {
                val schedules = responseOfHttp.body()?.get("data")
                if(schedules != null) {
                    Log.d("MainActivity2", "Devices are not null")
                }
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        loadNewSchedules(schedules = schedules!!)
                    } catch (e: Exception) {
                        Log.e("MainActivity2","Loading Data Error: $e")
                    }
                }
            } else {
                Log.e("MainActivity2", "Status Code: ${responseOfHttp.code()}")
            }
        } catch (e: Exception) {
            Log.e("MainActivity2","From Fetch New Devices: $e")
        }
    }

    private suspend fun loadNewSchedules(schedules: List<Map<String,Any>>){
        scheduleAdapter.updateData()

        val scheduleDao = database.scheduleDao()

        val current_user = myPreferences.getLong("CURRENT_USER_ID", -1L)

        val listIds = schedules.map { (it["id"] as Double).toLong() }

        // Step 1: Cancellation of All Alarms (of user)
        Scheduler.deinitialize(current_user)

        // Step 2: Delete non-affected Devices despite having schedules
        scheduleDao.deleteSchedulesByNotInIds(listIds)

        // Step 3: Insert/Upsert (New) Devices
        for(schedule in schedules) {
            // val deviceStatus: Boolean? = deviceDao.getDeviceStatusById((device["id"] as Number).toLong())
            // val statusValue = if (deviceStatus != null) deviceStatus else false
            try{
                Log.i("MainActivity2","BBB:${(schedule["id"] as Number).toLong()}")
                Log.i("MainActivity2","${schedule}")

                val frequency = (if ((schedule["frequency"] as Number).toInt() == 0) null else (schedule["frequency"] as Number).toInt())
                val isActivatedRaw = scheduleDao.getIsActivatedStatusById((schedule["id"]!! as Number).toLong())
                val isActivatedLocal = (if (isActivatedRaw != null) isActivatedRaw else true)
                val isActivatedOnline = if (schedule["isActivated"] != null) (schedule["isActivated"] as Boolean) else true
                val isActivated = (if (isActivatedLocal == false || isActivatedOnline == false) false else true)


                // TODO("DEVICE_ID is changed to ID (which is affecter_id")
                val structuredDeviceMap = mapOf<String,Any?>(
                    "id" to (schedule["id"]!! as Number).toLong(),
                    "name" to schedule["name"]!!,
                    "minute_on" to (schedule["minute_on"]!! as Number).toInt(),
                    "hour_on" to (schedule["heure_on"]!! as Number).toInt(),
                    "minute_off" to (schedule["minute_off"]!! as Number).toInt(),
                    "hour_off" to (schedule["heure_off"]!! as Number).toInt(),
                    "frequency" to frequency,
                    "device" to (schedule["affecter_id"]!! as Number).toLong(),
                    "activated" to isActivated
                )
                scheduleDao.upsertSchedule(Schedule.fromMap(structuredDeviceMap))
            } catch (e: Exception) {
                Log.e("MainActivity2","some error here in loading: $e")
            }
        }
        // Step 4: Initialize All Alarms (from the Schedules Table)
        // TODO("Change from Initialize ALL to Initialize By UserID")
        Scheduler.initialize(current_user)
        Log.i("MainActivity2","Loaded Data from Fetch SUCCESSFULLY")

        // Step 5: Signal adapter to change (fetch from local DB) ListView Items
        scheduleAdapter.updateData()
    }

    private class ScheduleAdapter(
        private val context: Context,
        private var mDataList: MutableList<Schedule>
    ) : BaseAdapter() {

        override fun getCount(): Int {
            return mDataList.size
        }

        override fun getItem(position: Int): Any {
            return mDataList[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
            val layoutInflater = LayoutInflater.from(context)
            val rowMain = layoutInflater.inflate(R.layout.schedule_listview_row, viewGroup, false)

            val textName = rowMain.findViewById<TextView>(R.id.textName)
            val textTimeOff = rowMain.findViewById<TextView>(R.id.textTimeOff)
            val textTimeOn = rowMain.findViewById<TextView>(R.id.textTimeOn)
            val textInterval = rowMain.findViewById<TextView>(R.id.textChaque24h)
            val textActive = rowMain.findViewById<TextView>(R.id.textActive)
            val btnEdit = rowMain.findViewById<ImageButton>(R.id.btnEdit)
            val btnDelete = rowMain.findViewById<ImageButton>(R.id.btnDelete)

            val schedule = mDataList[position]

            textName.text = schedule.name
            textTimeOff.text = "Time Off: ${schedule.hour_off}:${schedule.minute_off}"
            textTimeOn.text = "Time On: ${schedule.hour_on}:${schedule.minute_on}"
            textInterval.text = "Chaque-24h: ${ if (schedule.frequency != null) "Oui" else "Non"}"
            textActive.text = "Active: ${ if (schedule.activated) "Oui" else "Non"}"

            btnDelete.setOnClickListener {
                deleteSchedule(schedule)
            }

            btnEdit.setOnClickListener {
                try{
                    editSchedule(schedule)
                } catch (e: Exception) {
                    Log.e("MainActivity2","Error Edit schedule: $e")
                }
            }

            return rowMain
        }

        private fun editSchedule(schedule: Schedule) {
            try {
                Intent(context, ModifyScheduleActivity::class.java).also {
                    it.putExtra("schedule_id", schedule.id)
                    context.startActivity(it)
                }
            } catch (e: Exception) {
                Log.e("MainActivity2","Error Accessing Edit-Schedule ${schedule.id} $e")
            }
        }
        private fun deleteSchedule(schedule: Schedule) {
            (context as AppCompatActivity).lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val scheduleDao = AppDatabase.getInstance(context).scheduleDao()
                    val Scheduler = (context as SchedulesActivity).Scheduler

                    if((context as SchedulesActivity).myPreferences.getBoolean("USER_ACTIVE",false)){
                        var api = RetrofitInstance.api.deleteSchedule(
                            reglageId = schedule.id,
                            authToken = (context as SchedulesActivity).myPreferences.getString("jwt","")!!
                        )
                        Log.d("MainActivity2","Deleted Schedule (FETCH CALL)")
                    }

                    val deviceSchedule = scheduleDao.getScheduleAndDeviceByScheduleId(schedule.id)

                    Scheduler.cancel(
                        AlarmItem(
                            time = mapOf(
                                "hour" to deviceSchedule.schedule.hour_on,
                                "minute" to deviceSchedule.schedule.minute_on
                            ),
                            frequency = deviceSchedule.schedule.frequency,
                            telephone = deviceSchedule.device.telephone,
                            messageOn = deviceSchedule.device.msg_on,
                            messageOff = deviceSchedule.device.msg_off,
                            action = true,
                            deviceId = deviceSchedule.device.id,
                            userId = deviceSchedule.device.user_id,
                            scheduleId = deviceSchedule.schedule.id
                        )
                    )

                    Scheduler.cancel(
                        AlarmItem(
                            time = mapOf(
                                "hour" to deviceSchedule.schedule.hour_off,
                                "minute" to deviceSchedule.schedule.minute_off
                            ),
                            frequency = deviceSchedule.schedule.frequency,
                            telephone = deviceSchedule.device.telephone,
                            messageOn = deviceSchedule.device.msg_on,
                            messageOff = deviceSchedule.device.msg_off,
                            action = false,
                            deviceId = deviceSchedule.device.id,
                            userId = deviceSchedule.device.user_id,
                            scheduleId = deviceSchedule.schedule.id
                        )
                    )

                    scheduleDao.deleteSchedule(schedule)
                    Log.d("MainActivity2", "Removal of 2 alarms (on/off)")
                    updateData()
                } catch (e: Exception) {
                    (context as AppCompatActivity).runOnUiThread {
                        Toast.makeText(context,"Error on Delete, Cannot Access the server", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("MainActivity2", "Error deleting schedule: $e")
                }
            }
        }

        fun updateData() {
            Log.d("MainActivity2","update status: notify Data set changed")
            val scheduleDao = AppDatabase.getInstance(context).scheduleDao()
            GlobalScope.launch(Dispatchers.IO){
                try {
                    mDataList = scheduleDao.getSchedulesByDeviceId(
                        (context as AppCompatActivity).
                            intent.getLongExtra("device_id", -1L)
                    ) as MutableList<Schedule>
                    // Update the UI on the main thread
                    (context as AppCompatActivity).runOnUiThread {
                        notifyDataSetChanged()
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity2", "Error status: idk $e")
                }
            }
        }

    }
}
