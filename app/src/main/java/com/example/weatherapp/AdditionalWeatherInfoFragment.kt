package com.mkrdeveloper.weatherappexample.fragments

import MainWeatherFragment
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.weatherapp.R
import com.example.weatherapp.databinding.FragmentAdditionalWeatherInfoBinding
import com.example.weatherapp.utils.RetrofitInstance
import com.example.weatherapp.data.pollutionModels.Components
import com.example.weatherapp.data.pollutionModels.PollutionData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdditionalWeatherInfoFragment : Fragment() {
    private var _binding: FragmentAdditionalWeatherInfoBinding? = null
    private val binding get() = _binding!!
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
}
