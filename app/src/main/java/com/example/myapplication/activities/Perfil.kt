package com.example.myapplication.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R

class Perfil : AppCompatActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.perfil)

        // Botones
        val deleteAccountButton = findViewById<Button>(R.id.botonDeleteAccount)
        val logOutButton = findViewById<Button>(R.id.botonLogout)

        deleteAccountButton.setOnClickListener {
            //Borrar cuenta
            startActivity(Intent(this, DeleteAccount::class.java))
        }

        logOutButton.setOnClickListener {
            //Cerrar sesion
            startActivity(Intent(this, Logout::class.java))
        }

    }
}