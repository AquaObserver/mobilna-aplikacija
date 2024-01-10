package com.example.aquaobserver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.aquaobserver.api.DeviceToken
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MyFirebaseMessagingService : FirebaseMessagingService() {
    val BASE_URL = "https://polliwog-enormous-walrus.ngrok-free.app/"
    // val BASE_URL = "http://10.0.2.2:8000/"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            generateNotification(it.title!!, it.body!!)
        }
    }

    private fun generateNotification(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        var notificationBuilder = NotificationCompat.Builder(this.applicationContext, "fcm_default_channel")
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000, 1000))
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
        notificationBuilder = notificationBuilder.setContent(getRemoteView(title, message))
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel("fcm_default_channel", "com.example.aquaobserver", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(notificationChannel)
            Log.d("FCM", "Notification channel created")
        }
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        Log.d("FCM", "Notification notified")
    }

    private fun getRemoteView(title: String, message: String) : RemoteViews {
        val remoteView = RemoteViews("com.example.aquaobserver", R.layout.notification)
        remoteView.setTextViewText(R.id.notification_title, title)
        remoteView.setTextViewText(R.id.notification_message, message)
        remoteView.setImageViewResource(R.id.notification_logo, R.drawable.logo)
        return remoteView
    }

    override fun onNewToken(token: String) {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BASE_URL)
            .build()
        val apiService = retrofit.create(ApiInterface::class.java)
        val response = apiService.postRegisterDevice(DeviceToken(token))
        response.enqueue(object : Callback<DeviceToken> {
            override fun onResponse(
                call: Call<DeviceToken>,
                response: Response<DeviceToken>
            ) {
                if (response.isSuccessful) {
                    Log.d("device-token-post", "Threshold pushed successfully: ${response.body()}")
                } else {
                    Log.d("device-token-post", "${response.code()} - ${response.message()} || ${response.body()}")
                }
            }
            override fun onFailure(call: Call<DeviceToken>, t: Throwable) {
                Log.d("device-token-post", "Failed to post device token. Unexpected network error.")
            }
        })
    }
}