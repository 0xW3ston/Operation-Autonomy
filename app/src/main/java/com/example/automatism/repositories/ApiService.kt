package com.example.automatism.repositories

import com.example.automatism.database.models.Schedule
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("login")
    suspend fun loginUser(@Body requestBody: Map<String, String>): Response<Map<String, String>>

    @GET("devices")
    suspend fun getDevices(@Header("Authorization") authToken: String): Response<Map<String,List<Map<String, Any>>>>

    @POST("affecters/{idDevice}/modifierStatus")
    suspend fun setDeviceStatus(
        @Path("idDevice") idDevice: Long,
        @Header("Authorization") authToken: String,
        @Body requestBody: Map<String, Boolean>,
    ): Response<Any>

    @POST("affecters/{idDevice}/modifierTelephone")
    suspend fun setTelephone(
        @Path("idDevice") idDevice: Long,
        @Header("Authorization") authToken: String,
        @Body requestBody: Map<String, String>,
    ): Response<Any>

    @POST("affecters/{idDevice}/modifierConfigStatus")
    suspend fun setConfigStatus(
        @Path("idDevice") idDevice: Long,
        @Header("Authorization") authToken: String,
        @Body requestBody: Map<String, Boolean>
    ): Response<Any>


    // C.R.U.D. Schedules (User Active)
    @POST("reglage/ajouterReglage")
    suspend fun addNewSchedule(
        @Header("Authorization") authToken: String,
        @Body requestBody: Schedule
    ): Response<Map<String, Any>>

    @GET("reglage/{reglage}/delete")
    suspend fun deleteSchedule(
        @Path("reglage") reglageId: Long,
        @Header("Authorization") authToken: String
    ): Response<Map<String,Any>>

    @POST("reglage/{reglage}/modifierReglage")
    suspend fun modifierSchedule(
        @Path("reglage") scheduleId: Long,
        @Header("Authorization") authToken: String,
        @Body requestBody: Schedule
    ): Response<Map<String,Any>>

    @GET("reglages/{idAffecter}")
    suspend fun getSchedulesForDeviceId(
        @Path("idAffecter") deviceId: Long,
        @Header("Authorization") authToken: String
    ): Response<Map<String,List<Map<String, Any>>>>

    // TODO("Should I make an API Route for even Selecting all Schedules")
}
