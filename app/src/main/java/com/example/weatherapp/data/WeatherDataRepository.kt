package com.example.weatherapp.data

import android.content.Context
import android.util.Log
import com.example.weatherapp.data.forecastModels.Forecast
import com.example.weatherapp.data.weatherModels.CurrentWeather
import com.example.weatherapp.data.pollutionModels.PollutionData
import com.example.weatherapp.utils.RetrofitInstance
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*

class WeatherDataRepository(private val context: Context) {
    private val updateIntervalMillis: Long = 3600000 // 1 hour in ms

    private suspend fun fetchWeatherDataFromApi(city: String, apiKey: String): CurrentWeather? {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.api.getCurrentWeather(city, "metric", apiKey)
                if (response.isSuccessful) {
                    response.body()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun fetchPollutionDataFromApi(lat: Double, lon: Double, apiKey: String): PollutionData? {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.api.getPollution(lat, lon, apiKey)
                if (response.isSuccessful) {
                    response.body()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun fetchForecastDataFromApi(lat: Double, lon: Double, apiKey: String): Forecast? {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.api.getForecast(lat, lon, apiKey, "standard")
                if (response.isSuccessful) {
                    response.body()
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.d("MainActivity", "Wyjątek w fetchForecastDataFromApi: ${e.message}")
                null
            }
        }
    }

    suspend fun getWeatherData(city: String, apiKey: String, forceUpdate: Boolean = false): CurrentWeather? {
        val (currentWeatherFromFile, lastUpdateTime, cityName) = readWeatherDataFromFile()
        val currentTime = System.currentTimeMillis()
        val isDataValid = !forceUpdate && currentWeatherFromFile != null &&
                currentTime - lastUpdateTime < updateIntervalMillis &&
                cityName.equals(city, ignoreCase = true)

        return if (isDataValid) {
            Log.d("MainActivity", "Weather data valid")
            currentWeatherFromFile
        } else {
            val currentWeatherFromApi = fetchWeatherDataFromApi(city, apiKey)
            currentWeatherFromApi?.let {
                val weatherDataJson = Gson().toJson(it)
                saveWeatherDataToFile(weatherDataJson, city)
            }
            currentWeatherFromApi
        }
    }

    suspend fun getPollutionData(lat: Double, lon: Double, apiKey: String, forceUpdate: Boolean = false): PollutionData? {
        val (pollutionDataFromFile, lastUpdateTime) = readPollutionDataFromFile()
        val currentTime = System.currentTimeMillis()
        val isDataValid = !forceUpdate && pollutionDataFromFile != null &&
                currentTime - lastUpdateTime < updateIntervalMillis &&
                pollutionDataFromFile.coord.lat.equals(lat) &&
                pollutionDataFromFile.coord.lon.equals(lon)

        return if (isDataValid) {
            Log.d("MainActivity", "Pollution data valid")
            pollutionDataFromFile
        } else {
            val pollutionDataFromApi = fetchPollutionDataFromApi(lat, lon, apiKey)
            pollutionDataFromApi?.let {
                val pollutionDataJson = Gson().toJson(it)
                savePollutionDataToFile(pollutionDataJson)
            }
            pollutionDataFromApi
        }
    }

    suspend fun getForecastData(lat: Double, lon: Double, apiKey: String, forceUpdate: Boolean = false): Forecast? {
        val (forecastDataFromFile, lastUpdateTime) = readForecastDataFromFile()

        val currentTime = System.currentTimeMillis()
        val isDataValid = !forceUpdate && forecastDataFromFile != null &&
                currentTime - lastUpdateTime < updateIntervalMillis &&
                forecastDataFromFile.city.coord.lat.equals(lat) &&
                forecastDataFromFile.city.coord.lon.equals(lon)

        return if (isDataValid) {
            Log.d("MainActivity", "Forecast data valid")
            forecastDataFromFile
        } else {
            val forecastDataFromApi = fetchForecastDataFromApi(lat, lon, apiKey)
            forecastDataFromApi?.let {
                val forecastDataJson = Gson().toJson(it)
                saveForecastDataToFile(forecastDataJson)
            }
            forecastDataFromApi
        }
    }

    private fun readWeatherDataFromFile(): Triple<CurrentWeather?, Long, String?> {
        try {
            val fis = context.openFileInput("weather_data.json")
            val isr = InputStreamReader(fis)
            val bufferedReader = BufferedReader(isr)
            val stringBuilder = StringBuilder()
            var text: String?
            while (bufferedReader.readLine().also { text = it } != null) {
                stringBuilder.append(text)
            }
            fis.close()
            val fullData = JSONObject(stringBuilder.toString())
            val weatherData = fullData.getString("weatherData")
            val lastUpdateTime = fullData.getLong("timestamp")
            val cityName = fullData.optString("cityName") // Odczyt nazwy miasta
            return Triple(Gson().fromJson(weatherData, CurrentWeather::class.java), lastUpdateTime, cityName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Triple(null, 0, null)
    }

    private fun readPollutionDataFromFile(): Pair<PollutionData?, Long> {
        try {
            val fis = context.openFileInput("pollution_data.json")
            val isr = InputStreamReader(fis)
            val bufferedReader = BufferedReader(isr)
            val stringBuilder = StringBuilder()
            var text: String?
            while (bufferedReader.readLine().also { text = it } != null) {
                stringBuilder.append(text)
            }
            fis.close()
            val fullData = JSONObject(stringBuilder.toString())
            val pollutionData = fullData.getString("pollutionData")
            val lastUpdateTime = fullData.getLong("timestamp")
            return Pair(Gson().fromJson(pollutionData, PollutionData::class.java), lastUpdateTime)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(null, 0)
    }

    private fun readForecastDataFromFile(): Pair<Forecast?, Long> {
        try {
            val fis = context.openFileInput("forecast_data.json")
            val isr = InputStreamReader(fis)
            val bufferedReader = BufferedReader(isr)
            val stringBuilder = StringBuilder()
            var text: String?
            while (bufferedReader.readLine().also { text = it } != null) {
                stringBuilder.append(text)
            }
            fis.close()
            val fullData = JSONObject(stringBuilder.toString())
            val forecastData = fullData.getString("forecastData")
            val lastUpdateTime = fullData.getLong("timestamp")
            return Pair(Gson().fromJson(forecastData, Forecast::class.java), lastUpdateTime)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(null, 0)
    }

    private fun saveWeatherDataToFile(weatherData: String, cityName: String) {
        try {
            val currentTimestamp = System.currentTimeMillis()
            val dataToSave = JSONObject().apply {
                put("timestamp", currentTimestamp)
                put("weatherData", weatherData)
                put("cityName", cityName)
            }
            val fos = context.openFileOutput("weather_data.json", Context.MODE_PRIVATE)
            fos.write(dataToSave.toString().toByteArray())
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun savePollutionDataToFile(pollutionData: String) {
        try {
            val currentTimestamp = System.currentTimeMillis()
            val dataToSave = JSONObject().apply {
                put("timestamp", currentTimestamp)
                put("pollutionData", pollutionData)
            }
            val fos = context.openFileOutput("pollution_data.json", Context.MODE_PRIVATE)
            fos.write(dataToSave.toString().toByteArray())
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun saveForecastDataToFile(forecastData: String) {
        try {
            val currentTimestamp = System.currentTimeMillis()
            val dataToSave = JSONObject().apply {
                put("timestamp", currentTimestamp)
                put("forecastData", forecastData)
            }
            val fos = context.openFileOutput("forecast_data.json", Context.MODE_PRIVATE)
            fos.write(dataToSave.toString().toByteArray())
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
