package com.example.liveness

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.room.Room
import com.bumptech.glide.Glide
import com.example.liveness.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding



    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        lifecycle.currentState
        setContentView(binding.root)

        val livenessLauncher = registerForActivityResult(LivenessActivity.ResultContract()) { checkIn->
            if (checkIn != null) {
                // Aqui se obtiene la imagen
                // checkIn.images.get(0) para obtener la imagen 0

                binding.recyclerView.adapter = ImageAdapter(checkIn.images.orEmpty())
            }

        }

        binding.startBtn.setOnClickListener {
            livenessLauncher.launch(null)
        }

        binding.sendAllBtn.setOnClickListener {
            if (isNetworkAvailable(this)) {
                // Tiene conexión a Internet, puede enviar la información
                sendAllCheckIns()
            } else {
                // No tiene conexión a Internet, muestra un mensaje al usuario
                Toast.makeText(this, "No hay conexión a Internet", Toast.LENGTH_SHORT).show()
            }
        }

        binding.sendOneByOneBtn.setOnClickListener {
            if (isNetworkAvailable(this)) {
                // Tiene conexión a Internet, puede enviar la información
                sendCheckInOneByOne()
            } else {
                // No tiene conexión a Internet, muestra un mensaje al usuario
                Toast.makeText(this, "No hay conexión a Internet", Toast.LENGTH_SHORT).show()
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities != null &&
                (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }

    private val checkInApi: CheckInApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://your-api-url.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(CheckInApi::class.java)
    }

    private val checkInDao: CheckInDao by lazy {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        ).build()
        db.checkInDao()
    }

    private fun sendAllCheckIns() {
        GlobalScope.launch(Dispatchers.IO) {
            val checkIns = checkInDao.getAll()

            val response = checkInApi.sendBatchCheckIn(checkIns)
            if (response.isSuccessful) {
                checkIns.forEach { checkInDao.delete(it) }
            } else {
                // manejar error
            }
        }
    }

    private fun sendCheckInOneByOne() {
        GlobalScope.launch(Dispatchers.IO) {
            val checkIns = checkInDao.getAll()

            for (checkIn in checkIns) {
                val response = checkInApi.sendCheckIn(checkIn)
                if (response.isSuccessful) {
                    checkInDao.delete(checkIn)
                } else {
                    // manejar error
                }
            }
        }
    }
}



class ImageAdapter(private val images: List<String>) : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val imageView = ImageView(parent.context)
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return object : ViewHolder(imageView) {}
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val image = images[position]
        Glide.with(holder.itemView).load(image).into(holder.itemView as ImageView)
    }

    override fun getItemCount(): Int {
        return images.size
    }
}