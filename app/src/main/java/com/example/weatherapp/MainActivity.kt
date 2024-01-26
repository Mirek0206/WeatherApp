package com.example.weatherapp

import MainWeatherFragment
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.weatherapp.databinding.ActivityMainBinding
import com.mkrdeveloper.weatherappexample.fragments.AdditionalWeatherInfoFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            // Wyświetlanie MainWeatherFragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, MainWeatherFragment())
                .commit()
        }

    }
}
