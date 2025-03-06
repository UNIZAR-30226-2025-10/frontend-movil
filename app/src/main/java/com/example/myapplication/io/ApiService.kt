package com.example.myapplication.io

import retrofit2.Call
import com.example.myapplication.io.response.LoginResponse
import com.example.myapplication.io.request.LoginRequest
import com.example.myapplication.io.request.RegisterUserRequest
import com.example.myapplication.io.response.RegisterUserResponse
import com.example.myapplication.io.request.DeleteRequest
import com.example.myapplication.io.response.DeleteResponse
import com.example.myapplication.io.request.LogOutRequest
import com.example.myapplication.io.response.LogOutResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.DELETE

interface ApiService {
    @POST("login") // Petición a la ruta del login
    fun postlogin(@Body request: LoginRequest): Call<LoginResponse>

    @POST("register-oyente") // Petición a la ruta del registro
    fun postRegisterOyente(@Body request: RegisterUserRequest): Call<RegisterUserResponse>

    // Nuevo método para hacer logout
    @POST("logout") // Ruta de logout
    fun postlogout(@Body request: LogOutRequest): Call<LogOutResponse>

    // Nuevo método para eliminar cuenta
    @DELETE("delete_account") // Ruta de eliminar cuenta
    fun deleteAccount(@Body request: DeleteRequest): Call<DeleteResponse>

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
