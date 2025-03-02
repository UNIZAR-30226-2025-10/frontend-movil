package com.example.myapplication.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R

class Inicio : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.inicio)

        // Botones
        val signInButton = findViewById<Button>(R.id.signInButton)
        val logInButton = findViewById<Button>(R.id.logInButton)

        signInButton.setOnClickListener {
            // Ir a pantalla de login normal
            startActivity(Intent(this, Login::class.java))
        }

        logInButton.setOnClickListener {
            // Ir a pantalla de  elegir tipo de registro
            startActivity(Intent(this, ElegirRegistro::class.java))
        }

    }
}
