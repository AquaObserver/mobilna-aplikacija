package com.example.aquaobserver

import retrofit2.Call
import retrofit2.http.GET
interface ApiInterface {

    @GET("/readings/")
    fun getData(): Call<MyData>

}