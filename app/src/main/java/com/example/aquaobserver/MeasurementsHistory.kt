package com.example.aquaobserver

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.DatePicker
import android.widget.TextView
import android.widget.Toast
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
    private val xValues1: List<String> = listOf (
        "0:00", "1:00", "2:00", "3:00", "4:00", "5:00",
        "6:00", "7:00", "8:00", "9:00", "10:00", "11:00",
        "12:00", "13:00", "14:00", "15:00", "16:00", "17:00",
        "18:00", "19:00", "20:00", "21:00", "22:00", "23:00", "24:00"
    )

    private var itemSelected: Any? = null
    private var GlobalStartHour = 0
    private var GlobalEndHour = 0

    val BASE_URL = "https://polliwog-enormous-walrus.ngrok-free.app/"
    // val BASE_URL = "http://10.0.2.2:8000/"

    private lateinit var tvDatePicker: TextView
    private lateinit var btnDatePicker: Button
    private lateinit var tvInvalidDate: TextView

    private val handler = Handler()
    private val delayMillis: Long = 60_000 // 60 seconds

    private val fetchThresholdRunnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun run() {
            prepareLaunch(tvDatePicker.text.toString())
            handler.postDelayed(this, delayMillis)
        }
    }

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
        tvInvalidDate = findViewById(R.id.tvInvalidDate)

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

        val items = listOf(
            "Cijeli dan",
            "00:00 do 01:00", "01:00 do 02:00", "02:00 do 03:00", "03:00 do 04:00", "04:00 do 05:00",
            "05:00 do 06:00", "06:00 do 07:00", "07:00 do 08:00", "08:00 do 09:00", "09:00 do 10:00",
            "10:00 do 11:00", "11:00 do 12:00", "12:00 do 13:00", "13:00 do 14:00", "14:00 do 15:00",
            "15:00 do 16:00", "16:00 do 17:00", "17:00 do 18:00", "18:00 do 19:00", "19:00 do 20:00",
            "20:00 do 21:00", "21:00 do 22:00", "22:00 do 23:00", "23:00 do 24:00"
        )
        val autoComplete : AutoCompleteTextView = findViewById(R.id.auto_complete)
        val adapter = ArrayAdapter(this, R.layout.list_item,items)
        autoComplete.setAdapter(adapter)
        val currentHour = (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 1) % 24
        val defaultHourRange = String.format("%02d:00 do %02d:00", currentHour, (currentHour + 1) % 24)
        autoComplete.setText(defaultHourRange, false)
        autoComplete.onItemClickListener = AdapterView.OnItemClickListener { _, _, _, _ ->
            itemSelected = autoComplete.text.toString()
            Toast.makeText(this, "Item: $itemSelected", Toast.LENGTH_SHORT).show()
            prepareLaunch(tvDatePicker.text.toString())
            lineChart.invalidate()
        }
        itemSelected = autoComplete.text.toString()
        prepareLaunch(LocalDate.now().toString())
        handler.post(fetchThresholdRunnable)
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
            } catch (e: Exception) {
                Log.e("MeasurementsHistory", "Error: ${e.message}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createLineChart(readings: List<DateReading>) {
        lineChart = findViewById(R.id.chart)
        if (readings.isEmpty()) {
            lineChart.visibility = View.GONE
            tvInvalidDate.visibility = View.VISIBLE
            lineChart.invalidate()
            return
        } else {
            lineChart.visibility = View.VISIBLE
            tvInvalidDate.visibility = View.GONE
            lineChart.invalidate()
        }
        lineChart.axisRight.setDrawLabels(false)
        lineChart.description = null
        lineChart.highlightValue(null)

        val xAxis: XAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.axisLineColor = Color.BLACK
        xAxis.axisLineWidth = 2f
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

        if(itemSelected == "Cijeli dan" || itemSelected == null) {
            xAxis.valueFormatter = IndexAxisValueFormatter(xValues1)
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = 24f
            xAxis.labelCount = 6
            readings.forEachIndexed { index, reading ->
                val time = LocalTime.parse(reading.time)
                if (time.minute == 0 || reading.time == "23:58:00") {
                    val hour = time.hour.toFloat() + (time.minute.toFloat() / 60f) + (time.second.toFloat() / 3600f)
                    entries1.add(Entry(hour, reading.waterLevel.toFloat()))
                }
            }
        } else {
            val regexPattern = Regex("""(\d{2}):\d{2} do (\d{2}):\d{2}""")
            val matchResult = regexPattern.find(itemSelected.toString())
            val (startHour, endHour )= matchResult?.destructured?.let { (start, end) ->
                Pair(start.toInt(), end.toInt())
            } ?: run { return }
            GlobalEndHour = endHour
            GlobalStartHour = startHour
            var xValues2: List<String> = mutableListOf<String>().apply {
                for (minute in 0..59 step 1) {
                    add(String.format("%02d:%02d", startHour, minute))
                }
                add(String.format("%02d:00", endHour))
            }
            xAxis.valueFormatter = IndexAxisValueFormatter(xValues2)
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = 60f
            xAxis.labelCount = 6
            readings.forEachIndexed { _, reading ->
                val time = LocalTime.parse(reading.time)
                if (time.hour == startHour || (time.hour == endHour && time.minute == 0)) {
                    if (time.minute == 0) {
                        if (time.hour == startHour) {
                            entries1.add(Entry(0f + time.second.toFloat()/60f, reading.waterLevel.toFloat()))
                        } else {
                            entries1.add(Entry(60f + time.second.toFloat()/60f, reading.waterLevel.toFloat()))
                        }
                    } else {
                        entries1.add(Entry(time.minute.toFloat() + time.second.toFloat()/60f, reading.waterLevel.toFloat()))
                    }
                }
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


        var markerView = CustomMarkerView(this, R.layout.marker_view)
        markerView.chartView = lineChart
        lineChart.marker = markerView

        lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                if (e != null) {
                    val time = calculateTimeFromEntry(e.x)
                    markerView.setEntry(e, time)
                    lineChart.invalidate()
                }
            }
            override fun onNothingSelected() {}
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
            emptyList()
        }
    }

    private fun calculateTimeFromEntry(entryX: Float): String {
        if(itemSelected == "Cijeli dan" || itemSelected == null) {
            val hour = entryX.toInt()
            val minute = ((entryX - hour) * 60).toInt()
            return String.format("%02d:%02d:00", hour, minute)
        } else{
            if(entryX == 60f){
                val hour = GlobalEndHour
                val minute = 0
                val seconds = (((entryX % 1) * 100) * 0.6).toInt()
                return String.format("%02d:%02d:%02d", hour, minute, seconds)
            }
            else{
                val hour = GlobalStartHour
                val minute = entryX.toInt()
                val seconds = (((entryX % 1) * 100) * 0.6).toInt()
                return String.format("%02d:%02d:%02d", hour, minute, seconds)
            }
        }
    }
}

class CustomMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {
    private val tvContent: TextView = findViewById(R.id.tvContent)
    fun setEntry(entry: Entry, time: String) {
        tvContent.text = "Vrijeme: ${time} \nRazina vode: ${entry.y.toString()}%"
    }

    override fun refreshContent(entry: Entry?, highlight: Highlight?) {}

    override fun getOffset(): MPPointF {
        return MPPointF(-width / 2f, -height.toFloat())
    }
}


