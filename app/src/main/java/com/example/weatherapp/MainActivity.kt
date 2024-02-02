package com.example.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.weatherapp.data.WeatherDataRepository
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.fragments.AdditionalWeatherInfoFragment
import com.example.weatherapp.fragments.ForecastFragment
import com.example.weatherapp.fragments.MainWeatherFragment
import com.example.weatherapp.data.SharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var weatherDataRepository: WeatherDataRepository
    private lateinit var sharedViewModel: SharedViewModel

    lateinit var mainWeatherFragment: MainWeatherFragment
    lateinit var additionalWeatherInfoFragment: AdditionalWeatherInfoFragment
    lateinit var forecastFragment: ForecastFragment

    fun updateWeatherAndPollutionData(city: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val apiKey = getString(R.string.api_key)
            val weatherData = weatherDataRepository.getWeatherData(city, apiKey)
            val pollutionData = weatherData?.let {
                weatherDataRepository.getPollutionData(it.coord.lat, it.coord.lon, apiKey)
            }
            val forecastData = weatherData?.let {
                weatherDataRepository.getForecastData(it.coord.lat, it.coord.lon, apiKey)
            }

            Log.d("MainActivity", "Weather data: $weatherData")
            Log.d("MainActivity", "Pollution data: $pollutionData")
            Log.d("MainActivity", "Forecast data: $forecastData")

            withContext(Dispatchers.Main) {
                if (weatherData != null && pollutionData != null && forecastData != null) {
                    val currentTempUnit = sharedViewModel.tempUnit.value ?: MainWeatherFragment.TempUnit.CELSIUS

                    mainWeatherFragment.updateUI(weatherData)
                    additionalWeatherInfoFragment.updateUI(weatherData, pollutionData)
                    forecastFragment.updateUI(forecastData, currentTempUnit)

                    saveLastSearchedCity(city)
                } else {
                    if (!isNetworkAvailable()) {
                        Toast.makeText(this@MainActivity, "Brak danych - brak internetu!", Toast.LENGTH_LONG).show()
                    }
                    else
                    {
                        Toast.makeText(this@MainActivity, "Brak danych - problem z serwisem OpenWeather!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun saveLastSearchedCity(city: String) {
        val sharedPreferences = getSharedPreferences("WeatherApp", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("lastSearchedCity", city).apply()
    }

    fun getLastSearchedCityOrDefault(): String {
        val sharedPreferences = getSharedPreferences("WeatherApp", Context.MODE_PRIVATE)
        return sharedPreferences.getString("lastSearchedCity", "Warsaw") ?: "Warsaw"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicjalizacja weatherDataRepository
        weatherDataRepository = WeatherDataRepository(this)

        // Tworzenie fragmentów i przekazanie do nich weatherDataRepository
        mainWeatherFragment = MainWeatherFragment().apply {
            setWeatherDataRepository(weatherDataRepository)
        }
        additionalWeatherInfoFragment = AdditionalWeatherInfoFragment().apply {
            setWeatherDataRepository(weatherDataRepository)
        }
        forecastFragment = ForecastFragment().apply {
            setWeatherDataRepository(weatherDataRepository)
        }

        // Ustawienie ViewPager i aktualizacja danych pogodowych
        if (savedInstanceState == null) {
            setupViewPager()
            val lastSearchedCity = getLastSearchedCityOrDefault()
            Log.d("MainActivity", lastSearchedCity)
            updateWeatherAndPollutionData(lastSearchedCity)
        }
    }


    private fun setupViewPager() {
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        mainWeatherFragment = MainWeatherFragment().apply {
            setWeatherDataRepository(weatherDataRepository)
        }
        additionalWeatherInfoFragment = AdditionalWeatherInfoFragment().apply {
            setWeatherDataRepository(weatherDataRepository)
        }
        forecastFragment = ForecastFragment().apply {
            setWeatherDataRepository(weatherDataRepository)
        }
    }

    inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> mainWeatherFragment
                1 -> additionalWeatherInfoFragment
                2 -> forecastFragment
                else -> throw IllegalStateException("Invalid position")
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

}
