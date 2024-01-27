package com.example.weatherapp

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.weatherapp.data.WeatherDataRepository
import com.example.weatherapp.data.models.CurrentWeather
import com.example.weatherapp.data.pollutionModels.PollutionData
import com.example.weatherapp.databinding.FragmentAdditionalWeatherInfoBinding
import kotlinx.coroutines.*
import java.sql.Date
import java.util.Locale

class AdditionalWeatherInfoFragment : Fragment() {
    private var _binding: FragmentAdditionalWeatherInfoBinding? = null
    private val binding get() = _binding!!
    private lateinit var weatherDataRepository: WeatherDataRepository
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var weatherData: CurrentWeather? = null
    private var pollutionData: PollutionData? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdditionalWeatherInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI(weatherData, pollutionData)
    }

    fun setWeatherDataRepository(repository: WeatherDataRepository) {
        this.weatherDataRepository = repository
    }

    fun updateUI(weather: CurrentWeather?, pollution: PollutionData?) {
        weather?.let {
            updateWeatherUI(it)
        }
        pollution?.let {
            updatePollutionUI(it)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateWeatherUI(weather: CurrentWeather) {
        binding.tvSunrise.text = formatTime(weather.sys.sunrise)
        binding.tvSunset.text = formatTime(weather.sys.sunset)
        binding.tvPressure.text = "${weather.main.pressure} hPa"
        binding.tvHumidity.text = "${weather.main.humidity}%"
        binding.tvWind.text = "${weather.wind.speed} m/s"
    }

    private fun updatePollutionUI(pollution: PollutionData) {
        val airQuality = calculateAirQuality(pollution)
        binding.tvAirQual.text = airQuality
    }

    private fun calculateAirQuality(pollution: PollutionData): String {
        val components = pollution.list[0].components
        val averageIndex = listOf(
            getIndex(components.so2, listOf(20, 80, 250, 350)),
            getIndex(components.no2, listOf(40, 70, 150, 200)),
            getIndex(components.pm10, listOf(20, 50, 100, 200)),
            getIndex(components.pm2_5, listOf(10, 25, 50, 75)),
            getIndex(components.o3, listOf(60, 100, 140, 180)),
            getIndex(components.co, listOf(4400, 9400, 12400, 15400))
        ).average().toInt()

        return when (averageIndex) {
            1 -> "Good"
            2 -> "Fair"
            3 -> "Moderate"
            4 -> "Poor"
            else -> "Very Poor"
        }
    }

    private fun getIndex(value: Double, thresholds: List<Int>): Int {
        return thresholds.indexOfFirst { value < it }.takeIf { it != -1 } ?: thresholds.size
    }

    private fun formatTime(timeInSeconds: Int): String {
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return dateFormat.format(Date((timeInSeconds * 1000).toLong()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
