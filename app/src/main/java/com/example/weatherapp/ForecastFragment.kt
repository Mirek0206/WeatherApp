package com.example.weatherapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.weatherapp.data.ForecastAdapter
import com.example.weatherapp.data.WeatherDataRepository
import com.example.weatherapp.data.forecastModels.Forecast
import com.example.weatherapp.databinding.FragmentForecastBinding

class ForecastFragment : Fragment() {
    private var _binding: FragmentForecastBinding? = null
    private val binding get() = _binding!!
    private lateinit var weatherDataRepository: WeatherDataRepository
    private var forecast: Forecast? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentForecastBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI(forecast)
    }

    fun setWeatherDataRepository(repository: WeatherDataRepository) {
        this.weatherDataRepository = repository
    }

    fun updateUI(forecast: Forecast?) {
        this.forecast = forecast

        if (isAdded && isVisible && _binding != null) {
            forecast?.let {
                val adapter = ForecastAdapter(forecast.list, requireContext())
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
        updateUI(forecast)
    }
}
