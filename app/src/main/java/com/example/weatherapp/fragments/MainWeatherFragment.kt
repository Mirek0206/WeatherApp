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
import com.bumptech.glide.Glide
import com.example.weatherapp.MainActivity
import com.example.weatherapp.data.WeatherDataRepository
import com.example.weatherapp.data.weatherModels.CurrentWeather
import com.example.weatherapp.databinding.FragmentMainWeatherBinding
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class MainWeatherFragment : Fragment() {
    private var _binding: FragmentMainWeatherBinding? = null
    private val binding get() = _binding!!
    private lateinit var weatherDataRepository: WeatherDataRepository
    private var currentWeatherData: CurrentWeather? = null
    private var favoritesDialog: AlertDialog? = null

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
            val currentLocation = getCurrentLocationFromTextView()
            // Przełącz ulubione
            toggleFavorite(currentLocation)
        }

        // Zaktualizuj bieżącą lokalizację wartością z TextView
        saveCurrentLocation(getCurrentLocationFromTextView())
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

        searchView.setOnQueryTextFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                showFavoritesList()
            }
        }
    }



    @SuppressLint("SetTextI18n")
    fun updateUI(weather: CurrentWeather?) {
        this.currentWeatherData = weather
        if (weather != null && _binding != null) {
            binding.tvLocation.text = weather.name
            binding.tvTemp.text = "${weather.main.temp.roundToInt()}°C"
            binding.tvStatus.text = weather.weather.first().description
            binding.tvLatCoordTemp.text = "Lat: ${weather.coord.lat}"
            binding.tvLongCoordTemp.text = "Lon: ${weather.coord.lon}"

            val updateDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(weather.dt * 1000L))
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


    private fun getCurrentLocationFromTextView(): String {
        // Pobiera nazwę miejscowości z TextView i zapisuje jako bieżącą lokalizację
        return binding.tvLocation.text.toString()
    }

    private fun saveCurrentLocation(location: String) {
        val sharedPreferences = activity?.getSharedPreferences("WeatherApp", Context.MODE_PRIVATE) ?: return
        with(sharedPreferences.edit()) {
            putString("currentLocation", location)
            apply()
        }
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
}
