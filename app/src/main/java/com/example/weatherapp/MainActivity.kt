package com.example.weatherapp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.weatherapp.data.WeatherDataRepository
import com.example.weatherapp.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var weatherDataRepository: WeatherDataRepository

    lateinit var mainWeatherFragment: MainWeatherFragment
    lateinit var additionalWeatherInfoFragment: AdditionalWeatherInfoFragment

    fun updateWeatherAndPollutionData(city: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val apiKey = getString(R.string.api_key)
            val weatherData = weatherDataRepository.getWeatherData(city, apiKey)
            if (weatherData != null) {
                val pollutionData = weatherDataRepository.getPollutionData(weatherData.coord.lat, weatherData.coord.lon, apiKey)

                withContext(Dispatchers.Main) {
                    val mainWeatherFragment = supportFragmentManager.findFragmentByTag("f0") as? MainWeatherFragment
                    val additionalWeatherInfoFragment = supportFragmentManager.findFragmentByTag("f1") as? AdditionalWeatherInfoFragment

                    mainWeatherFragment?.updateUI(weatherData)
                    additionalWeatherInfoFragment?.updateUI(weatherData, pollutionData)

                    saveLastSearchedCity(city)
                }
            } else {
                withContext(Dispatchers.Main) {
                    Log.d("MainActivity", "Nie udało się pobrać danych pogodowych dla miasta: $city")
                }
            }
        }
    }

    private fun saveLastSearchedCity(city: String) {
        val sharedPreferences = getSharedPreferences("WeatherApp", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("lastSearchedCity", city).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        // Ustawienie ViewPager i aktualizacja danych pogodowych
        if (savedInstanceState == null) {
            setupViewPager()
            val lastSearchedCity = weatherDataRepository.getLastSearchedCityOrDefault()
            updateWeatherAndPollutionData(lastSearchedCity)
        }
    }

    private fun setupViewPager() {
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
    }

    inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 2  // Liczba fragmentów

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> MainWeatherFragment().apply {
                    setWeatherDataRepository(weatherDataRepository)
                }
                else -> AdditionalWeatherInfoFragment().apply {
                    setWeatherDataRepository(weatherDataRepository)
                }
            }
        }
    }
}
