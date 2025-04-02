package com.example.myapplication.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.myapplication.utils.Preferencias

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val splashScreen = installSplashScreen()

        // Configurar condición de mantener en pantalla
        splashScreen.setKeepOnScreenCondition { true }

        // Postergar la transición a la actividad de inicio de sesión después de 1500 milisegundos (1.5 segundos)
        Handler(Looper.getMainLooper()).postDelayed({
            val token = Preferencias.obtenerValorString("token", "")

            val nextActivity = if (token != null) {
                Home::class.java
            } else {
                Inicio::class.java
            }
            //val nextActivity =  Inicio::class.java

            val intent = Intent(this, nextActivity)
            startActivity(intent)
            finish()
        }, 1500)
    }
}
