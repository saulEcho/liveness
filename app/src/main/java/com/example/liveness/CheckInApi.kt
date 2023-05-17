package com.example.liveness

import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST

interface CheckInApi {
    @POST("/checkin")
    suspend fun sendCheckIn(@Body checkIn: EmployeeCheckIn): Response

    @POST("/checkin/batch")
    suspend fun sendBatchCheckIn(@Body checkIns: List<EmployeeCheckIn>): Response
}