package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.io.ApiService


class Home : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var editTextPassword: EditText

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_bueno)

        // Inicialización de ApiService
        apiService = ApiService.create()

        // Referenciar los botones
        val buttonPerfil: ImageButton = findViewById(R.id.profileImageButton)
        val buttonHome:  ImageButton = findViewById(R.id.nav_home)
        val buttonSearch:  ImageButton = findViewById(R.id.nav_search)
        val buttonCrear:  ImageButton = findViewById(R.id.nav_create)

        buttonPerfil.setOnClickListener {
            startActivity(Intent(this, Perfil::class.java))
        }

        buttonHome.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }

        buttonSearch.setOnClickListener {
            startActivity(Intent(this, Buscador::class.java))
        }

        buttonCrear.setOnClickListener {
            startActivity(Intent(this, Perfil::class.java))
        }
    }
}
