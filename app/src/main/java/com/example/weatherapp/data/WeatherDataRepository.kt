package com.example.weatherapp.data

import android.content.Context
import android.util.Log
import com.example.weatherapp.data.forecastModels.Forecast
import com.example.weatherapp.data.forecastModels.ForecastData
import com.example.weatherapp.data.models.CurrentWeather
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
                val response = RetrofitInstance.api.getForecast(lat, lon, apiKey)
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

    suspend fun getWeatherData(city: String, apiKey: String): CurrentWeather? {
        val (currentWeatherFromFile, lastUpdateTime) = readWeatherDataFromFile()
        // Sprawdzenie, czy dane z pliku dotyczą szukanego miasta i czy są jeszcze aktualne
        val currentTime = System.currentTimeMillis() / 1000 // Konwersja na sekundy
        val isDataValid = currentWeatherFromFile != null &&
                currentTime - lastUpdateTime < updateIntervalMillis / 1000 &&
                currentWeatherFromFile.name.equals(city, ignoreCase = true)
        return if (isDataValid) {
            currentWeatherFromFile
        } else {
            val currentWeatherFromApi = fetchWeatherDataFromApi(city, apiKey)
            currentWeatherFromApi?.let {
                val weatherDataJson = Gson().toJson(it)
                saveWeatherDataToFile(weatherDataJson)
            }
            currentWeatherFromApi
        }
    }

    suspend fun getPollutionData(lat: Double, lon: Double, apiKey: String): PollutionData? {
        val (pollutionDataFromFile, lastUpdateTime) = readPollutionDataFromFile()

        val currentTime = System.currentTimeMillis() / 1000
        val isDataValid = pollutionDataFromFile != null &&
                currentTime - lastUpdateTime < updateIntervalMillis / 1000 &&
                pollutionDataFromFile.coord.lat == lat && pollutionDataFromFile.coord.lon == lon

        return if (isDataValid) {
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

    suspend fun getForecastData(lat: Double, lon: Double, apiKey: String): Forecast? {
        val (forecastDataFromFile, lastUpdateTime) = readForecastDataFromFile()

        val currentTime = System.currentTimeMillis() / 1000
        val isDataValid = forecastDataFromFile != null &&
                currentTime - lastUpdateTime < updateIntervalMillis / 1000 &&
                forecastDataFromFile.city.coord.lat == lat && forecastDataFromFile.city.coord.lon == lon

        return if (isDataValid) {
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

    private fun readWeatherDataFromFile(): Pair<CurrentWeather?, Long> {
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
            val lastUpdateTime = JSONObject(weatherData).getLong("dt")
            return Pair(Gson().fromJson(weatherData, CurrentWeather::class.java), lastUpdateTime)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(null, 0)
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
            val lastUpdateTime = JSONObject(pollutionData).getLong("dt")
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
            val lastUpdateTime = JSONObject(forecastData).getLong("dt")
            return Pair(Gson().fromJson(forecastData, Forecast::class.java), lastUpdateTime)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(null, 0)
    }

    private fun saveWeatherDataToFile(weatherData: String) {
        try {
            val currentTimestamp = System.currentTimeMillis()
            val dataToSave = JSONObject().apply {
                put("timestamp", currentTimestamp)
                put("weatherData", weatherData)
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

    fun getLastSearchedCityOrDefault(): String {
        val sharedPreferences = context.getSharedPreferences("WeatherApp", Context.MODE_PRIVATE)
        return sharedPreferences.getString("lastSearchedCity", "Warsaw") ?: "Warsaw"
    }
}
