package com.example.automatism.activities

import android.content.Context
import android.content.Intent
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

        lifecycleScope.launch {
            runBlocking {
                loadSchedules()
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

    fun loadSchedules(){
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
            val btnEdit = rowMain.findViewById<ImageButton>(R.id.btnEdit)
            val btnDelete = rowMain.findViewById<ImageButton>(R.id.btnDelete)

            val schedule = mDataList[position]

            textName.text = schedule.name
            textTimeOff.text = "Time Off: ${schedule.hour_off}:${schedule.minute_off}"
            textTimeOn.text = "Time On: ${schedule.hour_on}:${schedule.minute_on}"

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
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val scheduleDao = AppDatabase.getInstance(context).scheduleDao()
                    val Scheduler = (context as SchedulesActivity).Scheduler


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
                            userId = deviceSchedule.device.user_id
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
                            userId = deviceSchedule.device.user_id
                        )
                    )

                    scheduleDao.deleteSchedule(schedule)
                    Log.d("MainActivity2", "Removal of 2 alarms (on/off)")
                    updateData()
                } catch (e: Exception) {
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
