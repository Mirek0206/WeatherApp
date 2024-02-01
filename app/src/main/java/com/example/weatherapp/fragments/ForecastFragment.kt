package com.example.weatherapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.weatherapp.data.ForecastAdapter
import com.example.weatherapp.data.WeatherDataRepository
import com.example.weatherapp.data.forecastModels.Forecast
import com.example.weatherapp.databinding.FragmentForecastBinding
import com.example.weatherapp.data.SharedViewModel

class ForecastFragment : Fragment() {
    private lateinit var weatherDataRepository: WeatherDataRepository
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var adapter: ForecastAdapter
    private var _binding: FragmentForecastBinding? = null
    private val binding get() = _binding!!
    private var forecast: Forecast? = null
    private var currentTempUnit = MainWeatherFragment.TempUnit.CELSIUS

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentForecastBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun setWeatherDataRepository(repository: WeatherDataRepository) {
        this.weatherDataRepository = repository
    }

    fun updateUI(forecast: Forecast?, tempUnit: MainWeatherFragment.TempUnit) {
        this.forecast = forecast

        if (isAdded && isVisible && _binding != null) {
            forecast?.let {
                val adapter = ForecastAdapter(forecast.list, tempUnit, requireContext())
                binding.rvForecast.layoutManager = GridLayoutManager(context, 2)
                binding.rvForecast.adapter = adapter
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        updateUI(forecast, currentTempUnit)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        // Inicjalizacja adaptera z początkową jednostką temperatury
        adapter = ForecastAdapter(listOf(), currentTempUnit, requireContext())
        binding.rvForecast.layoutManager = GridLayoutManager(context, 2)
        binding.rvForecast.adapter = adapter

        sharedViewModel.tempUnit.observe(viewLifecycleOwner, Observer { tempUnit ->
            currentTempUnit = tempUnit
            if (forecast != null) {
                adapter = ForecastAdapter(forecast!!.list, tempUnit, requireContext())
                binding.rvForecast.adapter = adapter
            }
        })

        updateUI(forecast, currentTempUnit)
    }
}
