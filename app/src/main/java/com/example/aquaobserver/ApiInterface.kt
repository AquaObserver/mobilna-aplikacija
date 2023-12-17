package com.example.aquaobserver

import com.example.aquaobserver.api.MyDateReadings
import com.example.aquaobserver.api.MyReadings
import com.example.aquaobserver.api.Reading
import com.example.aquaobserver.api.UserThreshold
import com.example.aquaobserver.api.UserThresholdUpdate
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiInterface {

    @GET("/getLatest/")
    fun getLatestReading():Call<Reading>

    @GET("/readings/")
    fun getReadings(): Call<MyReadings>

    @GET("/readings/{date}")
    suspend fun getDateReadings(@Path("date") date: String): MyDateReadings

    @GET("/userThreshold/")
    fun getThreshold(): Call<UserThreshold>

    @POST("/userThreshold/")
    fun pushThreshold(
        @Body request: UserThresholdUpdate
    ): Call<UserThresholdUpdate>

}