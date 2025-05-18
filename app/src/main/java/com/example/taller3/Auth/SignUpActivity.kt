package com.example.taller3.Auth

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.taller3.Models.Usuario
import com.example.taller3.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var FotoPerfil: Uri? = null
    private var archivoCamara: File? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        configurarPermisos()
        configurarBotones()
    }

    private fun configurarPermisos() {
        val permisos = mutableListOf(Manifest.permission.CAMERA)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permisos.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permisos.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        permisos.forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(it), 0)
            }
        }
    }


    private fun configurarBotones() {
        val cargarGaleria = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                FotoPerfil = it
                mostrarImagen(it)
                mostrarToast("Imagen seleccionada desde galería")
            }
        }

        val tomarFoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { fueExitosa ->
            if (fueExitosa && archivoCamara?.exists() == true) {
                FotoPerfil = FileProvider.getUriForFile(this, "$packageName.fileprovider", archivoCamara!!)
                mostrarImagen(FotoPerfil!!)
                mostrarToast("Foto tomada correctamente")
            } else {
                mostrarToast("Error al tomar la foto")
            }
        }

        binding.btnUploadGaleria.setOnClickListener {
            cargarGaleria.launch("image/*")
        }

        binding.btnUploadDocCamara.setOnClickListener {
            archivoCamara = File(getExternalFilesDir(null), "carnet_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", archivoCamara!!)
            tomarFoto.launch(uri)
        }

        binding.btnSignUp.setOnClickListener {
            val usuario = recolectarDatos() ?: return@setOnClickListener
            if (FotoPerfil == null) {
                mostrarToast("Debes subir una imagen de tu carné")
                return@setOnClickListener
            }

            mostrarToast("Registrando usuario...")
            crearUsuarioEnAuth(usuario, FotoPerfil!!)
        }
    }

    private fun recolectarDatos(): Usuario? {
        val nombre = binding.etNombre.text.toString().trim()
        val apellido = binding.etApellido.text.toString().trim()
        val correo = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmar = binding.etConfirmPassword.text.toString().trim()

        return when {
            nombre.isEmpty() || apellido.isEmpty() -> {
                mostrarToast("Completa todos los campos"); null
            }
            !Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> {
                mostrarToast("Correo inválido"); null
            }
            password.length < 6 || password != confirmar -> {
                mostrarToast("Contraseña inválida o no coinciden"); null
            }
            else -> Usuario(nombre, apellido, correo, password)
        }
    }

    private fun crearUsuarioEnAuth(usuario: Usuario, uri: Uri) {
        auth.createUserWithEmailAndPassword(usuario.correo, usuario.password)
            .addOnFailureListener {
                mostrarToast("Error creando usuario: ${it.message}")
            }
    }

    private fun registrarEnFirestore(usuario: Usuario) {
        db.collection("usuarios").document(usuario.id).set(usuario)
            .addOnSuccessListener {
                mostrarToast("🎉 Cuenta registrada correctamente")
                finish()
            }
            .addOnFailureListener {
                mostrarToast("Error al guardar usuario: ${it.message}")
            }
    }

    private fun mostrarImagen(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use {
                val bitmap = BitmapFactory.decodeStream(it)
                binding.imgPreview.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            mostrarToast("Error al mostrar la imagen")
        }
    }

    private fun mostrarToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        Log.d("SignUpActivity", msg)
    }
}
