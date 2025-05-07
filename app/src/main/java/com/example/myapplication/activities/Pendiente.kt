package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R

class Pendiente : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.solicitud_pendiente)

        // Botones
        val eliminar = findViewById<Button>(R.id.btn_delete)
        val logout = findViewById<Button>(R.id.btn_close)

        eliminar.setOnClickListener {
            // Ir a pantalla de login normal
            startActivity(Intent(this, Inicio::class.java))
        }

        logout.setOnClickListener {
            // Ir a pantalla de  elegir tipo de registro
            startActivity(Intent(this, Logout::class.java))
        }

    }
}
