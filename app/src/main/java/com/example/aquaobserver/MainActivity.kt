package com.example.aquaobserver

import android.annotation.SuppressLint
import android.content.Intent
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
import androidx.appcompat.app.AlertDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var criticalLvl: Int = 20
    private var maxVolume: Int = 55
    private var currVolume: Int = 60
    private val sdFormat = SimpleDateFormat("hh:mm:ss", Locale.getDefault())
    private val lastUpdated: String = sdFormat.format(Date())

    private lateinit var btnChangeCriticalLevel : Button
    private lateinit var btnMeasurementHistory : Button

    private lateinit var bucketProgressBar: ProgressBar

    private lateinit var criticalLevelResultTv: TextView
    private lateinit var maxVolumeResultTv : TextView
    private lateinit var currentVolumeResultTv : TextView
    private lateinit var lastUpdatedResultTv : TextView
    private lateinit var bucketTv: TextView

    val BASE_URL = "http://127.0.0.1:8000/"

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

        criticalLevelResultTv.text = criticalLvl.toString()

        maxVolumeResultTv.text = maxVolume.toString() + "L"
        currentVolumeResultTv.text = currVolume.toString()
        lastUpdatedResultTv.text = lastUpdated

        //bucketTv.text = currVolume.toString() + "%"
        //bucketProgressBar.progress = currVolume

        btnChangeCriticalLevel.setOnClickListener(this)
        btnMeasurementHistory.setOnClickListener(this)

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
                    this.criticalLvl = newCriticalLevel.text.toString().toInt()
                    this.criticalLevelResultTv.text = newCriticalLevel.text
                    Toast.makeText(this, "Kriticna razina promijenjena", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
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

    private fun getMyData(){
        val retrofitBuilder= Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BASE_URL)
            .build()
            .create(ApiInterface::class.java)

        val retrofitData= retrofitBuilder.getData()

        retrofitData.enqueue(object : Callback<List<Reading>?> {
            override fun onResponse(
                call: Call<List<Reading>?>,
                response: Response<List<Reading>?>
            ) {
                val responseBody = response.body()!!
                val firstValue = responseBody[0].waterLevel
                val myStringBuilder = StringBuilder()

                for(myData in responseBody) {
                    myStringBuilder.append(myData.waterLevel)
                    myStringBuilder.append("\n")
                }
                Log.d("MainActivity", "ejla" + firstValue.toString())
                bucketTv.text  = firstValue.toString() + "%"
                bucketProgressBar.progress = firstValue.toInt()
            }

            override fun onFailure(call: Call<List<Reading>?>, t: Throwable) {
                Log.d("MainActivity", "onFailure: " + t.message)
            }
        })
    }
}