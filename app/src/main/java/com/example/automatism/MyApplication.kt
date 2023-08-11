package com.example.automatism

import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import android.content.Context


class MyApplication : MultiDexApplication(){
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}