package com.example.weatherapp

import androidx.fragment.app.Fragment
import com.example.weatherapp.databinding.FragmentForecastBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


class ForecastFragment : Fragment() {
    private var _binding: FragmentForecastBinding? = null
    private val binding get() = _binding!!
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
}