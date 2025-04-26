package com.example.myapplication.io

import com.example.myapplication.io.request.AceptarInvitacionRequest
import com.example.myapplication.io.request.ActualizarFavoritoRequest
import com.example.myapplication.io.request.AddToPlaylistRequest
import com.example.myapplication.io.request.AudioColeccionRequest
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
import com.example.myapplication.io.request.CambiarPrivacidadPlaylistRequest
import com.example.myapplication.io.request.CancionInfoRequest
import com.example.myapplication.io.request.CrearAlbumRequest
import com.example.myapplication.io.request.CrearCancionRequest
import com.example.myapplication.io.request.ChangeFollowRequest
import com.example.myapplication.io.request.ChangePasswordRequest
import com.example.myapplication.io.request.CreatePlaylistRequest
import com.example.myapplication.io.request.DarLikeNoizzyRequest
import com.example.myapplication.io.request.DeleteAccountRequest
import com.example.myapplication.io.request.DeleteNotiAlbumRequest
import com.example.myapplication.io.request.DeleteNotiCancionRequest
import com.example.myapplication.io.request.EditarAlbumRequest
import com.example.myapplication.io.request.DeleteFromPlaylistRequest
import com.example.myapplication.io.request.DeletePlaylistRequest
import com.example.myapplication.io.request.PlaylistRequest
import com.example.myapplication.io.request.EditarPerfilRequest
import com.example.myapplication.io.request.ExpelUserRequest
import com.example.myapplication.io.request.InvitarPlaylistRequest
import com.example.myapplication.io.request.LeavePlaylistRequest
import com.example.myapplication.io.request.LeerNotiLikeRequest
import com.example.myapplication.io.request.LeerNotiNoizzitoRequest
import com.example.myapplication.io.request.LeerNotiSeguidorRequest
import com.example.myapplication.io.request.PlayPauseRequest
import com.example.myapplication.io.request.PlayPauseResponse
import com.example.myapplication.io.request.PostNoizzitoRequest
import com.example.myapplication.io.request.PostNoizzyRequest
import com.example.myapplication.io.request.ProgresoRequest
import com.example.myapplication.io.request.UpdatePlaylistRequest
import com.example.myapplication.io.request.ValidarArtistaRequest
import com.example.myapplication.io.request.ValidarArtistaResponse
import com.example.myapplication.io.request.VerInteraccionRequest
import com.example.myapplication.io.response.ActualizarFavoritoResponse
import com.example.myapplication.io.response.AddReproduccionResponse
import com.example.myapplication.io.response.AudioResponse
import com.example.myapplication.io.response.BuscadorResponse
import com.example.myapplication.io.response.CrearAlbumResponse
import com.example.myapplication.io.response.CancionActualResponse
import com.example.myapplication.io.response.CancionInfoResponse
import com.example.myapplication.io.response.CancionesArtistaResponse
import com.example.myapplication.io.response.CancionesFavsArtistaResponse
import com.example.myapplication.io.response.CrearPlaylistResponse
import com.example.myapplication.io.response.DatosAlbumResponse
import com.example.myapplication.io.response.DatosArtistaResponse
import com.example.myapplication.io.response.DeleteAccountResponse
import com.example.myapplication.io.response.DeleteAlbumResponse
import com.example.myapplication.io.response.DiscografiaAlbumArtistaResponse
import com.example.myapplication.io.response.EditarPerfilResponse

import com.example.myapplication.io.response.EstadisticasAlbumResponse
import com.example.myapplication.io.response.GetEstadisticasFavsResponse
import com.example.myapplication.io.response.GetEstadisticasPlaylistResponse
import com.example.myapplication.io.response.GetEtiquetasResponse

import com.example.myapplication.io.response.GetDatosOyenteResponse
import com.example.myapplication.io.response.GetInteraccionesResponse
import com.example.myapplication.io.response.GetInvitacionesResponse

