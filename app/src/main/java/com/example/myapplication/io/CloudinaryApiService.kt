package com.example.myapplication.io

import com.example.myapplication.io.response.CloudinaryResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface CloudinaryApiService {

    @Multipart
    @POST("v1_1/{cloud_name}/image/upload")
    fun uploadImage(
        @Path("cloud_name") cloudName: String,
        @Part("file") file: MultipartBody.Part,
        @Part("api_key") apiKey: RequestBody,
        @Part("timestamp") timestamp: RequestBody,
        @Part("signature") signature: RequestBody,
        @Part("folder") folder: RequestBody
    ): Call<CloudinaryResponse>

    companion object Factory {
        private const val BASE_URL = "https://api.cloudinary.com" // URL de la API
        //private const val BASE_URL = "http://172.20.10.4:5000"
        //private const val BASE_URL = "http://10.1.65.120:5000"
        fun create(): CloudinaryApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(CloudinaryApiService::class.java)
        }
    }
}