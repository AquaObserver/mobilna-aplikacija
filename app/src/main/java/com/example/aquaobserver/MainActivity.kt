package com.example.aquaobserver

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.example.aquaobserver.api.MyReadings
import com.example.aquaobserver.api.UserThreshold
import com.example.aquaobserver.api.UserThresholdUpdate
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.os.Handler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.aquaobserver.api.Reading
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceIdReceiver
import com.google.firebase.iid.internal.FirebaseInstanceIdInternal
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var maxVolume: Int = 55

    private lateinit var btnChangeCriticalLevel : Button
    private lateinit var btnMeasurementHistory : Button

    private lateinit var bucketProgressBar: ProgressBar

    private lateinit var criticalLevelResultTv: TextView
    private lateinit var maxVolumeResultTv : TextView
    private lateinit var currentVolumeResultTv : TextView
    private lateinit var lastUpdatedResultTv : TextView
    private lateinit var bucketTv: TextView
    private lateinit var tvCriticalIndicator: TextView

    private val handler = Handler()
    private val delayMillis: Long = 10000 // 10 seconds

    val BASE_URL = "http://10.0.2.2:8000/"

    private val fetchThresholdRunnable = object : Runnable {
        override fun run() {
            Log.d("MainActivity", "Level updated")
            getMyData()
            handler.postDelayed(this, delayMillis)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnChangeCriticalLevel = findViewById(R.id.btn_change_critical_level)
        btnMeasurementHistory = findViewById(R.id.btn_measurement_history)

        bucketProgressBar = findViewById(R.id.bucket_progress)
        bucketTv = findViewById(R.id.tv_bucket)

        criticalLevelResultTv = findViewById(R.id.tv_critical_level_result)
        maxVolumeResultTv = findViewById(R.id.tv_max_volume_result)
        currentVolumeResultTv = findViewById(R.id.tv_current_volume_result)
        lastUpdatedResultTv = findViewById(R.id.tv_last_update_result)

        tvCriticalIndicator = findViewById(R.id.tvCriticalIndicator)

        maxVolumeResultTv.text = maxVolume.toString() + "L"

        btnChangeCriticalLevel.setOnClickListener(this)
        btnMeasurementHistory.setOnClickListener(this)

        askNotificationPermission()

        handler.post(fetchThresholdRunnable)

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("token-token", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            val token = task.result
            Log.d("token-token", token)
        })
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btn_change_critical_level -> {
                val popUpInflater = LayoutInflater.from(this)
                val criticalLvlView = popUpInflater.inflate(R.layout.change_critical_level, null)
                val newCriticalLevel = criticalLvlView.findViewById<EditText>(R.id.et_new_critical_level)
                val addDialog = AlertDialog.Builder(this)

                addDialog.setView(criticalLvlView)
                addDialog.setPositiveButton("Ok") {
                    dialog,_ ->

                    //POST za update
                    val retrofit = Retrofit.Builder()
                        .addConverterFactory(GsonConverterFactory.create())
                        .baseUrl(BASE_URL)
                        .build()

                    val apiService = retrofit.create(ApiInterface::class.java)

                    val response = apiService.pushThreshold(UserThresholdUpdate(newCriticalLevel.text.toString().toDouble()))

                    response.enqueue(object : Callback<UserThresholdUpdate> {
                        override fun onResponse(
                            call: Call<UserThresholdUpdate>,
                            response: Response<UserThresholdUpdate>
                        ) {
                            if (response.isSuccessful) {
                                val updatedUserThreshold = response.body()
                                Log.d("MainActivity", "Threshold pushed successfully: $updatedUserThreshold")
                            } else {
                                Log.d("MainActivity", "Failed to push threshold: ${response.code()}")
                            }
                        }

                        override fun onFailure(call: Call<UserThresholdUpdate>, t: Throwable) {
                            Log.d("MainActivity", "Failed to push threshold: ${t.message}")
                        }
                    })


                    this.criticalLevelResultTv.text = newCriticalLevel.text.toString() + "%"
                    Toast.makeText(this, "Kriticna razina promijenjena", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    criticalLevelCase()
                }
                addDialog.setNegativeButton("Cancel") {
                    dialog, _ ->
                    dialog.dismiss()
                    Toast.makeText(this, "Cancel", Toast.LENGTH_SHORT).show()
                }
                addDialog.create()
                addDialog.show()
            }
            R.id.btn_measurement_history -> {
                intent = Intent(this, MeasurementsHistory::class.java)
                startActivity(intent)
            }
        }
    }

    private fun getMyData() {
        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BASE_URL)
            .build()
            .create(ApiInterface::class.java)

        /*GET za readings
        val retrofitReadingsData = retrofitBuilder.getReadings()
        retrofitReadingsData.enqueue(object : Callback<MyReadings> {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(
                call: Call<MyReadings>,
                response: Response<MyReadings>
            ) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()

                    if (apiResponse != null) {

                        val readings = apiResponse.readings

                        } else {
                            Log.d("MainActivity", "Readings list is empty.")
                        }
                    } else {
                        Log.d("MainActivity", "Response body is null.")
                    }
                } else {
                    Log.d("MainActivity", "Unsuccessful response: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<MyReadings>, t: Throwable) {
                Log.d("MainActivity", "onFailure: ${t.message}")
            }
        }) */

        val retrofitLatestReading = retrofitBuilder.getLatestReading()
        retrofitLatestReading.enqueue(object : Callback<Reading> {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call<Reading>, response: Response<Reading>) {
                if (response.isSuccessful) {
                    val latestReading = response.body()

                    if (latestReading != null) {

                        val lastValue = latestReading.waterLevel
                        Log.d("MainActivity", "Latest value: $lastValue")
                        bucketTv.text = "$lastValue%"
                        bucketProgressBar.progress = lastValue.toInt()
                        criticalLevelCase()

                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                        val dateTime = LocalDateTime.parse(latestReading.tstz, formatter)
                        lastUpdatedResultTv.text = String.format("%02d:%02d:%02d", dateTime.hour, dateTime.minute, dateTime.second)
                        currentVolumeResultTv.text = String.format("%.2f",(lastValue / 100 * maxVolume.toFloat())) + "L"

                    } else {
                        Log.d("MainActivity", "Latest reading is null.")
                    }
                } else {
                    Log.d("MainActivity", "Unsuccessful response: ${response.code()}")
                }
            }
            override fun onFailure(call: Call<Reading>, t: Throwable) {
                Log.d("MainActivity", "onFailure: ${t.message}")
            }
        })

        val retrofitThresholdData = retrofitBuilder.getThreshold()
        retrofitThresholdData.enqueue(object : Callback<UserThreshold> {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(
                call: Call<UserThreshold>,
                response: Response<UserThreshold>
            ) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null) {
                        val userThreshold = apiResponse.threshold
                        Log.d("MainActivity", "Threshold: $userThreshold")
                        criticalLevelResultTv.text = userThreshold.toInt().toString() + "%"
                        criticalLevelCase()

                    } else {
                        Log.d("MainActivity", "Response body is null.")
                    }
                } else {
                    Log.d("MainActivity", "Unsuccessful response: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<UserThreshold>, t: Throwable) {
                Log.d("MainActivity", "onFailure: ${t.message}")
            }
        })

    }
    fun criticalLevelCase() {
        val criticalLevelText = criticalLevelResultTv.text?.toString()
        val bucketValueText = bucketTv.text?.toString()

        if (!criticalLevelText.isNullOrBlank() && !bucketValueText.isNullOrBlank()) {
            try {
                val criticalLevel = extractNumericValue(criticalLevelText)
                val bucketValue = extractNumericValue(bucketValueText)
                if (criticalLevel > bucketValue) {
                    tvCriticalIndicator.visibility = View.VISIBLE
                    bucketTv.setTextColor(Color.parseColor("#FF0000"))
                } else {
                    tvCriticalIndicator.visibility = View.GONE
                    bucketTv.setTextColor(Color.parseColor("#000000"))
                }
            } catch (e: NumberFormatException) {
                Log.e("MainActivity", "Error converting text to float: $e")
                tvCriticalIndicator.visibility = View.GONE

            }
        } else {
            tvCriticalIndicator.visibility = View.GONE
        }
    }

    private fun extractNumericValue(text: String): Float {
        val regex = Regex("\\d+")
        val matchResult = regex.find(text)
        return matchResult?.value?.toFloat() ?: throw NumberFormatException("Invalid numeric value")
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("notify permission", "can show notifications")
        } else {
            // TODO: Inform user that your app will not show notifications.
            Log.d("notify permission", "can not show notifications")
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM can send notifications
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}