import com.example.myapplication.io.response.GetMisAlbumesResponse
import com.example.myapplication.io.response.GetNovedadesResponse
import com.example.myapplication.io.response.GetNuevosSeguidoresResponse
import com.example.myapplication.io.response.GetPlaylistOyenteResponse
import com.example.myapplication.io.response.GetSignatureResponse
import com.example.myapplication.io.response.HayNotificacionesResponse
import com.example.myapplication.io.response.HistorialArtistasResponse
import com.example.myapplication.io.response.HistorialRecientesResponse
import com.example.myapplication.io.response.LogOutResponse
import com.example.myapplication.io.response.HistorialEscuchasResponse
import com.example.myapplication.io.response.InfoPerfilArtistaResponse
import com.example.myapplication.io.response.PlaylistsResponse
import com.example.myapplication.io.response.RecomendacionesResponse
import com.example.myapplication.io.response.InfoSeguidoresResponse
import com.example.myapplication.io.response.Interaccion
import com.example.myapplication.io.response.MisAlbumesResponse
import com.example.myapplication.io.response.MisNoizzysResponse
import com.example.myapplication.io.response.NumFavoritasArtistaResponse
import com.example.myapplication.io.response.PendientesResponse
import com.example.myapplication.io.response.PlaylistResponse
import com.example.myapplication.io.response.PopularesArtistaResponse
import com.example.myapplication.io.response.SearchPlaylistResponse
import com.example.myapplication.io.response.SeguidoresResponse
import com.example.myapplication.io.response.SeguidosResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.Call
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface ApiService {
    @POST("login") // Petición a la ruta del login
    fun postlogin(@Body request: LoginRequest): Call<LoginResponse>

    @POST("/switch-session") // Petición a la ruta del login
    fun switch_session(@Body request: LoginRequest): Call<LoginResponse>

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
    fun CambiarPass1(@Body requestBody: CambiarPass1Request): Call<Void>
    @POST("/verify-codigo")
    fun CambiarPass2(@Body requestBody: CambiarPass2Request): Call<CambiarPass2Response>
    @POST("/reset-password")
    fun CambiarPass3(
        @Header("Authorization") authHeader: String,
        @Body request: CambiarPass3Request
    ): Call<Void>

    @HTTP(method = "DELETE", path = "/delete-account", hasBody = true)
    fun deleteAccount(
        @Header("Authorization") authHeader: String,
        @Body request: DeleteAccountRequest
    ): Call<Void>

    // Nuevo método para cerrar sesión
    @POST("logout")  // <- Asegúrate de que esta línea esté presente
    fun logout(@Header("Authorization") authHeader: String): Call<Void>

    //Nuevo método para guardar el progreso en la db
    @PATCH("change-progreso")
    fun change_progreso(
        @Header("Authorization") authHeader: String,
        @Body progreso: ProgresoRequest
    ): Call<Void>

    // Nuevo método para el buscador
    @GET("/search")
    fun searchBuscador(
        @Header("Authorization") token: String,
        @Query("termino") termino: String
    ): Call<BuscadorResponse>

    @GET("/get-data-cancion")
    fun getInfoCancion(
        @Header("Authorization") token: String,
        @Query ("id") cancionId: String
    ): Call<CancionInfoResponse>

    @PUT("/put-cancion-sola")
    fun reproducirCancion(
        @Header("Authorization") token: String,
        @Header("sid") sid: String,
        @Body request: AudioRequest
    ): Call<AudioResponse>

    @PUT("/put-cancion-coleccion")
    fun reproducirColeccion(
        @Header("Authorization") token: String,
        @Header("sid") sid: String,
        @Body request: AudioColeccionRequest
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
    ): Call<Void>

    @PUT("/add-reproduccion")
    fun addReproduccion(
        @Header("Authorization") token: String,
    ): Call<AddReproduccionResponse>

    @GET("/get-cancion-actual")
    fun getCancionActual(@Header("Authorization") token: String): Call<CancionActualResponse>

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

    @GET("/get-mis-datos-artista")
    fun getMisDatosArtista(@Header("Authorization") token: String): Call<InfoPerfilArtistaResponse>

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

    @GET("/get-pendientes")
    fun getPendientes(
        @Header("Authorization") token: String
    ): Call<PendientesResponse>

    @POST("/check-artista")
    fun validarArtista(
        @Header("Authorization") token: String,
        @Body request: ValidarArtistaRequest
    ): Call<Void>

    @GET("/get-mis-albumes")
    fun getMisAlbumesArtista (
        @Header("Authorization") token: String
    ): Call<MisAlbumesResponse>

    @POST("/create-album")
    fun crearAlbum(
        @Header("Authorization") token: String,
        @Body request: CrearAlbumRequest
    ): Call<CrearAlbumResponse>

    @GET("/get-tags")
    fun getEtiquetas (
        @Header("Authorization") token: String
    ): Call<GetEtiquetasResponse>

    @POST("/create-cancion")
    fun crearCancion(
        @Header("Authorization") token: String,
        @Body request: CrearCancionRequest
    ): Call<Void>

    @GET("/get-estadisticas-album")
    fun getEstadisticasAlbum(
        @Header("Authorization") token: String,
        @Query("id") id: String
    ): Call<EstadisticasAlbumResponse>

    @GET("/get-mis-seguidos")
    fun getSeguidos(
        @Header("Authorization") token: String
    ): Call<SeguidosResponse>

    @GET("/get-mis-seguidores")
    fun getSeguidores(
        @Header("Authorization") token: String
    ): Call<SeguidoresResponse>

    @PUT("/change-follow")
    fun changeFollow(
        @Header("Authorization") token: String,
        @Body request: ChangeFollowRequest
    ): Call<Void>

    @GET("/get-mis-albumes")
    fun getMisAlbumes(
        @Header("Authorization") token: String
    ): Call<GetMisAlbumesResponse>

    @DELETE("/delete-album")
    fun deleteAlbum(
        @Header("Authorization") token: String,
        @Query("id") id: String
    ): Call<DeleteAlbumResponse>

    @PATCH("/change-album")
    fun changeAlbum(
        @Header("Authorization") token: String,
        @Query("id") id: String,
        @Body request: EditarAlbumRequest
    ): Call<DeleteAlbumResponse>

    @GET("/get-estadisticas-favs")
    fun getEstadisticasFavs(
        @Header("Authorization") token: String,
        @Query("id") id: String
    ): Call<GetEstadisticasFavsResponse>

    @GET("/get-estadisticas-playlists")
    fun getEstadisticasPlaylists(
        @Header("Authorization") token: String,
        @Query("id") id: String
    ): Call<GetEstadisticasPlaylistResponse>

    @DELETE("/delete-cancion")
    fun deleteCancion(
        @Header("Authorization") token: String,
        @Query("id") id: String
    ): Call<Void>
    
    @GET("/get-datos-oyente")
    fun getDatosOyente(
        @Header("Authorization") token: String,
        @Query("nombreUsuario") nombreUsuario: String
    ): Call<GetDatosOyenteResponse>

    @POST("/create-playlist")
    fun crearPlaylist(
        @Header("Authorization") token: String,
        @Body request: CreatePlaylistRequest
    ): Call<CrearPlaylistResponse>

    @GET("/search-for-playlist")
    fun searchForSongs(
        @Header("Authorization") token: String,
        @Query("termino") termino: String,
        @Query("playlist") playlistId: String
    ): Call<SearchPlaylistResponse>

    @POST("add-to-playlist")
    fun addSongToPlaylist(
        @Header("Authorization") token: String,
        @Body request: AddToPlaylistRequest
    ): Call<Void>

    @PUT("change-playlist")
    fun updatePlaylist(
        @Header("Authorization") token: String,
        @Body request: UpdatePlaylistRequest
    ): Call<Void>

    @HTTP(method = "DELETE", path = "/delete-playlist", hasBody = true)
    fun deletePlaylist(
        @Header("Authorization") token: String,
        @Body request: DeletePlaylistRequest
    ): Call<Void>

    @PATCH("change-privacidad")
    fun changePlaylistPrivacy(
        @Header("Authorization") token: String,
        @Body request: CambiarPrivacidadPlaylistRequest
    ): Call<Void>

    @GET("/get-invitaciones")
    fun getInvitaciones(
        @Header("Authorization") token: String
    ): Call<GetInvitacionesResponse>

    @GET("/get-novedades-musicales")
    fun getNovedades(
        @Header("Authorization") token: String
    ): Call<GetNovedadesResponse>

    @GET("/get-interacciones")
    fun getInteracciones(
        @Header("Authorization") token: String
    ): Call<GetInteraccionesResponse>

    @GET("/get-nuevos-seguidores")
    fun getNuevosSeguidores(
        @Header("Authorization") token: String
    ): Call<GetNuevosSeguidoresResponse>

    @GET("/has-notificaciones")
    fun hayNotificaciones(
        @Header("Authorization") token: String
    ): Call<HayNotificacionesResponse>

    @POST("/accept-invitacion")
    fun aceptarInvitacionPlaylist(
        @Header("Authorization") authHeader: String,
        @Body request: AceptarInvitacionRequest
    ): Call<Void>

    @HTTP(method = "DELETE", path = "/delete-invitacion", hasBody = true)
    fun rechazarInvitacionPlaylist(
        @Header("Authorization") authHeader: String,
        @Body request: AceptarInvitacionRequest
    ): Call<Void>

    @PATCH("/read-interacciones")
    fun verInteraccion(
        @Header("Authorization") authHeader: String,
        @Body request: VerInteraccionRequest
    ): Call<Void>

    @HTTP(method = "DELETE", path = "/delete-notificacion-cancion", hasBody = true)
    fun deleteNotificacionCancion(
        @Header("Authorization") authHeader: String,
        @Body request: DeleteNotiCancionRequest
    ): Call<Void>

    @HTTP(method = "DELETE", path = "/delete-notificacion-album", hasBody = true)
    fun deleteNotificacionAlbum(
        @Header("Authorization") authHeader: String,
        @Body request: DeleteNotiAlbumRequest
    ): Call<Void>

    @PATCH("/read-nuevo-seguidor")
    fun leerNotificacionSeguidor(
        @Header("Authorization") authHeader: String,
        @Body request: LeerNotiSeguidorRequest
    ): Call<Void>

    @POST("invite-to-playlist")
    fun invitePlaylist(
        @Header("Authorization") token: String,
        @Body request: InvitarPlaylistRequest
    ): Call<Void>

    @HTTP(method = "DELETE", path = "/delete-from-playlist", hasBody = true)
    fun removeSongFromPlaylist(
        @Header("Authorization") token: String,
        @Body request: DeleteFromPlaylistRequest
    ): Call<Void>

    @PATCH("/read-like")
    fun leerNotificacionLike(
        @Header("Authorization") authHeader: String,
        @Body request: LeerNotiLikeRequest
    ): Call<Void>

    @PATCH("/read-noizzito")
    fun leerNotificacionNoizzito(
        @Header("Authorization") authHeader: String,
        @Body request: LeerNotiNoizzitoRequest
    ): Call<Void>

    @GET("/get-mis-noizzys")
    fun getMisNoizzys(
        @Header("Authorization") token: String
    ): Call<MisNoizzysResponse>

    @GET("/search-for-noizzy")
    fun searchForSongsNoizzy(
        @Header("Authorization") token: String,
        @Query("termino") termino: String
    ): Call<SearchPlaylistResponse>

    @POST("post-noizzy")
    fun postNoizzy(
        @Header("Authorization") token: String,
        @Body request: PostNoizzyRequest
    ): Call<Void>

    @PUT("change-like")
    fun darLikeNoizzy(
        @Header("Authorization") token: String,
        @Body request: DarLikeNoizzyRequest
    ): Call<Void>

    @POST("post-noizzito")
    fun postNoizzito(
        @Header("Authorization") token: String,
        @Body request: PostNoizzitoRequest
    ): Call<Void>


    @GET("/get-datos-artista")
    fun getDatosArtista(
        @Header("Authorization") token: String,
        @Query("nombreUsuario") nombreUsuario: String
    ): Call<DatosArtistaResponse>

    @GET("/get-canciones-populares")
    fun getCancionesPopulares(
        @Header("Authorization") token: String,
        @Query("nombreUsuario") nombreUsuario: String
    ): Call<PopularesArtistaResponse>

    @GET("get-numero-canciones-favoritas")
    fun getNumeroCancionesFavoritas(
        @Header("Authorization") token: String,
        @Query("nombreUsuario") nombreUsuario: String
    ): Call<NumFavoritasArtistaResponse>

    @GET("get-albumes")
    fun albumesArtista(
        @Header("Authorization") token: String,
        @Query("nombreUsuario") nombreUsuario: String
    ): Call<DiscografiaAlbumArtistaResponse>

    @GET("get-canciones")
    fun cancionesArtista(
        @Header("Authorization") token: String,
        @Query("nombreUsuario") nombreUsuario: String
    ): Call<CancionesArtistaResponse>

    @GET("get-canciones-favoritas")
    fun cancionesFavsArtista(
        @Header("Authorization") token: String,
        @Query("nombreUsuario") nombreUsuario: String
    ): Call<CancionesFavsArtistaResponse>

    @GET("get-datos-album")
    fun getDatosAlbum(
        @Header("Authorization") token: String,
        @Query("id") id: String
    ): Call<DatosAlbumResponse>

    @GET("/get-playlists")
    fun getPlaylistOyente(
        @Header("Authorization") token: String,
        @Query("nombreUsuario") nombreUsuario: String
    ): Call<GetPlaylistOyenteResponse>

    @HTTP(method = "DELETE", path = "/leave-playlist", hasBody = true)
    fun leavePlaylist(
        @Header("Authorization") token: String,
        @Body request: LeavePlaylistRequest
    ): Call<Void>

    @HTTP(method = "DELETE", path = "/expel-from-playlist", hasBody = true)
    fun expulsarUsuario(
        @Header("Authorization") token: String,
        @Body request: ExpelUserRequest
    ): Call<Void>

    @PUT("change-contrasenya")
    fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Call<Void>





    
    companion object Factory {
        private const val BASE_URL = "https://api-noizz.onrender.com" // URL de la API
        //private const val BASE_URL = "http://192.168.0.62:5000"
        //private const val BASE_URL = "http://10.1.65.120:5000"
        fun create(): ApiService {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)  // Timeout de conexión
                .readTimeout(60, TimeUnit.SECONDS)     // Timeout de lectura
                .writeTimeout(60, TimeUnit.SECONDS)    // Timeout de escritura
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)  // Asigna el OkHttpClient con los timeouts
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}