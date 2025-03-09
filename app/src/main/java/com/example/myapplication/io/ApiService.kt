package com.example.myapplication.io

import retrofit2.Call
import com.example.myapplication.io.response.LoginResponse
import com.example.myapplication.io.request.LoginRequest
import com.example.myapplication.io.request.RegisterUserRequest
import com.example.myapplication.io.response.RegisterUserResponse
import com.example.myapplication.io.request.DeleteAccountRequest
import com.example.myapplication.io.response.BuscadorResponse
import com.example.myapplication.io.response.DeleteAccountResponse
import com.example.myapplication.io.response.LogOutResponse
import com.example.myapplication.io.response.HistorialCancionesResponse
import com.example.myapplication.io.response.HistorialEscuchasResponse
import com.example.myapplication.io.response.PlaylistsResponse
import com.example.myapplication.io.response.RecomendacionesResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ApiService {
    @POST("login") // Petición a la ruta del login
    fun postlogin(@Body request: LoginRequest): Call<LoginResponse>

    @POST("register-oyente") // Petición a la ruta del registro
    fun postRegisterOyente(@Body request: RegisterUserRequest): Call<RegisterUserResponse>

    // Nuevo método para eliminar cuenta
    @POST("delete_account") // Ruta de eliminar cuenta
    fun deleteAccount(
        @Header("Authorization") authHeader: String,
        @Body request: DeleteAccountRequest
    ): Call<DeleteAccountResponse>

    // Nuevo método para cerrar sesión
    @POST("logout") // 
    fun logout(
        @Header("Authorization") authHeader: String
    ): Call<LogOutResponse>

    // Nuevo método para el buscador
    @GET("/search")
    fun searchBuscador(
        @Header("Authorization") token: String,
        @Query("termino") termino: String
    ): Call<BuscadorResponse>


    // Agregar los nuevos métodos para obtener datos del home
    @GET("/get-historial-canciones")
    fun getHistorialCanciones(@Header("Authorization") token: String): Call<HistorialCancionesResponse>

    @GET("/get-historial-colecciones")
    fun getHistorialEscuchas(@Header("Authorization") token: String): Call<HistorialEscuchasResponse>


    @GET("/get-mis-playlists")
    fun getMisPlaylists(@Header("Authorization") token: String): Call<PlaylistsResponse>

    @GET("/get-recomendaciones")
    fun getRecomendaciones(@Header("Authorization") token: String): Call<RecomendacionesResponse>



    companion object Factory {
        private const val BASE_URL = "https://api-noizz.onrender.com" // URL de la API

        fun create(): ApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(ApiService::class.java)
        }
    }
}
