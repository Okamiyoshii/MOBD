package com.streeteats.app.network

import android.util.Log
import com.streeteats.app.model.CustomerRegistrationRequest
import com.streeteats.app.model.CustomerRecord
import com.streeteats.app.model.FoodTruck
import com.streeteats.app.model.MenuItem
import com.streeteats.app.model.OwnerRegistrationRequest
import com.streeteats.app.model.OwnerRecord
import com.streeteats.app.model.StopLocation
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

interface StreetEatsApi {

    @GET("Food_Truck.php")
    suspend fun getFoodTrucks(): Response<List<FoodTruck>>

    @GET("Food_Truck.php")
    suspend fun getFoodTruckById(
        @Query("id") id: String
    ): Response<FoodTruck>

    @GET("Users_Customers.php")
    suspend fun getCustomers(): Response<List<CustomerRecord>>

    @GET("Truck_Owners.php")
    suspend fun getOwners(): Response<List<OwnerRecord>>

    @POST("Menu_Items.php")
    suspend fun createMenuItemJson(
        @Body menuItem: MenuItem
    ): Response<MenuItem>

    @Multipart
    @POST("Menu_Items.php")
    suspend fun createMenuItemMultipart(
        @Part image: MultipartBody.Part?,
        @Part("item_id") itemId: RequestBody,
        @Part("truck_id") truckId: RequestBody,
        @Part("item_name") itemName: RequestBody,
        @Part("price") price: RequestBody,
        @Part("description") description: RequestBody?
    ): Response<MenuItem>

    @POST("Stops_Locations.php")
    suspend fun createStop(
        @Body stop: StopLocation
    ): Response<StopLocation>

    @POST("Users_Customers.php")
    suspend fun registerCustomer(
        @Body customer: CustomerRegistrationRequest
    ): Response<Any>

    @POST("Truck_Owners.php")
    suspend fun registerOwner(
        @Body owner: OwnerRegistrationRequest
    ): Response<Any>

    @POST("Ratings.php")
    suspend fun createRating(
        @Body rating: Map<String, Any>
    ): Response<Any>

    @POST("Bookmarks_Follows.php")
    suspend fun toggleBookmark(
        @Body bookmark: Map<String, Any>
    ): Response<Any>
}

object RetrofitClient {
    private const val BASE_URL = "https://dk8.df0.mytemp.website/Group12/"

    val api: StreetEatsApi by lazy {
        val loggingInterceptor = Interceptor { chain ->
            val request = chain.request()
            Log.d("NetworkLog", "--> HTTP REQUEST: ${request.method} ${request.url}")
            try {
                val response = chain.proceed(request)
                Log.d("NetworkLog", "<-- HTTP RESPONSE: ${response.code} ${response.message} for ${request.url}")
                response
            } catch (e: Exception) {
                Log.e("NetworkLog", "XX HTTP EXCEPTION for ${request.url}", e)
                throw e
            }
        }

        val client = try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .addInterceptor(loggingInterceptor)
                .build()
        } catch (e: Exception) {
            OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build()
        }

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(StreetEatsApi::class.java)
    }
}
