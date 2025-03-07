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

    companion object Factory {
        private const val BASE_URL = "http://192.168.0.28:5000" // URL de la API

        fun create(): ApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(ApiService::class.java)
        }
    }
}
