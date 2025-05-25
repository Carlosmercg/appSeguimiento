package com.example.taller3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taller3.Mapas.DisponibleActivity
import com.example.taller3.Models.Usuario
import com.example.taller3.adapters.UsuarioDisponibleAdapter
import com.example.taller3.databinding.ActivityUsuariosDisponiblesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UsuariosDisponiblesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsuariosDisponiblesBinding
    private lateinit var adapter: UsuarioDisponibleAdapter
    private val usuarios = mutableListOf<Usuario>()
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsuariosDisponiblesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cargarUsuariosDisponibles()

        adapter = UsuarioDisponibleAdapter(usuarios) { usuario ->
            Intent(this, DisponibleActivity::class.java).apply {
                Log.d("UsuariosDisponibles", "Usuario seleccionado: $usuario")
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
                    if(documento.id != auth.currentUser?.uid) {
                        val usuario = documento.toObject(Usuario::class.java)
                        usuarios.add(usuario)
                    }
                    //Log.d("UsuariosDisponibles", "Usuario agregado: $usuario")
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar usuarios disponibles", Toast.LENGTH_LONG).show()
                Log.e("UsuariosDisponibles", "Firestore error: ", it)
            }
    }
}