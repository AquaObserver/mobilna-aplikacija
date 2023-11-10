package com.example.aquaobserver

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), View.OnClickListener {
    // place holders za vars dok nema api-a
    private var criticalLvl: Int = 20
    private var maxVolume: Int = 55
    private var currVolume: Int = 30
    private val sdFormat = SimpleDateFormat("hh:mm:ss", Locale.getDefault())
    private val lastUpdated: String = sdFormat.format(Date())

    private lateinit var btnChangeCriticalLevel : Button
    private lateinit var btnMeasurementHistory : Button

    private lateinit var criticalLevelTv: TextView
    private lateinit var criticalLevelResultTv: TextView
    private lateinit var maxVolumeTv : TextView
    private lateinit var maxVolumeResultTv : TextView

    private lateinit var currentVolumeTv : TextView
    private lateinit var currentVolumeResultTv : TextView

    private lateinit var lastUpdatedTv : TextView
    private lateinit var lastUpdatedResultTv : TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnChangeCriticalLevel = findViewById(R.id.btn_change_critical_level)
        btnMeasurementHistory = findViewById(R.id.btn_measurement_history)

        criticalLevelTv = findViewById(R.id.tv_critical_level)
        criticalLevelResultTv = findViewById(R.id.tv_critical_level_result)

        maxVolumeTv = findViewById(R.id.tv_max_volume)
        maxVolumeResultTv = findViewById(R.id.tv_max_volume_result)

        currentVolumeTv = findViewById(R.id.tv_current_volume)
        currentVolumeResultTv = findViewById(R.id.tv_current_volume_result)

        lastUpdatedTv = findViewById(R.id.tv_last_update)
        lastUpdatedResultTv = findViewById(R.id.tv_last_update_result)

        this.criticalLevelResultTv.text = criticalLvl.toString()
        this.maxVolumeResultTv.text = maxVolume.toString() + "L"
        this.currentVolumeResultTv.text = currVolume.toString()
        this.lastUpdatedResultTv.text = lastUpdated

        btnChangeCriticalLevel.setOnClickListener(this)
        btnMeasurementHistory.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btn_change_critical_level -> {
                this.criticalLvl = (1..100).random()
                this.criticalLevelResultTv.text = criticalLvl.toString()
            }
            R.id.btn_measurement_history -> {
                intent = Intent(this, MeasurementsHistory::class.java)
                startActivity(intent)
            }
        }
    }
}