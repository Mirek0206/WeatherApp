package com.example.weatherapp.data

import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.weatherapp.R
import com.example.weatherapp.data.forecastModels.ForecastData
import com.example.weatherapp.fragments.MainWeatherFragment
import java.sql.Date
import java.util.Locale
import kotlin.math.roundToInt

class ForecastAdapter(private val forecastList: List<ForecastData>, private val tempUnit: MainWeatherFragment.TempUnit, private val context: Context) : RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    class ForecastViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvItemTime: TextView = view.findViewById(R.id.tv_Item_time)
        val tvItemStatus: TextView = view.findViewById(R.id.tv_item_status)
        val tvItemTemp: TextView = view.findViewById(R.id.tv_item_temp)
        val imgItem: ImageView = view.findViewById(R.id.img_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.rv_item_layout, parent, false)
        return ForecastViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        val forecastData = forecastList[position]

        // Konwersja temperatury na wybraną jednostkę
        val temp = when (tempUnit) {
            MainWeatherFragment.TempUnit.CELSIUS -> kelvinToCelsius(forecastData.main.temp)
            MainWeatherFragment.TempUnit.FAHRENHEIT -> kelvinToFahrenheit(forecastData.main.temp)
            MainWeatherFragment.TempUnit.KELVIN -> forecastData.main.temp
        }

        holder.tvItemTemp.text = "${temp.roundToInt()}${tempUnit.symbol}"
        holder.tvItemTime.text = formatTime(forecastData.dt)
        holder.tvItemStatus.text = forecastData.weather[0].description

        val iconUrl = "https://openweathermap.org/img/wn/${forecastData.weather[0].icon}@2x.png"
        Glide.with(context).load(iconUrl).into(holder.imgItem)
    }

    override fun getItemCount() = forecastList.size

    private fun formatTime(timeInSeconds: Int): String {
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return dateFormat.format(Date((timeInSeconds * 1000).toLong()))
    }

    private fun kelvinToCelsius(kelvin: Double): Double {
        return kelvin - 273.15
    }

    private fun kelvinToFahrenheit(kelvin: Double): Double {
        return (kelvin * 9/5) - 459.67
    }
}
