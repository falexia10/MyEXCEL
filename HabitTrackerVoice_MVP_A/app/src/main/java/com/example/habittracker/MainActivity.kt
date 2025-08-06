
package com.example.habittracker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.habittracker.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var folderUri: Uri? = null
    private val db by lazy { AppDatabase.get(this).registroDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        promptBiometric()

        binding.btnPickFolder.setOnClickListener { pickFolder() }
        binding.btnDictate.setOnClickListener { startDictation() }
        binding.btnAdd.setOnClickListener { addEntry() }
        binding.btnExport.setOnClickListener { exportExcel() }

        checkAudioPermission()
        // Default date today DD/MM/YYYY
        val cal = Calendar.getInstance()
        val d = "%02d/%02d/%04d".format(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH)+1, cal.get(Calendar.YEAR))
        binding.etDate.setText(d)
    }

    private fun promptBiometric() {
        val biometricManager = BiometricManager.from(this)
        val can = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (can == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor: Executor = ContextCompat.getMainExecutor(this)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Desbloquear")
                .setSubtitle("Usa tu huella o cara")
                .setNegativeButtonText("Cancelar")
                .build()
            val biometricPrompt = BiometricPrompt(this, executor,
                object : BiometricPrompt.AuthenticationCallback() {})
            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun pickFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        folderPicker.launch(intent)
    }

    private val folderPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            res.data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                folderUri = uri
                findViewById<TextView>(R.id.tvStatus).text = "Carpeta seleccionada"
            }
        }
    }

    private fun startDictation() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora")
        }
        try {
            startActivityForResult(intent, 1001)
        } catch (e: Exception) {
            Toast.makeText(this, "Reconocimiento no disponible", Toast.LENGTH_SHORT).show()
        }
    }

    
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
        val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val spoken = results?.firstOrNull() ?: return
        // Por ahora, volcamos el texto reconocido en "Entrante" para que puedas editarlo rápidamente.
        binding.etEntrante.setText(spoken)
        binding.tvStatus.text = "Dictado capturado, revisa/edita y pulsa Añadir"
    }
}
}
(s: String) = s.length

    private fun addEntry() {
        val fecha = binding.etDate.text.toString().trim()
        val entrante = binding.etEntrante.text.toString().trim().ifEmpty { null }
        val principal = binding.etPrincipal.text.toString().trim().ifEmpty { null }
        val postre = binding.etPostre.text.toString().trim().ifEmpty { null }
        val esfuerzo = binding.etEffort.text.toString().toIntOrNull()
        val horas = binding.etSleep.text.toString().toDoubleOrNull()
        val lectura = binding.etReading.text.toString().toIntOrNull()

        lifecycleScope.launch {
            db.insert(Registro(
                fecha = fecha,
                entrante = entrante,
                principal = principal,
                postre = postre,
                esfuerzo = esfuerzo,
                horas = horas,
                lecturaMin = lectura
            ))
            binding.tvStatus.text = "Registro añadido"
            clearInputsExceptDate()
        }
    }

    private fun clearInputsExceptDate() {
        binding.etEntrante.setText("")
        binding.etPrincipal.setText("")
        binding.etPostre.setText("")
        binding.etEffort.setText("")
        binding.etSleep.setText("")
        binding.etReading.setText("")
    }

    private fun exportExcel() {
        val uri = folderUri
        if (uri == null) {
            Toast.makeText(this, "Primero elige la carpeta", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val registros = db.getAll()
            val ok = ExcelExporter(this@MainActivity).exportToFolder(uri, registros)
            binding.tvStatus.text = if (ok) "Excel exportado" else "Error al exportar"
        }
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 2001)
        }
    }
}
