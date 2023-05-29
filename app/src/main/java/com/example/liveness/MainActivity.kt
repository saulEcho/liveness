package com.example.liveness

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
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
import java.io.File
import java.io.FileInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding



    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        lifecycle.currentState
        setContentView(binding.root)

        /*val livenessLauncher = registerForActivityResult(LivenessActivity2.ResultContract()) { checkIn->
            if (checkIn != null) {
                 Aqui se obtiene la imagen
                 checkIn.images.get(0) para obtener la imagen 0

                Log.d("forResult", "onCreate for result: ")
                binding.recyclerView.adapter = ImageAdapter(checkIn.images.orEmpty())

                checkIn.images.forEach { path ->
                    val sourceFile = File(path)

                    // Asegurarte de que la subcarpeta existe antes de intentar guardar el archivo en ella
                    val destDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "${checkIn.employeeCode}")
                    destDir.mkdirs()  // Si destDir ya existe, no se hace nada. Si no existe, se intenta crear.

                    val destFile = File(destDir, "${sourceFile.name}")
                    sourceFile.copyTo(destFile, true)
                }
            }

        }*/

        val livenessLauncher = registerForActivityResult(LivenessActivity2.ResultContract()) { checkIn->
            if (checkIn != null) {
                // Aquí se obtiene la imagen
                // checkIn.images.get(0) para obtener la imagen 0

                Log.d("forResult", "onCreate for result: ")
                binding.recyclerView.adapter = ImageAdapter(checkIn.images.orEmpty())

                checkIn.images.forEach { path ->
                    val sourceFile = File(path)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val resolver = contentResolver
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, "${checkIn.employeeCode}_${sourceFile.name}")
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/${checkIn.employeeCode}")
                        }

                        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        resolver.openOutputStream(imageUri!!).use { outputStream ->
                            FileInputStream(sourceFile).use { inputStream ->
                                inputStream.copyTo(outputStream!!)
                            }
                        }
                    } else {
                        // Para versiones anteriores a Android Q
                        val destDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), checkIn.employeeCode)
                        if (!destDir.exists()) {
                            destDir.mkdirs()
                        }
                        val destFile = File(destDir, sourceFile.name)
                        sourceFile.copyTo(destFile, true)
                    }
                }

            }
        }

        /*binding.startBtn.setOnClickListener {
            livenessLauncher.launch(null)
        }*/

        binding.startBtn.setOnClickListener {
            val alertDialogView = LayoutInflater.from(this).inflate(R.layout.dialog_employee_data, null)
            AlertDialog.Builder(this)
                .setView(alertDialogView)
                .setPositiveButton("Aceptar") { dialog, _ ->
                    val employeeName = alertDialogView.findViewById<EditText>(R.id.employee_name).text.toString()
                    val documentNumber = alertDialogView.findViewById<EditText>(R.id.document_number).text.toString()
                    val employeeCode = alertDialogView.findViewById<EditText>(R.id.employee_code).text.toString()

                    if (employeeName.isBlank() || documentNumber.isBlank() || employeeCode.isBlank()) {
                        Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }

                    val sharedPref = this.getSharedPreferences("employeeData", Context.MODE_PRIVATE)
                    with (sharedPref.edit()) {
                        putString("employee_name", employeeName)
                        putString("document_number", documentNumber)
                        putString("employee_code", employeeCode)
                        apply()
                    }

                    val testEmployeeName = sharedPref.getString("employee_name", null)
                    val testDocumentNumber = sharedPref.getString("document_number", null)
                    val testEmployeeCode = sharedPref.getString("employee_code", null)

                    Log.d("MainActivity", "testEmployeeName: $testEmployeeName, testDocumentNumber: $testDocumentNumber, testEmployeeCode: $testEmployeeCode")

                    livenessLauncher.launch(null)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.cancel()
                }
                .show()
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

            Log.d("sendAllCheckIns", "sendAllCheckIns: ${checkIns}")
            /*val response = checkInApi.sendBatchCheckIn(checkIns)
            if (response.isSuccessful) {
                checkIns.forEach { checkInDao.delete(it) }
            } else {
                // manejar error
            }*/
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
        Log.d("onBindViewHolder", "onBindViewHolder: ${image}")
        Glide.with(holder.itemView).load(image).into(holder.itemView as ImageView)
    }

    override fun getItemCount(): Int {
        return images.size
    }
}