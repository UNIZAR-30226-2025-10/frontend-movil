package com.example.myapplication.io

import com.example.myapplication.io.request.ActualizarFavoritoRequest
import com.example.myapplication.io.request.AudioRequest
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
import com.example.myapplication.io.request.PlaylistRequest
import com.example.myapplication.io.request.EditarPerfilRequest
import com.example.myapplication.io.request.PlayPauseRequest
import com.example.myapplication.io.request.PlayPauseResponse
import com.example.myapplication.io.request.ValidarArtistaRequest
import com.example.myapplication.io.request.ValidarArtistaResponse
import com.example.myapplication.io.response.ActualizarFavoritoResponse
import com.example.myapplication.io.response.AddReproduccionResponse
import com.example.myapplication.io.response.AudioResponse
import com.example.myapplication.io.response.BuscadorResponse
import com.example.myapplication.io.response.DeleteAccountResponse
import com.example.myapplication.io.response.EditarPerfilResponse
import com.example.myapplication.io.response.GetSignatureResponse
import com.example.myapplication.io.response.HistorialArtistasResponse
import com.example.myapplication.io.response.HistorialRecientesResponse
import com.example.myapplication.io.response.LogOutResponse
import com.example.myapplication.io.response.HistorialEscuchasResponse
import com.example.myapplication.io.response.PlaylistsResponse
import com.example.myapplication.io.response.RecomendacionesResponse
import com.example.myapplication.io.response.InfoSeguidoresResponse
import com.example.myapplication.io.response.PendientesResponse
import com.example.myapplication.io.response.PlaylistResponse
import retrofit2.Retrofit
import retrofit2.Call
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PUT
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

    @HTTP(method = "DELETE", path = "/delete-account", hasBody = true)
    fun deleteAccount(
        @Header("Authorization") authHeader: String,
        @Body request: DeleteAccountRequest
    ): Call<Void>

    // Nuevo método para cerrar sesión
    @POST("logout")  // <- Asegúrate de que esta línea esté presente
    fun logout(@Header("Authorization") authHeader: String): Call<Void>


    // Nuevo método para el buscador
    @GET("/search")
    fun searchBuscador(
        @Header("Authorization") token: String,
        @Query("termino") termino: String
    ): Call<BuscadorResponse>

    @PUT("/put-cancion-sola")
    fun reproducirCancion(
        @Header("Authorization") token: String,
        @Header("sid") sid: String,
        @Body request: AudioRequest
    ): Call<AudioResponse>

    @GET("/get-datos-playlist")
    fun getDatosPlaylist(
        @Header("Authorization") token: String,
        @Query ("id") playlistId: String
    ): Call<PlaylistResponse>

    @PUT("/change-fav")
    fun actualizarFavorito(
        @Header("Authorization") token: String,
        @Body request: ActualizarFavoritoRequest
    ): Call<ActualizarFavoritoResponse>

    @PUT("/add-reproduccion")
    fun addReproduccion(
        @Header("Authorization") token: String,
    ): Call<AddReproduccionResponse>


    @PUT("/play-pause")
    fun playPause(
        @Header("Authorization") token: String,
        @Body request: PlayPauseRequest
    ): Call<PlayPauseResponse>

    @GET("/get-historial-colecciones")
    fun getHistorialRecientes(@Header("Authorization") token: String): Call<HistorialRecientesResponse>

    @GET("/get-historial-artistas")
    fun getHistorialArtistas(@Header("Authorization") token: String): Call<HistorialArtistasResponse>

    @GET("/get-historial-canciones")
    fun getHistorialEscuchas(@Header("Authorization") token: String): Call<HistorialEscuchasResponse>


    @GET("/get-mis-playlists")
    fun getMisPlaylists(@Header("Authorization") token: String): Call<PlaylistsResponse>

    @GET("/get-recomendaciones")
    fun getRecomendaciones(@Header("Authorization") token: String): Call<RecomendacionesResponse>

    @GET("/get-mis-datos-oyente")
    fun getMisDatosOyente(@Header("Authorization") token: String): Call<InfoSeguidoresResponse>

    @PUT("/change-datos-oyente")
    fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: EditarPerfilRequest
    ): Call<EditarPerfilResponse>

    @GET("/get-signature")
    fun getSignature(
        @Header("Authorization") token: String,
        @Query("folder") folder: String
    ): Call<GetSignatureResponse>

    @GET("/get_pendientes")
    fun getPendientes(
        @Header("Authorization") token: String
    ): Call<PendientesResponse>

    @POST("/check_artista")
    fun validarArtista(
        @Header("Authorization") token: String,
        @Body request: ValidarArtistaRequest
    ): Call<ValidarArtistaResponse>




    companion object Factory {
        private const val BASE_URL = "https://api-noizz.onrender.com" // URL de la API
        //private const val BASE_URL = "http://192.168.0.62:5000"
        //private const val BASE_URL = "http://10.1.65.120:5000"
        fun create(): ApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(ApiService::class.java)
        }
    }
}