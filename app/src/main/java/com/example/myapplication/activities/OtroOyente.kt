package com.example.myapplication.activities

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R

class OtroOyente : AppCompatActivity() {

    private lateinit var btnFollow: Button
    private lateinit var usernameText: TextView
    private lateinit var lastMessageText: TextView
    private lateinit var profileImage: ImageView

    private var isFollowing = false // Estado inicial del botón, si ya sigue o no.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.perfil_otro_oyente)

        // Inicialización de vistas
        btnFollow = findViewById(R.id.btnFollow)
        usernameText = findViewById(R.id.username)
        lastMessageText = findViewById(R.id.lastMessage)
        profileImage = findViewById(R.id.profileImage)

        // Aquí puedes cargar los datos dinámicamente, por ejemplo:
        usernameText.text = "Nombre del Oyente"
        lastMessageText.text = "Este es su último mensaje..." 

        // Configurar el estado inicial del botón
        updateFollowButtonState()

        // Evento de clic para el botón de seguir
        btnFollow.setOnClickListener {
            // Cambiar el estado de seguir/no seguir
            isFollowing = !isFollowing
            updateFollowButtonState()

            // Aquí puedes añadir la lógica para realizar una acción, como seguir al oyente en la base de datos
            if (isFollowing) {
                // Lógica para seguir al oyente
                lastMessageText.visibility = View.VISIBLE // Mostrar el último mensaje
            } else {
                // Lógica para dejar de seguir al oyente
                lastMessageText.visibility = View.GONE // Ocultar el último mensaje
            }
        }
    }

    // Método para actualizar el estado del botón de seguir
    private fun updateFollowButtonState() {
        if (isFollowing) {
            btnFollow.text = "Dejar de seguir"
        } else {
            btnFollow.text = "Seguir"
        }
    }
}