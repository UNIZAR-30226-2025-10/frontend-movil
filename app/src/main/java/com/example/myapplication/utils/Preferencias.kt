package com.example.myapplication.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.MiApp


object Preferencias {
    private const val PREFS_NAME = "mis_preferencias"
    private val sharedPreferences: SharedPreferences by lazy {
        MiApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    fun guardarValorString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }
    fun obtenerValorString(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    fun guardarValorSetString(key: String, value: Set<String>) {
        sharedPreferences.edit().putStringSet(key, value).apply()
    }

    fun obtenerValorSetString(key: String, defaultValue: Set<String>): Set<String> {
        return sharedPreferences.getStringSet(key, defaultValue) ?: defaultValue
    }

    fun guardarValorBooleano(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }
    fun obtenerValorBooleano(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun guardarValorEntero(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }
    fun obtenerValorEntero(key: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }
    fun borrarDatosUsuario(){
        sharedPreferences.edit().clear().apply()
    }
}

object TemaPreferencias {
    private const val PREF_NAME = "tema_prefs"
    private const val KEY_MODO_OSCURO = "modo_oscuro"

    fun guardarModoOscuro(context: Context, esOscuro: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_MODO_OSCURO, esOscuro).apply()
    }

    fun obtenerModoOscuro(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_MODO_OSCURO, false)
    }
}
