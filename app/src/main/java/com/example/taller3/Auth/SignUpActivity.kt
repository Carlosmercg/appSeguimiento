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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.taller3.Models.Usuario
import com.example.taller3.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var uriImagenPerfil: Uri? = null
    private var archivoImagen: File? = null

    private val permisoCamara = Manifest.permission.CAMERA
    private val permisoGaleria =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        configurarBotones()
    }

    private fun configurarBotones() {
        val cargarGaleria = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                uriImagenPerfil = guardarImagenLocal(it)
                mostrarImagen(uriImagenPerfil!!)
                mostrarToast("Imagen seleccionada desde galería")
            }
        }

        val tomarFoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { fueExitosa ->
            if (fueExitosa && archivoImagen?.exists() == true) {
                uriImagenPerfil = FileProvider.getUriForFile(this, "$packageName.fileprovider", archivoImagen!!)
                mostrarImagen(uriImagenPerfil!!)
                mostrarToast("Foto tomada correctamente")
            } else {
                mostrarToast("Error al tomar la foto")
            }
        }

        binding.btnUploadGaleria.setOnClickListener {
            if (tienePermiso(permisoGaleria)) {
                cargarGaleria.launch("image/*")
            } else {
                solicitarPermiso(permisoGaleria, "galería")
            }
        }

        binding.btnUploadDocCamara.setOnClickListener {
            if (tienePermiso(permisoCamara)) {
                archivoImagen = File(getExternalFilesDir(null), "perfil_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", archivoImagen!!)
                tomarFoto.launch(uri)
            } else {
                solicitarPermiso(permisoCamara, "cámara")
            }
        }

        binding.btnSignUp.setOnClickListener {
            val usuario = recolectarDatos() ?: return@setOnClickListener
            if (uriImagenPerfil == null) {
                mostrarToast("Debes subir una imagen de tu carné")
                return@setOnClickListener
            }

            mostrarToast("Registrando usuario...")
            auth.createUserWithEmailAndPassword(usuario.email, usuario.password)
                .addOnSuccessListener {
                    val uid = auth.currentUser!!.uid
                    val usuarioFinal = Usuario(
                        nombre = usuario.nombre,
                        apellido = usuario.apellido,
                        email = usuario.email,
                        password = usuario.password,
                        id = uid,
                        fotoPerfilUrl = uriImagenPerfil.toString()
                    )

                    db.collection("usuarios").document(uid).set(usuarioFinal)
                        .addOnSuccessListener {
                            mostrarToast("🎉 Cuenta creada exitosamente")
                            finish()
                        }
                        .addOnFailureListener {
                            mostrarToast("Error guardando en Firestore: ${it.message}")
                        }
                }
                .addOnFailureListener {
                    mostrarToast("Error creando cuenta: ${it.message}")
                }
        }
    }

    private fun tienePermiso(permiso: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permiso) == PackageManager.PERMISSION_GRANTED
    }

    private fun solicitarPermiso(permiso: String, origen: String) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permiso)) {
            mostrarToast("Permiso requerido para acceder a la $origen")
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permiso), if (origen == "cámara") 100 else 200)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (results.isNotEmpty() && results[0] != PackageManager.PERMISSION_GRANTED) {
            mostrarToast("Permiso denegado. No se puede crear la cuenta sin acceso a ${if (requestCode == 100) "la cámara" else "la galería"}.")
        }
    }

    private fun recolectarDatos(): Usuario? {
        val nombre = binding.etNombre.text.toString().trim()
        val apellido = binding.etApellido.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmar = binding.etConfirmPassword.text.toString().trim()

        return when {
            nombre.split(" ").size > 2 -> {
                mostrarToast("Máximo 2 nombres"); null
            }
            apellido.split(" ").size > 2 -> {
                mostrarToast("Máximo 2 apellidos"); null
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                mostrarToast("Correo inválido"); null
            }
            password.length < 6 || password != confirmar -> {
                mostrarToast("Contraseña inválida o no coinciden"); null
            }
            else -> Usuario(nombre, apellido, email, password)
        }
    }

    private fun guardarImagenLocal(uri: Uri): Uri? {
        return try {
            val input = contentResolver.openInputStream(uri)
            val file = File(getExternalFilesDir(null), "perfil_${System.currentTimeMillis()}.jpg")
            val output = FileOutputStream(file)
            input?.copyTo(output)
            input?.close()
            output.close()
            Uri.fromFile(file)
        } catch (e: Exception) {
            mostrarToast("Error al guardar imagen local")
            null
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
