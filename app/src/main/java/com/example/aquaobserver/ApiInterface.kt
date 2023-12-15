package com.example.aquaobserver

import com.example.aquaobserver.api.MyReadings
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

    @GET("/readings/")
    fun getReadings(): Call<MyReadings>

    @GET("/userThreshold/2")
    fun getThreshold(): Call<UserThreshold>

    @PUT("/userThreshold/{userId}")
    fun updateThreshold(
        @Path("userId") userId: Int,
        @Body request: UserThresholdUpdate
    ): Call<UserThresholdUpdate>

}