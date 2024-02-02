package com.example.weatherapp.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.weatherapp.MainActivity
import com.example.weatherapp.data.WeatherDataRepository
import com.example.weatherapp.data.weatherModels.CurrentWeather
import com.example.weatherapp.databinding.FragmentMainWeatherBinding
import com.example.weatherapp.data.SharedViewModel
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class MainWeatherFragment : Fragment() {
    enum class TempUnit(val symbol: String) {
        CELSIUS("°C"),
        FAHRENHEIT("°F"),
        KELVIN("K")
    }

    private lateinit var weatherDataRepository: WeatherDataRepository
    private lateinit var sharedViewModel: SharedViewModel
    private var _binding: FragmentMainWeatherBinding? = null
    private val binding get() = _binding!!
    private var currentWeatherData: CurrentWeather? = null
    private var favoritesDialog: AlertDialog? = null
    private var currentTempUnit = TempUnit.CELSIUS
    private var originalTempInCelsius: Double = 0.0





    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun setWeatherDataRepository(repository: WeatherDataRepository) {
        this.weatherDataRepository = repository
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearchView()
        updateUI(currentWeatherData)

        binding.imgAddToFavorites.setOnClickListener {
            // Pobierz aktualną lokalizację
            val currentLocation = (activity as MainActivity).getLastSearchedCityOrDefault()
            // Przełącz ulubione
            toggleFavorite(currentLocation)
        }

        // Ustawienie OnClickListener na tv_update_time
        binding.tvUpdateTime.setOnClickListener {
            refreshData()
        }

        binding.tvTemp.setOnClickListener {
            toggleTemperatureUnit()
        }
    }

    private fun setupSearchView() {
        val searchView = binding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    if (::weatherDataRepository.isInitialized) {
                        (activity as? MainActivity)?.updateWeatherAndPollutionData(it)
                    }
                }
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean = false
        })

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showFavoritesList()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateUI(weather: CurrentWeather?) {
        this.currentWeatherData = weather
        if (weather != null && _binding != null) {
            val updateDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(weather.dt * 1000L))
            originalTempInCelsius = weather.main.temp // Zakładamy, że temp jest w Celsjuszach
            updateTemperatureDisplay(originalTempInCelsius, currentTempUnit)

            binding.tvLocation.text = weather.name
            binding.tvStatus.text = weather.weather.first().description
            binding.tvLatCoordTemp.text = "Lat: ${weather.coord.lat}"
            binding.tvLongCoordTemp.text = "Lon: ${weather.coord.lon}"
            binding.tvUpdateTime.text = "Last update: $updateDate"

            val iconCode = weather.weather.first().icon
            val imageUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
            loadWeatherIcon(imageUrl)
        }
    }

    private fun loadWeatherIcon(url: String) {
        Glide.with(this).load(url).into(binding.imgWeather)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        favoritesDialog?.dismiss()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        updateUI(currentWeatherData)
    }

    @SuppressLint("MutatingSharedPrefs")
    private fun toggleFavorite(currentLocation: String) {
        // Sprawdź, czy currentLocation nie jest wartością domyślną
        if (currentLocation == "DefaultLocation") {
            Toast.makeText(requireContext(), "Wybierz prawidłową lokalizację", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPreferences = requireActivity().getSharedPreferences("Favorites", Context.MODE_PRIVATE)
        val favorites = sharedPreferences.getStringSet("favorites", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        val message: String = if (favorites.contains(currentLocation)) {
            favorites.remove(currentLocation)
            "$currentLocation usunięte z ulubionych"
        } else {
            favorites.add(currentLocation)
            "$currentLocation dodane do ulubionych"
        }

        with(sharedPreferences.edit()) {
            putStringSet("favorites", favorites)
            apply()
        }

        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }



    private fun showFavoritesList() {
        val sharedPreferences = requireActivity().getSharedPreferences("Favorites", Context.MODE_PRIVATE)
        val favoritesSet = sharedPreferences.getStringSet("favorites", setOf()) ?: setOf()
        val favoritesList = favoritesSet.toList()

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Wybierz ulubioną miejscowość")

        builder.setItems(favoritesList.toTypedArray()) { dialog, which ->
            val selectedLocation = favoritesList[which]
            binding.searchView.setQuery(selectedLocation, true)
            favoritesDialog?.dismiss()
        }

        favoritesDialog = builder.create()
        favoritesDialog?.listView?.isFocusable = false
        favoritesDialog?.listView?.isFocusableInTouchMode = false
        favoritesDialog?.show()
    }

    private fun refreshData() {
        val currentLocation = (activity as MainActivity).getLastSearchedCityOrDefault()
        if (currentLocation.isNotBlank() && ::weatherDataRepository.isInitialized) {
            (activity as MainActivity).updateWeatherAndPollutionData(currentLocation, true)
            } else {
                Toast.makeText(requireContext(), "Nie można odświeżyć danych: brak lokalizacji", Toast.LENGTH_SHORT).show()
            }
    }

    private fun toggleTemperatureUnit() {
        currentTempUnit = when (currentTempUnit) {
            TempUnit.CELSIUS -> TempUnit.FAHRENHEIT
            TempUnit.FAHRENHEIT -> TempUnit.KELVIN
            TempUnit.KELVIN -> TempUnit.CELSIUS
        }
        sharedViewModel.tempUnit.value = currentTempUnit
    }

    private fun celsiusToFahrenheit(celsius: Double): Double {
        return (celsius * 9/5) + 32
    }

    private fun celsiusToKelvin(celsius: Double): Double {
        return celsius + 273.15
    }

    private fun updateTemperatureDisplay(temperature: Double, unit: TempUnit) {
        val convertedTemp = when (unit) {
            TempUnit.CELSIUS -> temperature
            TempUnit.FAHRENHEIT -> celsiusToFahrenheit(temperature)
            TempUnit.KELVIN -> celsiusToKelvin(temperature)
        }
        binding.tvTemp.text = String.format(Locale.getDefault(), "%.1f%s", convertedTemp, unit.symbol)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        sharedViewModel.tempUnit.observe(this, androidx.lifecycle.Observer {
            currentTempUnit = it
            updateUI(currentWeatherData)
        })
    }
}
