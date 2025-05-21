package com.example.taller3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.taller3.Auth.LoginActivity
import com.example.taller3.Notifs.ServicioNotif
import com.example.taller3.databinding.ActivityMenuAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MenuAccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuAccountBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(this, "No hay usuario autenticado", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val uid = user.uid
        cargarDatosUsuario(uid)

        binding.btnCerrarSesion.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        binding.btnVerUsuariosDisponibles.setOnClickListener {
            startActivity(Intent(this, UsuariosDisponiblesActivity::class.java))
        }

        binding.btnDisponible.setOnClickListener {
            val nuevoEstado = !binding.btnDisponible.isSelected

            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            db.collection("usuarios")
                .document(uid)
                .update("disponible", nuevoEstado)
                .addOnSuccessListener {

                    binding.btnDisponible.isSelected = nuevoEstado
                    binding.btnDisponible.text =
                        if (nuevoEstado) "Disponible" else "No disponible"
                    Toast.makeText(this, if (nuevoEstado) "Ahora estás disponible" else "Ya no estás disponible", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al cambiar disponibilidad", Toast.LENGTH_SHORT).show()
                }
        }

        startService(Intent(this, ServicioNotif::class.java))
    }

    fun cargarDatosUsuario(uid: String) {
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nombre = document.getString("nombre") ?: ""
                    val apellido = document.getString("apellido") ?: ""
                    val correo = document.getString("email") ?: ""
                    val lat = document.getDouble("latitud") ?: 0.0
                    val lng = document.getDouble("longitud") ?: 0.0
                    val fotoUrl = document.getString("fotoPerfilUrl") ?: ""

                    binding.txtNombre.text = "$nombre $apellido"
                    binding.txtCorreo.text = correo
                    binding.txtUbicacion.text = "Ubicación: $lat, $lng"

                    ManejadorImagenes.mostrarImagenDesdeUrl(fotoUrl, binding.imgPerfil, this)
                } else {
                    Toast.makeText(
                        this,
                        "Documento de usuario no encontrado",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar datos del usuario", Toast.LENGTH_LONG)
                    .show()
                Log.e("MenuAccount", "Firestore error: ", it)
            }
    }
}
