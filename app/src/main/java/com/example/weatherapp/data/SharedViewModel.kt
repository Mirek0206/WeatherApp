package com.example.weatherapp.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.weatherapp.fragments.MainWeatherFragment

class SharedViewModel : ViewModel() {
    val tempUnit: MutableLiveData<MainWeatherFragment.TempUnit> = MutableLiveData(MainWeatherFragment.TempUnit.CELSIUS)
}