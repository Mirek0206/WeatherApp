package com.example.weatherapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.weatherapp.data.models.CurrentWeather
import com.example.weatherapp.data.pollutionModels.PollutionData

class SharedViewModel : ViewModel() {
    val weatherData = MutableLiveData<CurrentWeather>()
    val pollutionData = MutableLiveData<PollutionData>()

    fun updateData(weather: CurrentWeather, pollution: PollutionData) {
        weatherData.value = weather
        pollutionData.value = pollution
    }
}