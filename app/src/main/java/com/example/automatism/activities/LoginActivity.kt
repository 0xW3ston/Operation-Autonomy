package com.example.automatism.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.automatism.database.AppDatabase
import com.example.automatism.database.models.User
import com.example.automatism.databinding.LoginActivityBinding
import com.example.automatism.utils.AuthHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: LoginActivityBinding
    private lateinit var database: AppDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = LoginActivityBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("MainActivity2","onCreate Login")
            val authHelper = AuthHelper(this)
            database = AppDatabase.getInstance(this)
            val userDao = database.userDao()
            // val sharedPref = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
            // binding = LoginActivityBinding.inflate(layoutInflater)
            // val view = binding.root
            // setContentView(view)

            // setContentView(R.layout.login_activity)

            binding.btnLogin.setOnClickListener {
                var identifiant = binding.inputIdentifiant.text.toString()
                var password = binding.inputPassword.text.toString()
                // Toast.makeText(this,"You have tried to log in as $email identified by $password", Toast.LENGTH_SHORT).show()
                lifecycleScope.launch(Dispatchers.IO) {
                    Log.d("MainActivity2", "$identifiant => $password")
                    Log.d("MainActivity2", "Before Login")
                    val login_user_id = authHelper.login(identifiant, password)
                    Log.d("MainActivity2", "After Login ${login_user_id.toString()}")
                    if (authHelper.isLoggedIn()) {
                        userDao.upsertUser(
                            User(
                                id = login_user_id
                            )
                        )
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT)
                                .show()
                        }
                        Intent(this@LoginActivity, DevicesActivity::class.java).also {
                            startActivity(it)
                            finish()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "Login Failed!", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this@LoginActivity, "Error L: $e", Toast.LENGTH_SHORT)
                    .show()
            }
            Log.e("MainActivity2", "Error onCreate Login: $e")
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu?.clear()
        return super.onPrepareOptionsMenu(menu)
    }

}

/*
lifecycleScope.launch {
    try {
        val credentials = mapOf<String, Any>(
            "email" to email,
            "password" to password
        )
        // val response = RetrofitInstance.api.loginUser(credentials)
        if (response.isSuccessful) {
            val accessToken = response.body()?.get("jwt")
            Intent(this@LoginActivity, DevicesActivity::class.java).also {
                startActivity(it)
                finish()
            }
        } else {
            Toast.makeText(
                this@LoginActivity,
                "Failed, You have tried to log in as $email identified by $password",
                Toast.LENGTH_SHORT
            ).show()
        }
    } catch (e: IOException) {
        Log.e("MainActivity", "IOException: $e")
        // Handle network or IO error here
    } catch (e: HttpException) {
        Log.e("MainActivity", "HttpException: $e")
        // Handle HTTP error here (e.g., 401 Unauthorized, 404 Not Found, etc.)
    }

}
*/

/*
             lifecycleScope.launch {
                try {
                    val loginResponse = RetrofitInstance.api.loginUser(
                        mapOf<String, String>(
                            "email" to email,
                            "password" to password
                        )
                    )
                    if(loginResponse.code() == 200) {
                        val sharedPref_editor = sharedPref.edit()
                        val jwt_token = loginResponse.body()?.get("jwt")
                        val loginTimestamp = System.currentTimeMillis() // Current timestamp in milliseconds

                        Toast.makeText(this@LoginActivity,"Very Nice good $jwt_token", Toast.LENGTH_SHORT).show()

                        Log.d("MainActivity2","A Login Success")

                        Intent(this@LoginActivity, DevicesActivity::class.java).also {
                            sharedPref_editor.putString("jwt",jwt_token)
                            sharedPref_editor.putLong("LOGIN_TIMESTAMP", loginTimestamp)
                            sharedPref_editor.apply()
                            startActivity(it)
                            finish()
                        }
                    } else {
                        Log.d("MainActivity2","Failed Login")
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity2","Wrong Credentials maybe $e")
                }
                Log.d("MainActivity2","After If")
            }
*/