package com.example.myapplication.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R

class ElegirRegistro : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.elegir_registro)

        // Botones
        val btnOyente = findViewById<Button>(R.id.btnOyente)
        val btnArtista = findViewById<Button>(R.id.btnArtista)

        btnOyente.setOnClickListener {
            // Ir a registro como usuario oyente
            startActivity(Intent(this, RegisterUser::class.java))
        }

        btnArtista.setOnClickListener {
            // Ir a registro como usuario artista
            startActivity(Intent(this, RegisterArtist::class.java))
        }

    }
}