package com.example.myapplication.io

import com.example.myapplication.io.response.LoginResponse
import com.example.myapplication.io.request.LoginRequest
import com.example.myapplication.io.request.RegisterArtistRequest
import com.example.myapplication.io.request.RegisterUserRequest
import com.example.myapplication.io.request.VerifyArtistRequest
import com.example.myapplication.io.response.CambiarPass1Response
import com.example.myapplication.io.response.CambiarPass2Response
import com.example.myapplication.io.response.CambiarPass3Response
import com.example.myapplication.io.response.RegisterArtistResponse
import com.example.myapplication.io.response.RegisterUserResponse
import com.example.myapplication.io.response.VerifyArtistResponse
import com.example.myapplication.io.request.CambiarPass1Request
import com.example.myapplication.io.request.CambiarPass2Request
import com.example.myapplication.io.request.CambiarPass3Request
import com.example.myapplication.io.request.DeleteAccountRequest
import com.example.myapplication.io.response.BuscadorResponse
import com.example.myapplication.io.response.DeleteAccountResponse
import com.example.myapplication.io.response.LogOutResponse
import retrofit2.Retrofit
import retrofit2.Call
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @POST("login") // Petición a la ruta del login
    fun postlogin(@Body request: LoginRequest): Call<LoginResponse>

    @POST("register-oyente") // Petición a la ruta del registro
    fun postRegisterOyente(@Body request: RegisterUserRequest): Call<RegisterUserResponse>
    @POST("/register-artista")
    fun postRegisterArtista(@Body requestBody: RegisterArtistRequest): Call<RegisterArtistResponse>

    // Verificar el código de validación del artista
    @POST("/verify-artista")
    fun verifyArtista(
        @Header("Authorization") authHeader: String,
        @Body request: VerifyArtistRequest
    ): Call<VerifyArtistResponse>

    @POST("/forgot-password")
    fun CambiarPass1(@Body requestBody: CambiarPass1Request): Call<CambiarPass1Response>
    @POST("/verify-codigo")
    fun CambiarPass2(@Body requestBody: CambiarPass2Request): Call<CambiarPass2Response>
    @POST("/reset-password")
    fun CambiarPass3(
        @Header("Authorization") authHeader: String,
        @Body request: CambiarPass3Request
    ): Call<CambiarPass3Response>
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
