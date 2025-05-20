package com.example.taller3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taller3.Mapas.LocationsActivity
import com.example.taller3.Models.Usuario
import com.example.taller3.adapters.UsuarioDisponibleAdapter
import com.example.taller3.databinding.ActivityUsuariosDisponiblesBinding
import com.google.firebase.firestore.FirebaseFirestore

class UsuariosDisponiblesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsuariosDisponiblesBinding
    private lateinit var adapter: UsuarioDisponibleAdapter
    private val usuarios = mutableListOf<Usuario>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsuariosDisponiblesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cargarUsuariosDisponibles()

        adapter = UsuarioDisponibleAdapter(usuarios) { usuario ->
            Intent(this, LocationsActivity::class.java).apply {
                putExtra("usuarioID", usuario.id)
            }.also(::startActivity)
        }

        binding.listaUsuariosDisponibles.layoutManager = LinearLayoutManager(this)
        binding.listaUsuariosDisponibles.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        cargarUsuariosDisponibles()
    }

    private fun cargarUsuariosDisponibles() {
        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .whereEqualTo("disponible", true)
            .get()
            .addOnSuccessListener { resultado ->
                usuarios.clear()
                for (documento in resultado) {
                    val usuario = documento.toObject(Usuario::class.java)
                    usuarios.add(usuario)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar usuarios disponibles", Toast.LENGTH_LONG).show()
                Log.e("UsuariosDisponibles", "Firestore error: ", it)
            }
    }
}