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
import kotlinx.coroutines.withContext
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
            try{
                fetchNewDevices()
            } catch (e: Exception) {
                Log.e("MainActivity2","error in Fetching devices (onCreate)")
            }
            if(myPreferences.getBoolean("USER_ACTIVE",false)){
                try{
                    fetchNewSchedulesOfAllDevicesByUserId()
                } catch (e: Exception){
                    Log.e("MainActivity2","error in onCreate FOR USER ACTIVE: $e")
                }
            }
            try {
                runOnUiThread {
                    Log.e("MainActivity2","Good Initialization")
                    deviceAdapter.updateData()
                }
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
            R.id.oSettings -> {
                try {
                    Intent(this, UserSettingesActivity::class.java).also {
                        startActivity(it)
                        return true
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity2", "Exception: $e")
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

            val status_indicator = rowMain.findViewById<TextView>(R.id.circularStatusIndicator)
            val device_model = rowMain.findViewById<TextView>(R.id.textTitle)
            val device_description = rowMain.findViewById<TextView>(R.id.textSubtitle)
            val status_button_on = rowMain.findViewById<Button>(R.id.btnStatusOn)
            val status_button_off = rowMain.findViewById<Button>(R.id.btnStatusOff)
            val overflowButton = rowMain.findViewById<ImageButton>(R.id.btnOptions)
            var my_prefs = (mContext as DevicesActivity).myPreferences

            status_indicator.text = if (mDataList[position].status) "On" else "Off"
            if (mDataList[position].status) {
                status_indicator.setBackgroundResource(R.drawable.circular_background_green)
                status_button_on.isEnabled = false
                status_button_off.isEnabled = true
            } else {
                status_indicator.setBackgroundResource(R.drawable.circular_background_red)
                status_button_on.isEnabled = true
                status_button_off.isEnabled = false
            }

            device_model.text = mDataList[position].name
            device_description.text = mDataList[position].description
            // overflowButton.setBackgroundColor(ContextCompat.getColor(mContext, R.color.gray))
            // status_button.isChecked = mDataList[position].status
            // status_button.setBackgroundResource(if (mDataList[position].status) R.color.green else R.color.red)
            // status_button.setTextColor(
            //      if (mDataList[position].status)
            //         ContextCompat.getColor(mContext, R.color.green)
            //     else
            //         ContextCompat.getColor(mContext, R.color.red)
            // )


            status_button_on.tag = mDataList[position]
            status_button_off.tag = mDataList[position]
            overflowButton.tag = mDataList[position]

            // Set a single listener for both "On" and "Off" buttons
            val statusButtonListener = View.OnClickListener {
                val statusBtn = (it as Button)
                val all_tags = statusBtn.tag as? Device
                val device_id = all_tags?.id
                val buttonText = statusBtn.text.toString()

                Log.d("MainActivity2","the id is $device_id : ${all_tags?.status}")

                status_button_on.isEnabled = false
                status_button_off.isEnabled = false

                try {
                if (buttonText == "On") {
                    SMSManager.sendSMS(all_tags!!.telephone ,all_tags!!.msg_on)
                } else if (buttonText == "Off") {
                    SMSManager.sendSMS(all_tags!!.telephone ,all_tags!!.msg_off)
                }
                Log.d("MainActivity2","Test ${all_tags!!.status}")
                    (mContext as AppCompatActivity).lifecycleScope.launch(Dispatchers.IO) {
                    try{
                        Log.d("MainActivity2","Global Scope update status")
                        deviceDao.updateDeviceStatus(all_tags.id,!(all_tags.status))
                        // If false (it will be True) donc Green, if true (it will be False) donc Red

                        status_indicator.setBackgroundResource(R.drawable.circular_background_orange)
                        status_indicator.text = "..."

                        delay(10000) // Delay for 10 seconds
                        (mContext as AppCompatActivity).runOnUiThread {
                            status_button_on.isEnabled = all_tags.status
                            status_button_off.isEnabled = !(all_tags.status)
                            status_indicator.setBackgroundResource(
                                if (all_tags.status)
                                    R.drawable.circular_background_red
                                else
                                    R.drawable.circular_background_green
                            )
                            status_indicator.text = (if (all_tags.status)
                                                        "Off"
                                                    else
                                                        "On")
                            //status_button.setBackgroundColor(if (all_tags.status) ContextCompat.getColor(mContext, R.color.green) else ContextCompat.getColor(mContext, R.color.red))
                        }
                        runBlocking {
                            updateData()
                        }
                        if(my_prefs.getBoolean("USER_ACTIVE",false)){
                            var api = RetrofitInstance.api.setDeviceStatus(
                                idDevice = mDataList[position].id,
                                authToken = my_prefs.getString("jwt","")!!,
                                requestBody = mapOf(
                                    "status" to !(all_tags!!.status)
                                )
                            )
                        }
                    } catch(e: Exception) {
                        Log.e("MainActivity2","error: $e")
                        (mContext as AppCompatActivity).runOnUiThread {
                            Toast.makeText(mContext,"Error: $e", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                } catch (e: Exception){
                    Log.e("MainActivity2","error: $e")
                    (mContext as AppCompatActivity).runOnUiThread {
                        Toast.makeText(mContext,"Error: $e", Toast.LENGTH_SHORT).show()
                    }
                }
            }


            status_button_on.setOnClickListener(statusButtonListener)
            status_button_off.setOnClickListener(statusButtonListener)

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

            // TODO("REMOVED THIS / OR RATHER DESACTIVATED")
            /* status_button.setOnClickListener {
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
                        try{
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
                            if(my_prefs.getBoolean("USER_ACTIVE",false)){
                                var api = RetrofitInstance.api.setDeviceStatus(
                                    idDevice = mDataList[position].id,
                                    authToken = my_prefs.getString("jwt","")!!,
                                    requestBody = mapOf(
                                        "status" to !(all_tags!!.status)
                                    )
                                )
                            }
                        } catch(e: Exception) {
                            Log.e("MainActivity2","error: $e")
                            (mContext as AppCompatActivity).runOnUiThread {
                                Toast.makeText(mContext,"Error: $e", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                } catch (e: Exception){
                    Log.e("MainActivity2","error: $e")
                    (mContext as AppCompatActivity).runOnUiThread {
                        Toast.makeText(mContext,"Error: $e", Toast.LENGTH_SHORT).show()
                    }
                }
                status_button.text = if (status_button.text == "Off") "On" else "Off"
            } */

            overflowButton.setOnClickListener {
                showOverflowMenu(it, position)
            }

            return rowMain
        }

        fun showEditDialog(device: Device) {
            try {
                val builder = AlertDialog.Builder(mContext)
                val inflater = LayoutInflater.from(mContext)
                val dialogView = inflater.inflate(R.layout.device_edit_alertdialog, null)
                builder.setView(dialogView)

                // Find views in the custom layout
                val deviceTitle: TextView = dialogView.findViewById(R.id.device_title)
                val telephoneInput: EditText = dialogView.findViewById(R.id.telephone_input)
                val configurationSwitch: Button = dialogView.findViewById(R.id.configuration_switch)
                val saveButton: Button = dialogView.findViewById(R.id.save_button)

                (mContext as AppCompatActivity).runOnUiThread {
                    // val isConfigured = deviceDao.getDeviceById(device.id).isConfigured
                    // configurationSwitch.isChecked = device.isConfigured
                    if (device.isConfigured) {
                        configurationSwitch.text = "Deja Configurée"
                    } else {
                        configurationSwitch.text = "n'est pas configurée"
                    }
                }

                // Set initial values based on the device object
                deviceTitle.text = "Device ${device.name}"
                telephoneInput.setText(device.telephone)
                // TODO("FIX LATER")
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
                    (mContext as AppCompatActivity).lifecycleScope.launch(Dispatchers.IO) {
                        var A_Device = device
                        A_Device.telephone = telephoneInput.text.toString()
                        try {
                            var token = (mContext as DevicesActivity).myPreferences.getString("jwt", "")
                            Log.d("MainActivity2","Is Successful: ${token}, HTTP Code: ${device.id} :: ${A_Device.telephone}")
                            var api = RetrofitInstance.api.setTelephone(
                                idDevice = device.id,
                                authToken = token!!,
                                requestBody = mapOf(
                                    "telephone" to A_Device.telephone
                                )
                            )

                            val user_id = (mContext as DevicesActivity).myPreferences.getLong("CURRENT_USER_ID", -1L)
                            if(user_id == -1L || api.code() != 200) {
                                throw Exception("Error, No User_ID Or Refused")
                            }

                            runBlocking {
                                (mContext as DevicesActivity).Scheduler.deinitialize(userId = user_id)
                                deviceDao.update(A_Device)
                                (mContext as DevicesActivity).Scheduler.initialize(userId = user_id)
                            }

                            (mContext as AppCompatActivity).runOnUiThread {
                                Toast.makeText(
                                    mContext,
                                    "Saved Modifications: Telephone",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            (mContext as AppCompatActivity).runOnUiThread {
                                Toast.makeText(
                                    mContext,
                                    "An Error occured, the information no sont pas enregistrées",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            Log.e("MainActivity2","SAVE BUTTON: Settinges Device Error => $e")
                        }

                        // Update the device object with edited information
                        updateData()

                        (mContext as AppCompatActivity).runOnUiThread {
                            // Handle save action
                            alertDialog.dismiss()
                        }
                    }
                }

                configurationSwitch.setOnClickListener {
                    try {
                        showConfirmDialog(device)
                    } catch (e: Exception){
                        Toast.makeText(
                            mContext,
                            "An Error Occured, Device Not Configured Properly",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("MainActivity2","Configuration Switch Error: $e")
                    }
                    updateData()
                    // TODO("Configuration Activation Code")
                }

                alertDialog.show()
            } catch (e: Exception) {
                Log.e("MainActivity2"," Show Dialog Error: $e")
            }
        }

        fun showConfirmDialog(device: Device) {
            val alertDialogConfirm = AlertDialog.Builder(mContext).create()
            val inflater = LayoutInflater.from(mContext)
            val dialogView = inflater.inflate(R.layout.confirm_alertdialog, null)
            alertDialogConfirm.setView(dialogView)

            // Access the views inside the dialog
            val dialogTitle = dialogView.findViewById<TextView>(R.id.confirmDialogTitle)
            val dialogMessage = dialogView.findViewById<TextView>(R.id.confirmDialogMessage)
            val btnCancel = dialogView.findViewById<Button>(R.id.confirmDialogBtnCancel)
            val btnConfirm = dialogView.findViewById<Button>(R.id.confirmDialogBtnConfirm)

            // Set the title and message
            dialogTitle?.text = "Alert"
            dialogMessage?.text = "Are you sure you want to continue?"

            btnCancel.setOnClickListener {
                alertDialogConfirm.dismiss() // Close the dialog
            }

            btnConfirm.setOnClickListener {

                // Disable both buttons
                btnConfirm.isEnabled = false
                btnCancel.isEnabled = false

                val authToken = (mContext as DevicesActivity).myPreferences.getString("jwt", "")
                (mContext as AppCompatActivity).lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        SMSManager.sendConfigSMS(device)
                        var updated_device = deviceDao.getDeviceById(device.id)
                        updated_device.isConfigured = true
                        deviceDao.update(updated_device)

                        try {
                            var api = RetrofitInstance.api.setConfigStatus(
                                idDevice = device.id,
                                authToken = authToken!!,
                                requestBody = mapOf(
                                    "isConfigured" to true
                                )
                            )
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    mContext,
                                    "Configured Successfully (Local), No successful connection to the backend",
                                    Toast.LENGTH_SHORT
                                ).show()
                                alertDialogConfirm.dismiss() // Close the dialog
                            }
                            return@launch
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                mContext,
                                "Configured Successfully (Local + Online)",
                                Toast.LENGTH_SHORT
                            ).show()
                            alertDialogConfirm.dismiss() // Close the dialog
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                mContext,
                                "Configuration Error: $e",
                                Toast.LENGTH_SHORT
                            ).show()
                            alertDialogConfirm.dismiss() // Close the dialog
                        }
                        Log.e("MainActivity2", "Configuring Error: $e")
                    } finally {
                        // renable both buttons after it's complete (error or good)
                        withContext(Dispatchers.Main) {
                            btnConfirm.isEnabled = true
                            btnCancel.isEnabled = true
                        }
                        (mContext as DevicesActivity).deviceAdapter.updateData()
                    }
                }
            }

            alertDialogConfirm.setCancelable(false)
            alertDialogConfirm.show()
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
                        try{
                            showEditDialog(device)
                            Log.d("MainActivity2","yes hello")
                        } catch (e: Exception) {
                            Log.e("MainActivity2","sss: $e")
                        }
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
            var responseOfHttp = RetrofitInstance.api.getDevices(authToken = authToken)
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
            // val deviceStatus: Boolean? = deviceDao.getDeviceStatusById((device["id"] as Number).toLong())
            // val statusValue = if (deviceStatus != null) deviceStatus else false
            Log.i("MainActivity2","Devices: ${device}")
            try{
                Log.i("MainActivity2","AAA:${(device["id"] as Number).toLong()}")
                Log.i("MainActivity2","AAA:${deviceDao.getDeviceStatusById((device["id"] as Number).toLong())}")
                Log.i("MainActivity2","${device}")
                val deviceStatus: Boolean? = deviceDao.getDeviceStatusById((device["id"] as Number).toLong())

                var statusValue = deviceStatus ?: false
                var isConfigured = if ((device["isconfig"] as Number).toInt() == 1) true else false
                if(myPreferences.getBoolean("USER_ACTIVE",false)) {
                    statusValue = if ((device["statut"] as Number).toInt() == 1) true else false
                }

            // TODO("DEVICE_ID is changed to ID (which is affecter_id")
            val structuredDeviceMap = mapOf<String,Any>(
                "id" to device["id"]!!,
                "name" to device["model"]!!,
                "description" to device["description"]!!,
                "telephone" to (device["tlf"] ?: ""),
                "msg_on" to device["msg_on"]!!,
                "msg_off" to device["msg_off"]!!,
                "user_id" to device["user_id"]!!,
                "config" to device["config"]!!,
                "isConfigured" to isConfigured,
                "status" to statusValue
            )
            deviceDao.upsert(Device.fromMap(structuredDeviceMap))
            } catch (e: Exception) {
                Log.e("MainActivity2","some error here in loading: $e")
            }
        }
        // Step 4: Initialize All Alarms (from the Schedules Table)
        // TODO("Change from Initialize ALL to Initialize By UserID")
        Scheduler.initialize(current_user)
        Log.i("MainActivity2","Loaded Data from Fetch SUCCESSFULLY")

        // Step 5: Signal adapter to change (fetch from local DB) ListView Items
        deviceAdapter.updateData()
    }

    suspend fun loadNewSchedules(schedules: List<Map<String,Any>>){

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
                val isActivatedOnline = schedule["isActivated"] as Boolean
                val isActivated = (if (isActivatedLocal == false || isActivatedOnline == false) false else true)

                Log.d("MainActivity2","isActivated: ${isActivatedRaw}, Real isActivated: ${isActivated}")
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
    }

    suspend fun fetchNewSchedulesOfAllDevicesByUserId() {
        try {
            val authToken = myPreferences.getString("jwt", "")
            val userId = myPreferences.getLong("CURRENT_USER_ID", -1L)
            val deviceDao = database.deviceDao()
            val deviceIds = deviceDao.getAllDeviceIdsByUserId(userId)
            if (authToken.isNullOrEmpty()) {
                return
            }
            for(deviceId in deviceIds){
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
            }
        } catch (e: Exception) {
            Log.e("MainActivity2","From Fetch New Devices: $e")
        }
    }

}