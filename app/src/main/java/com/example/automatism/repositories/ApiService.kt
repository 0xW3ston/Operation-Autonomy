package com.example.automatism.repositories

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("login")
    suspend fun loginUser(@Body requestBody: Map<String, String>): Response<Map<String, String>>

    @GET("devices")
    suspend fun getDevices(@Header("Authorization") authToken: String): Response<Map<String,List<Map<String, Any>>>>

    @POST("devices/{idDevice}/modifierStatus")
    suspend fun setDeviceStatus(
        @Path("idDevice") idDevice: Long,
        @Header("Authorization") authToken: String,
        @Body requestBody: Map<String, Boolean>,
    )

    @POST("devices/{idDevice}/modifierTelephone")
    suspend fun setTelephone(
        @Path("idDevice") idDevice: Long,
        @Header("Authorization") authToken: String,
        @Body requestBody: Map<String, String>,
    )

    @POST("devices/{idDevice}/ajouterSchedule")
    suspend fun addNewSchedule(
        @Path("idDevice") idDevice: Long,
        @Header("Authorization") authToken: String,
        @Body requestBody: Map<String, Any>
    )

    @GET("devices/{idDevice}/config")
    suspend fun getConfig(
        @Path("idDevice") idDevice: Long,
        @Header("Authorization") authToken: String
    )
}
