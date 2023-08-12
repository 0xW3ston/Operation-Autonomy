package com.example.automatism.activities

import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.lifecycleScope
import com.example.automatism.R
import com.example.automatism.database.AppDatabase
import com.example.automatism.database.dao.DeviceDao
import com.example.automatism.database.models.Device
import com.example.automatism.database.models.Schedule
import com.example.automatism.databinding.DevicesActivityBinding
import com.example.automatism.utils.AuthHelper
import com.example.automatism.utils.RetrofitInstance
import com.example.automatism.utils.SMSManager
import com.example.automatism.utils.alarm.AndroidAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

class DevicesActivity : AppCompatActivity() {

    private lateinit var binding: DevicesActivityBinding
    private lateinit var authHelper: AuthHelper
    private lateinit var deviceAdapter: DeviceAdapter
    private var dataList: MutableList<Device> = mutableListOf()
    private lateinit var database: AppDatabase
    private lateinit var myPreferences: SharedPreferences
    private lateinit var Scheduler: AndroidAlarmScheduler

    override fun onResume() {
        super.onResume()
        deviceAdapter.updateData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DevicesActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Scheduler = AndroidAlarmScheduler(this)

        authHelper = AuthHelper(this)

        myPreferences = this.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)

        database = AppDatabase.getInstance(this)
        val deviceDao = database.deviceDao()

        deviceAdapter = DeviceAdapter(this, dataList)
        binding.devicesListview.adapter = deviceAdapter

