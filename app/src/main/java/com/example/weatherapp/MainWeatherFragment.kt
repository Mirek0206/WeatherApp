package com.example.weatherapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.weatherapp.data.WeatherDataRepository
import com.example.weatherapp.data.models.CurrentWeather
import com.example.weatherapp.databinding.FragmentMainWeatherBinding
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainWeatherFragment : Fragment() {
    private var _binding: FragmentMainWeatherBinding? = null
    private val binding get() = _binding!!
    private lateinit var weatherDataRepository: WeatherDataRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun setWeatherDataRepository(repository: WeatherDataRepository) {
        this.weatherDataRepository = repository
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicjalizacja SearchView
        val searchView = binding.searchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {

                    // Sprawdzenie, czy weatherDataRepository został zainicjalizowany
                    if (::weatherDataRepository.isInitialized) {
                        Log.d("MainActivity", "Coś tutaj nie działa :(")
                        // Wywołanie metody w MainActivity
                        (activity as? MainActivity)?.updateWeatherAndPollutionData(it)
                    }
                }
                searchView.clearFocus() // Usunięcie fokusu z SearchView
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    @SuppressLint("SetTextI18n")
    fun updateUI(weather: CurrentWeather) {
        binding.tvLocation.text = weather.name
        binding.tvTemp.text = "${weather.main.temp}°C"
        binding.tvStatus.text = weather.weather.first().description
        binding.tvLatCoordTemp.text = "Lat: ${weather.coord.lat}"
        binding.tvLongCoordTemp.text = "Lon: ${weather.coord.lon}"

        // Formatowanie i wyświetlanie daty ostatniej aktualizacji
        val updateDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(weather.dt * 1000L))
        binding.tvUpdateTime.text = "Last update: $updateDate"

        val iconCode = weather.weather.first().icon
        val imageUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
        loadWeatherIcon(imageUrl)
    }

    private fun loadWeatherIcon(url: String) {
        Glide.with(this)
            .load(url)
            .into(binding.imgWeather)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
