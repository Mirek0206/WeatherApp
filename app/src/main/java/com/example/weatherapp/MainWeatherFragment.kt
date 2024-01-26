import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.weatherapp.R
import com.example.weatherapp.data.models.CurrentWeather
import com.example.weatherapp.databinding.FragmentMainWeatherBinding
import com.example.weatherapp.utils.RetrofitInstance
import com.google.gson.Gson
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class MainWeatherFragment : Fragment() {
    private var _binding: FragmentMainWeatherBinding? = null
    private val binding get() = _binding!!
    private val updateIntervalMillis: Long = 3600000 // 1 hour in ms
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lastSearchedCity = getLastSearchedCity()
        if (isNetworkAvailable()) {
            getCurrentWeather(lastSearchedCity)
        } else {
            readWeatherDataFromFile()
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    getCurrentWeather(it)
                    saveLastSearchedCity(it)
                }
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        startPeriodicUpdate(lastSearchedCity)
    }

    private fun startPeriodicUpdate(city: String) {
        coroutineScope.launch {
            while (isActive) {
                delay(updateIntervalMillis)
                getCurrentWeather(city)
            }
        }
    }

    private fun getCurrentWeather(city: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val apiKey = getString(R.string.api_key)
                val response = RetrofitInstance.api.getCurrentWeather(city, "metric", apiKey)
                if (response.isSuccessful && response.body() != null) {
                    val weatherData = Gson().toJson(response.body())
                    saveWeatherDataToFile(weatherData)
                    withContext(Dispatchers.Main) {
                        updateUI(response.body()!!)
                    }
                } else {
                    showError("Error: ${response.errorBody()?.string()}")
                }
            } catch (e: IOException) {
                showError("No internet connection")
            } catch (e: Exception) {
                showError("Error occurred: ${e.message}")
            }
        }
    }

    private fun saveWeatherDataToFile(weatherData: String) {
        try {
            val currentTimestamp = System.currentTimeMillis()
            val dataToSave = JSONObject().apply {
                put("timestamp", currentTimestamp)
                put("weatherData", weatherData)
            }
            val fos = requireContext().openFileOutput("weather_data.json", Context.MODE_PRIVATE)
            fos.write(dataToSave.toString().toByteArray())
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun readWeatherDataFromFile() {
        try {
            val fis = requireContext().openFileInput("weather_data.json")
            val isr = InputStreamReader(fis)
            val bufferedReader = BufferedReader(isr)
            val stringBuilder = StringBuilder()
            var text: String?
            while (run {
                    text = bufferedReader.readLine()
                    text
                } != null) {
                stringBuilder.append(text)
            }
            fis.close()
            val fullData = JSONObject(stringBuilder.toString())
            val lastUpdateTime = fullData.getLong("timestamp")
            val weatherData = fullData.getString("weatherData")

            if (System.currentTimeMillis() - lastUpdateTime > updateIntervalMillis) {
                getCurrentWeather(getLastSearchedCity()) // Dane są stare, odśwież je
            } else {
                val weather = Gson().fromJson(weatherData, CurrentWeather::class.java)
                updateUI(weather) // Dane są aktualne, użyj ich
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(weather: CurrentWeather) {
        binding.tvLocation.text = weather.name
        binding.tvTemp.text = "${weather.main.temp}°C"
        binding.tvStatus.text = weather.weather.first().description
        binding.tvLatCoordTemp.text = "Lat: ${weather.coord.lat}"
        binding.tvLongCoordTemp.text = "Lon: ${weather.coord.lon}"

        val iconCode = weather.weather.first().icon
        val imageUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
        loadWeatherIcon(imageUrl)

        val currentTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        binding.tvUpdateTime.text = "Updated at: $currentTime"
    }

    private fun loadWeatherIcon(url: String) {
        Glide.with(this)
            .load(url)
            .into(binding.imgWeather)
    }

    private fun showError(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    private fun saveLastSearchedCity(city: String) {
        val sharedPreferences = activity?.getSharedPreferences("WeatherApp", Context.MODE_PRIVATE)
        sharedPreferences?.edit()?.putString("lastSearchedCity", city)?.apply()
    }

    private fun getLastSearchedCity(): String {
        val sharedPreferences = activity?.getSharedPreferences("WeatherApp", Context.MODE_PRIVATE)
        return sharedPreferences?.getString("lastSearchedCity", "Warsaw") ?: "Warsaw"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel()
        _binding = null
    }
}
