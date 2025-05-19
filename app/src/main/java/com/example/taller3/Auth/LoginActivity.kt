package com.example.taller3.Auth

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.example.taller3.MenuAccountActivity
import com.example.taller3.Services.ServicioDisp
import com.example.taller3.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val permisoNotifsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido: Boolean ->
        if (concedido) {
            iniciarServicioDisp()
        } else {
            mostrarDialogoJustificacion()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        binding.btnLogin.isEnabled = false

        // Validación en tiempo real
        binding.emailEditText.addTextChangedListener { validateFields() }
        binding.passwordEditText.addTextChangedListener { validateFields() }

        // Login al presionar Enter
        binding.passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && binding.btnLogin.isEnabled) {
                binding.btnLogin.performClick()
                true
            } else false
        }

        // Iniciar sesión
        binding.btnLogin.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val uid = auth.currentUser?.uid
                    if (uid == null) {
                        Toast.makeText(this, "No se pudo obtener el UID del usuario", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    Log.d("LoginActivity", "UID autenticado: $uid")

                    db.collection("usuarios").document(uid).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MenuAccountActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                            } else {
                                Toast.makeText(this, "Usuario autenticado pero no encontrado en Firestore", Toast.LENGTH_LONG).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al verificar datos del usuario: ${it.message}", Toast.LENGTH_LONG).show()
                            Log.e("LoginActivity", "Firestore error: ", it)
                        }
                }
                .addOnFailureListener { ex ->
                    when (ex) {
                        is FirebaseAuthInvalidUserException -> {
                            Toast.makeText(this, "Correo no registrado", Toast.LENGTH_LONG).show()
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(this, "Error: ${ex.localizedMessage}", Toast.LENGTH_LONG).show()
                            Log.e("LoginActivity", "Firebase Auth error: ", ex)
                        }
                    }
                }
        }

        // Ir al registro
        binding.btnSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        Intent(this, ServicioDisp::class.java).also { startService(it) }
    }

    // Valida correo y contraseña
    private fun validateFields() {
        val emailValid = binding.emailEditText.text.toString().trim().let {
            it.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(it).matches()
        }
        val passwordValid = binding.passwordEditText.text.toString().trim().isNotEmpty()
        binding.btnLogin.isEnabled = emailValid && passwordValid
    }

    private fun mostrarDialogoJustificacion() {
        AlertDialog.Builder(this)
            .setTitle("Permiso requerido")
            .setMessage(
                "Las notificaciones deben ser activadas " +
                        "para observar el cambio de la lista de usuarios."
            )
            .setPositiveButton("Aceptar") { _, _ ->
                permisoNotifsLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            .setCancelable(false)
            .show()
    }
    private fun solicitarPermisoNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val estado = ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (estado == PackageManager.PERMISSION_GRANTED) {
                // Ya tenía permiso
                iniciarServicioDisp()
            } else {
                // No hay permiso: lánzalo
                permisoNotifsLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // En versiones anteriores no hace falta pedirlo
            iniciarServicioDisp()
        }
    }
    private fun iniciarServicioDisp() {
        val intentServicio = Intent(this, ServicioDisp::class.java)
        startService(intentServicio)
    }

}
