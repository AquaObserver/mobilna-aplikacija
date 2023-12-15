package com.example.aquaobserver.api

data class Reading(
    val deviceId: Int,
    val id: Int,
    val tstz: String,
    val waterLevel: Double
)