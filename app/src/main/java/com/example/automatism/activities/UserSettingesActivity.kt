package com.example.automatism.activities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.automatism.R
import com.example.automatism.databinding.UserSettingsActivityBinding

class UserSettingesActivity : AppCompatActivity() {

    private lateinit var binding: UserSettingsActivityBinding
    private lateinit var myPreferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Use data binding to set up the layout
            binding = UserSettingsActivityBinding.inflate(layoutInflater)
            setContentView(binding.root)

            myPreferences = this.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)

            // Populate spinner with mode options
            val modeOptions = listOf("Active", "Offline")
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modeOptions)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.modeSpinner.adapter = adapter

            if (myPreferences.getBoolean("FETCHED_USER_ACTIVE", false)) {
                val existingMode = (if (myPreferences.getBoolean("USER_ACTIVE",false)) 0 else 1)
                binding.modeSpinner.setSelection(existingMode)
                Log.i("MainActivity1","An Admin")
            }

            if (!(myPreferences.getBoolean("FETCHED_USER_ACTIVE", false))) {
                Log.i("MainActivity1","Normal User")
                binding.modeSpinner.setSelection(1)
                binding.modeSpinner.isEnabled = false
            }

            // Set click listener for the save button
            binding.saveButton.setOnClickListener {
                val selectedMode = binding.modeSpinner.selectedItem as String
                val isModeActive = (selectedMode == "Active")

                val editor = myPreferences.edit()
                editor.putBoolean("USER_ACTIVE", isModeActive)
                editor.apply()
                val message = if (isModeActive) "Active mode Activated" else "Offline mode Activated"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            Log.e("MainActivity2","Error onCreate User Settings $e")
        }
    }
}
