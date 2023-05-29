package com.example.liveness

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Outline
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.example.liveness.core.*
import com.example.liveness.core.tasks.*
import com.example.liveness.databinding.ActivityLivenessBinding
import com.example.liveness.databinding.ActivityLivenessDosBinding
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class LivenessActivity2 : AppCompatActivity() {

    private var documentNumber: String? = ""
    private var employeeCode: String? = ""
    private var employeeName: String? = ""
    private lateinit var binding: ActivityLivenessDosBinding
    private lateinit var cameraController: LifecycleCameraController
    private var imageFiles = arrayListOf<String>()
    private lateinit var circularProgressBar: CircularProgressBar



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLivenessDosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        circularProgressBar = binding.progressBar
// Recuperar los datos de SharedPreferences
        val sharedPref = this.getSharedPreferences("employeeData", Context.MODE_PRIVATE)
        employeeName = sharedPref.getString("employee_name", null)
        documentNumber = sharedPref.getString("document_number", null)
        employeeCode = sharedPref.getString("employee_code", null)

        Log.d("LivenessActivity2", "employeeName: $employeeName, documentNumber: $documentNumber, employeeCode: $employeeCode")

        binding.toolbar.setNavigationOnClickListener { finish() }
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(this, "Permission deny", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.launch(Manifest.permission.CAMERA)

        binding.cameraPreview.clipToOutline = true
        binding.cameraPreview.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.height / 2.0f)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun startCamera() {
        cameraController = LifecycleCameraController(this)
        cameraController.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        cameraController.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(this),
            FaceAnalyzer(buildLivenessDetector())
        )
        cameraController.bindToLifecycle(this)
        binding.cameraPreview.controller = cameraController
    }

    private fun buildLivenessDetector(): LivenessDetector {
        val listener = object : LivenessDetector.Listener {
            @SuppressLint("SetTextI18n")
            override fun onTaskStarted(task: DetectionTask) {
                when (task) {
                    is FacingDetectionTask ->
                        binding.guide.text = "Por favor, de frente a la cámara."
                    is LeftFacingDetectionTask ->
                        binding.guide.text = "Mueva  la cabeza hacia la derecha"
                    is RightFacingDetectionTask ->
                        binding.guide.text = "Mueva  la cabeza hacia la izquierda"
                    is SmileDetectionTask ->
                        binding.guide.text = "Por favor sonríe"
                }
            }

            override fun onTaskCompleted(task: DetectionTask, isLastTask: Boolean) {

                takePhoto(File(cacheDir, "${System.currentTimeMillis()}.jpg")) {
                    circularProgressBar.progress += 25f
                    imageFiles.add(it.absolutePath)
                    if (isLastTask) {
                        finishForResult()
                    }
                }
            }

            override fun onTaskFailed(task: DetectionTask, code: Int) {
                if (code == LivenessDetector.ERROR_MULTI_FACES) {
                    Toast.makeText(
                        this@LivenessActivity2,
                        "Asegúrese de que solo haya una cara en la pantalla.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                // Reinicia el progreso
                circularProgressBar.progress = 0f
            }
        }

        return LivenessDetector(
            FacingDetectionTask(),
            LeftFacingDetectionTask(),
            RightFacingDetectionTask(),
            SmileDetectionTask()
        ).also { it.setListener(listener) }
    }

    /*private fun finishForResult() {
        val result = ArrayList(imageFiles.takeLast(4))
        setResult(RESULT_OK, Intent().putStringArrayListExtra(ResultContract.RESULT_KEY, result))
        finish()
    }*/

    private fun finishForResult() {




        val checkIn = EmployeeCheckIn(
            timestamp = Date(),
            images = ArrayList(imageFiles.takeLast(4)),
            employeeCode = employeeCode!!,  // Código quemado por ahora
            place = "Oficina",        // Lugar quemado por ahora
            checkInStatus = "Entrada", // Estado quemado por ahora
            name = employeeName!!,            // Nombre quemado por ahora
            idNumber = documentNumber!!     // Número de identificación quemado por ahora
        )
        // Insert the check-in into the database.
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        ).build()
        CoroutineScope(Dispatchers.IO).launch {
            db.checkInDao().insert(checkIn)
        }
        setResult(RESULT_OK, Intent().putExtra(ResultContract.RESULT_KEY, checkIn))
        finish()
    }


    private fun takePhoto(file: File, onSaved: (File) -> Unit) {
        cameraController.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(e: ImageCaptureException) {
                    e.printStackTrace()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onSaved(file)
                }
            }
        )
    }

    class ResultContract : ActivityResultContract<Any?, EmployeeCheckIn?>() {

        companion object {
            const val RESULT_KEY = "check_in"
        }

        override fun createIntent(context: Context, input: Any?): Intent {
            return Intent(context, LivenessActivity2::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): EmployeeCheckIn? {
            if (resultCode == RESULT_OK && intent != null) {
                return intent.getSerializableExtra(RESULT_KEY) as EmployeeCheckIn?
            }
            return null
        }
    }
}