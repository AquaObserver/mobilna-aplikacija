package com.example.aquaobserver

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.DatePicker
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.example.aquaobserver.api.DateReading
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.MPPointF
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar
import java.util.Locale


class MeasurementsHistory : AppCompatActivity() {

    lateinit var lineChart: LineChart
    private val xValues: List<String> = listOf("0:00", "1:00", "2:00", "3:00", "4:00", "5:00",
        "6:00", "7:00", "8:00", "9:00", "10:00", "11:00",
        "12:00", "13:00", "14:00", "15:00", "16:00", "17:00",
        "18:00", "19:00", "20:00", "21:00", "22:00", "23:00", "24:00")

    val BASE_URL = "http://10.0.2.2:8000/"

    private lateinit var tvDatePicker: TextView
    private lateinit var btnDatePicker: Button

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_measurements_history)

        val btnBack = findViewById<Button>(R.id.btn_back)
        btnBack.setOnClickListener {
            intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        tvDatePicker = findViewById(R.id.tvDate)
        tvDatePicker.text = LocalDate.now().toString()
        btnDatePicker = findViewById(R.id.btnDatePicker)

        val myCalendar = Calendar.getInstance()
        val datePicker = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, month)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateLable(myCalendar)
        }

        btnDatePicker.setOnClickListener {

            DatePickerDialog(this, datePicker, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        prepareLaunch(LocalDate.now().toString())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateLable(myCalendar: Calendar) {

        val myFormat = "yyyy-MM-dd"
        val sdf=SimpleDateFormat(myFormat, Locale("HR"))
        tvDatePicker.setText(sdf.format(myCalendar.time))
        prepareLaunch(tvDatePicker.text.toString())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun prepareLaunch(date: String){
        lifecycleScope.launch {
            try {
                val readings = getDateReading(date)
                createLineChart(readings)
                Log.d("MeasurementsHistory", "Water: " + readings[0].waterLevel)
            } catch (e: Exception) {
                Log.e("MeasurementsHistory", "Error: ${e.message}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createLineChart(readings: List<DateReading>) {
        lineChart = findViewById(R.id.chart)
        lineChart.axisRight.setDrawLabels(false)
        lineChart.description = null

        val xAxis: XAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(xValues)
        xAxis.axisLineColor = Color.BLACK
        xAxis.axisLineWidth = 2f
        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = 24f
        xAxis.labelCount = 6
        xAxis.textSize = 13f

        val yAxis: YAxis = lineChart.axisLeft
        yAxis.axisMinimum = 0f
        yAxis.axisMaximum = 100f
        yAxis.axisLineWidth = 2f
        yAxis.axisLineColor = Color.BLACK
        yAxis.labelCount = 10
        yAxis.valueFormatter = IntPercentageFormatter()
        yAxis.textSize = 13f

        val entries1: MutableList<Entry> = mutableListOf()

        readings.forEachIndexed { index, reading ->
            val time = LocalTime.parse(reading.time)
            if (time.minute == 0 && time.second == 0 || reading.time == "23:58:00") {
                val hour = time.hour.toFloat() + (time.minute.toFloat() / 60f) + (time.second.toFloat() / 3600f)
                entries1.add(Entry(hour, reading.waterLevel.toFloat()))
            }
        }

        val dataSet1 = LineDataSet(entries1, "Razina vode")
        dataSet1.setDrawValues(false)
        dataSet1.color = Color.parseColor("#1AA4C2")
        dataSet1.formSize
        dataSet1.setDrawFilled(true)
        dataSet1.setFillColor(Color.parseColor("#1AA4C2"))
        dataSet1.fillAlpha = 100

        val legend = lineChart.legend
        legend.textSize = 20f
        legend.yOffset = 50f
        legend.xOffset = 90f

        // Set a custom MarkerView
        val markerView = CustomMarkerView(this, R.layout.marker_view)
        markerView.chartView = lineChart
        lineChart.marker = markerView

        // Set an OnChartValueSelectedListener to handle value selection events
        lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                if (e != null) {
                    val time = calculateTimeFromEntry(e.x)
                    markerView.setEntry(e, time)
                    lineChart.invalidate()
                }
            }

            override fun onNothingSelected() {
                // Handle when nothing is selected
            }
        })

        val lineData = LineData(dataSet1)
        lineChart.data = lineData
        lineChart.invalidate()
    }

    private class IntPercentageFormatter : ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            return "${value.toInt()}%"
        }
    }

    private suspend fun getDateReading(date: String): List<DateReading> {
        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BASE_URL)
            .build()
            .create(ApiInterface::class.java)

        return try {
            val apiResponse = retrofitBuilder.getDateReadings(date)
            apiResponse.data
        } catch (e: Exception) {
            emptyList() // or throw an exception if needed
        }
    }

    private fun calculateTimeFromEntry(entryX: Float): String {
        val hour = entryX.toInt()
        val minute = ((entryX - hour) * 60).toInt()
        return String.format("%02d:%02d:00", hour, minute)
    }
}

class CustomMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {
    private val tvContent: TextView = findViewById(R.id.tvContent)

    fun setEntry(entry: Entry, time: String) {
        //tvContent.text = String.format("Time: %s\nWater Level: %.2f", time, entry.y.toFloat())
        tvContent.text = "Vrijeme: ${time} \nRazina vode: ${entry.y.toString()}%"
        Log.d("MeasurementsHistory", "E " + entry.y)
    }

    override fun refreshContent(entry: Entry?, highlight: Highlight?) {
        // Do nothing, we handle content in setEntry method
    }

    override fun getOffset(): MPPointF {
        // Adjust the offset as needed to properly position the MarkerView
        return MPPointF(-width / 2f, -height.toFloat())
    }
}