        lifecycleScope.launch(Dispatchers.IO) {
            runOnUiThread {
                SMSManager.requestSMSPermission(this@DevicesActivity)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = ContextCompat.getSystemService(this, AlarmManager::class.java)
            if (alarmManager?.canScheduleExactAlarms() == false) {
                Intent().also { intent ->
                    intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    this.startActivity(intent)
                }
            }
        }

        /*
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Scheduler.deinitialize()
                Scheduler.initialize()
                Log.e("MainActivity2","Good Initialization")
                deviceAdapter.updateData()
                fetchNewDevices()
                /*
                runBlocking {
                    deviceAdapter.updateData()
                    fetchNewDevices()
                }*/
            } catch (e: Exception) {
                Log.e("MainActivity2","error in onCreate: $e")
            }
        }*/
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                runOnUiThread {
                    Log.e("MainActivity2","Good Initialization")
                    deviceAdapter.updateData()
                }
                fetchNewDevices()
            } catch (e: Exception) {
                Log.e("MainActivity2","error in onCreate: $e")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu,menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.oRefresh -> {
                try {
                    var numberX = Random.nextLong(from = 1000L, until = 1000000L)
                    lifecycleScope.launch {
                        try {
                            fetchNewDevices()
                        } catch (e: Exception) {
                            Log.e("MainActivity2", "Exception: $e")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity2","$e")
                }
            }
            R.id.oQuit -> {
                try {
                    authHelper.logout()
                } catch (e: Exception) {
                    Log.e("MainActivity2","Error Logout: $e")
                }
                Intent(this, LoginActivity::class.java).also {
                    startActivity(it)
                    finish()
                }
            }
        }
        return true
    }

    private class DeviceAdapter(context: Context,dataList: MutableList<Device>): BaseAdapter() {
        private val mContext: Context
        private var mDataList: MutableList<Device>
        private val database = AppDatabase.getInstance(context)
        private val deviceDao = database.deviceDao()

        init {
            mContext = context
            mDataList = dataList
        }

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
            val layoutInflater = LayoutInflater.from(mContext)
            val rowMain = layoutInflater.inflate(R.layout.device_listview_row, viewGroup, false)

            val device_model = rowMain.findViewById<TextView>(R.id.textTitle)
            val device_description = rowMain.findViewById<TextView>(R.id.textSubtitle)
            val status_button = rowMain.findViewById<Button>(R.id.btnStatus)
            val overflowButton = rowMain.findViewById<ImageButton>(R.id.btnOptions)

            device_model.text = mDataList[position].name
            device_description.text = mDataList[position].description
            status_button.text = if (mDataList[position].status) "On" else "Off"
            status_button.setBackgroundColor(if (mDataList[position].status) ContextCompat.getColor(mContext, R.color.green) else ContextCompat.getColor(mContext, R.color.red))
            overflowButton.setBackgroundColor(ContextCompat.getColor(mContext, R.color.gray))
            // status_button.isChecked = mDataList[position].status
            // status_button.setBackgroundResource(if (mDataList[position].status) R.color.green else R.color.red)
            // status_button.setTextColor(
            //      if (mDataList[position].status)
            //         ContextCompat.getColor(mContext, R.color.green)
            //     else
            //         ContextCompat.getColor(mContext, R.color.red)
            // )


            overflowButton.tag = mDataList[position]
            status_button.tag = mDataList[position]

            /* status_button.setOnCheckedChangeListener { _, isChecked ->
                val all_tags = status_button.tag as? Device
                val device_id = all_tags?.id
                Log.d("MainActivity2","the id is $device_id : ${isChecked.toString()}")
                // (mContext as AppCompatActivity).runOnUiThread {
                    // status_button.setTextColor(
                    //     if (all_tags!!.status == false)
                    //          ContextCompat.getColor(mContext, R.color.green)
                    //     else
                    //         ContextCompat.getColor(mContext, R.color.red)
                    // )
                // }
                // Disable the button and update its status
                status_button.isEnabled = false

                try {
                    if(all_tags!!.status){
                        SMSManager.sendSMS(all_tags.telephone,all_tags.msg_off)
                    } else {
                        SMSManager.sendSMS(all_tags.telephone,all_tags.msg_on)
                    }
                    (mContext as AppCompatActivity).lifecycleScope.launch(Dispatchers.IO) {
                        Log.d("MainActivity2","Global Scope update status")
                        deviceDao.updateDeviceStatus(all_tags.id,status_button.isChecked)
                        // If false (it will be True) donc Green, if true (it will be False) donc Red

                        delay(10000) // Delay for 10 seconds
                        (mContext as AppCompatActivity).runOnUiThread {
                            status_button.isEnabled = true // Re-enable the button
                        }
                        runBlocking {
                            updateData()
                        }
                    }

                } catch (e: Exception){
                    Toast.makeText(mContext,"Error: $e", Toast.LENGTH_SHORT).show()
                }
                status_button.text = if (status_button.isChecked) "On" else "Off"
            } */

            status_button.setOnClickListener {
                val all_tags = status_button.tag as? Device
                val device_id = all_tags?.id
                Log.d("MainActivity2","the id is $device_id : ${all_tags?.status}")
                /*(mContext as AppCompatActivity).runOnUiThread {
                    status_button.setTextColor(
                        if (all_tags!!.status == false)
                             ContextCompat.getColor(mContext, R.color.green)
                        else
                            ContextCompat.getColor(mContext, R.color.red)
                    )
                }*/
                // Disable the button and update its status
                status_button.setBackgroundColor(ContextCompat.getColor(mContext, R.color.gray))
                status_button.isEnabled = false

                try {
                    if(all_tags!!.status){
                        SMSManager.sendSMS(all_tags.telephone,all_tags.msg_off)
                    } else {
                        SMSManager.sendSMS(all_tags.telephone,all_tags.msg_on)
                    }
                    Log.d("MainActivity2","Test ${all_tags.status}")
                    (mContext as AppCompatActivity).lifecycleScope.launch(Dispatchers.IO) {
                        Log.d("MainActivity2","Global Scope update status")
                        deviceDao.updateDeviceStatus(all_tags.id,!(all_tags.status))
                        // If false (it will be True) donc Green, if true (it will be False) donc Red

                        delay(10000) // Delay for 10 seconds
                        (mContext as AppCompatActivity).runOnUiThread {
                            status_button.isEnabled = true // Re-enable the button
                            //status_button.setBackgroundColor(if (all_tags.status) ContextCompat.getColor(mContext, R.color.green) else ContextCompat.getColor(mContext, R.color.red))
                        }
                        runBlocking {
                            updateData()
                        }
                    }

                } catch (e: Exception){
                    Toast.makeText(mContext,"Error: $e", Toast.LENGTH_SHORT).show()
                }
                status_button.text = if (status_button.text == "Off") "On" else "Off"
            }

            overflowButton.setOnClickListener {
                showOverflowMenu(it, position)
            }

            return rowMain
        }

        fun showEditDialog(device: Device) {
            val builder = AlertDialog.Builder(mContext)
            val inflater = LayoutInflater.from(mContext)
            val dialogView = inflater.inflate(R.layout.device_edit_alertdialog, null)
            builder.setView(dialogView)

            // Find views in the custom layout
            val deviceTitle: TextView = dialogView.findViewById(R.id.device_title)
            val telephoneInput: EditText = dialogView.findViewById(R.id.telephone_input)
            val configurationSwitch: Switch = dialogView.findViewById(R.id.configuration_switch)
            val saveButton: Button = dialogView.findViewById(R.id.save_button)

            // Set initial values based on the device object
            deviceTitle.text = "Device ${device.name}"
            telephoneInput.setText(device.telephone)
            // TODO("FIX LATER")
            configurationSwitch.isChecked = true

            val alertDialog = builder.create()
            val window = alertDialog.window
            if (window != null) {
                val windowMetrics = mContext.resources.displayMetrics
                val screenHeight = windowMetrics.heightPixels
                val marginPercentage = 0.3 // Adjust this percentage as needed
                val marginPixels = (screenHeight * marginPercentage).toInt()
                val layoutParams = window.attributes
                layoutParams.gravity = Gravity.TOP
                layoutParams.y = marginPixels // Adjust the margin as needed
                window.attributes = layoutParams
            }

            saveButton.setOnClickListener {
                // Update the device object with edited information
                Toast.makeText(mContext,"Saved Modifications: ${telephoneInput.text.toString()} ==== ${configurationSwitch.isChecked}", Toast.LENGTH_SHORT).show()

                // Handle save action
                alertDialog.dismiss()
            }

            alertDialog.show()
        }

        fun updateData() {
            Log.d("MainActivity2","update status: notify Data set changed")
            val deviceDao = AppDatabase.getInstance(mContext).deviceDao()
            (mContext as AppCompatActivity).lifecycleScope.launch(Dispatchers.IO){
                try {
                    val user_id = (mContext as DevicesActivity).myPreferences.getLong("CURRENT_USER_ID",-1L)
                    Log.d("MainActivity2","[UPDATE DATA]: current user connected $user_id")
                    mDataList = deviceDao.getAllDevicesByUserId(user_id) as MutableList<Device>
                    (mContext as AppCompatActivity).runOnUiThread {
                        notifyDataSetChanged()
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity2", "Error status: idk $e")
                }
            }
            (mContext as AppCompatActivity).runOnUiThread {
                try{
                    notifyDataSetChanged()
                }catch (e: Exception) {
                    Log.e("MainActivity2", "Error status: idk $e")
                }
            }
        }

        private fun showOverflowMenu(view: View, position: Int) {
            val popupMenu = PopupMenu(mContext, view)
            popupMenu.inflate(R.menu.overflow_options_menu)

            val device = view.tag as Device
            val deviceId = (view.tag as Device).id


            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.menuOpt1 -> {
                        try {
                            Intent(mContext, SchedulesActivity::class.java).also {
                                it.putExtra("device_id", deviceId)
                                mContext.startActivity(it)
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity2","Luck let the Intent in (it's past) $deviceId")
                        }
                        true
                    }
                    R.id.menuOpt2 -> {
                        showEditDialog(device)
                        true
                    }
                    // Add more cases for other menu items if needed
                    else -> false
                }
            }

            popupMenu.show()
        }
    }

    private suspend fun fetchNewDevices() {
        try {
            val authToken = myPreferences.getString("jwt", "")
            if (authToken.isNullOrEmpty()) {
                return
            }
            var responseOfHttp = RetrofitInstance.api.getDevices(authToken = "Bearer $authToken")
            if (responseOfHttp.code() == 200) {
                val devices = responseOfHttp.body()?.get("devices")
                if(devices != null) {
                    Log.d("MainActivity2", "Devices are not null")
                }
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        loadNewDevices(devices = devices!!)
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

    private suspend fun loadNewDevices(devices: List<Map<String,Any>>) {
        val deviceDao = database.deviceDao()

        val current_user = myPreferences.getLong("CURRENT_USER_ID", -1L)

        val listIds = devices.map { (it["id"] as Double).toLong() }

        // Step 1: Cancellation of All Alarms (of user)
        Scheduler.deinitialize(current_user)

        // Step 2: Delete non-affected Devices despite having schedules
        deviceDao.deleteDevicesNotInListByIdsByUserId(listIds, current_user)

        // Step 3: Insert/Upsert (New) Devices
        for(device in devices) {
            val deviceStatus: Boolean? = deviceDao.getDeviceStatusById((device["id"] as Number).toLong())
            val statusValue = if (deviceStatus != null) deviceStatus else false
            // TODO("DEVICE_ID is changed to ID (which is affecter_id")
            val structuredDeviceMap = mapOf<String,Any>(
                "id" to device["id"]!!,
                "name" to device["model"]!!,
                "description" to device["description"]!!,
                "telephone" to device["tlf"]!!,
                "msg_on" to device["msg_on"]!!,
                "msg_off" to device["msg_off"]!!,
                "user_id" to device["user_id"]!!,
                "config" to device["config"]!!,
                "status" to statusValue
            )
            deviceDao.upsert(Device.fromMap(structuredDeviceMap))
        }
        // Step 4: Initialize All Alarms (from the Schedules Table)
        // TODO("Change from Initialize ALL to Initialize By UserID")
        Scheduler.initialize(current_user)
        Log.i("MainActivity2","Loaded Data from Fetch SUCCESSFULLY")

        // Step 5: Signal adapter to change (fetch from local DB) ListView Items
        deviceAdapter.updateData()
    }
}