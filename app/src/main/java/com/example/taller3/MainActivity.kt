package com.example.taller3

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.taller3.Auth.LoginActivity
import com.example.taller3.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ocultar la barra de acción para una experiencia completa
        supportActionBar?.hide()

        // Pasar a la pantalla de login después de 10 segundos
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Finaliza MainActivity para que no vuelva atrás
        }, 2000) // 5 segundos
    }
}