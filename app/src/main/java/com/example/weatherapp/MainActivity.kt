package com.example.weatherapp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.weatherapp.data.WeatherDataRepository
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.fragments.AdditionalWeatherInfoFragment
import com.example.weatherapp.fragments.ForecastFragment
import com.example.weatherapp.fragments.MainWeatherFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var weatherDataRepository: WeatherDataRepository

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

            withContext(Dispatchers.Main) {
                if (weatherData != null && pollutionData != null && forecastData != null) {
                    mainWeatherFragment.updateUI(weatherData)
                    additionalWeatherInfoFragment.updateUI(weatherData, pollutionData)
                    forecastFragment.updateUI(forecastData)
                    saveLastSearchedCity(city)
                } else {
                    Toast.makeText(this@MainActivity, "Brak aktualnych danych - problem z połączeniem", Toast.LENGTH_LONG).show()
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
        forecastFragment = ForecastFragment().apply {
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
                2 -> forecastFragment // Dodanie ForecastFragment
                else -> throw IllegalStateException("Invalid position")
            }
        }
    }
}
