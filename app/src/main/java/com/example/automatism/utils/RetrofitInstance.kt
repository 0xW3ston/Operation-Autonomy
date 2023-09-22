package com.example.automatism.utils

import com.example.automatism.repositories.ApiService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    // Create a custom Gson instance with null values included
    private val gson: Gson = GsonBuilder()
        .serializeNulls()
        .create()

    // Create your Retrofit service with the custom Gson instance
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://mysi.shop/api/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
    /* val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://mysi.shop/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    } */
}