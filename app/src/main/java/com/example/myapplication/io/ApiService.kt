package com.example.myapplication.io
import retrofit2.Call
import com.example.myapplication.io.response.LoginResponse
import com.example.myapplication.io.response.RegisterResponse
import com.example.myapplication.io.request.LoginRequest
import com.example.myapplication.io.request.RegisterRequest


import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.Body

interface ApiService {
    @POST("login") // Petición a la ruta del login
    fun postlogin(@Body request: LoginRequest): Call<LoginResponse>
    @POST("register") // Petición a la ruta del registro
    fun postRegister(@Body request: RegisterRequest): Call<RegisterResponse>


    companion object Factory{
        private const val BASE_URL = "https://backend-eg2q.onrender.com/api/" //URL de la API
        fun create(): ApiService{
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(ApiService::class.java)
        }
    }
}