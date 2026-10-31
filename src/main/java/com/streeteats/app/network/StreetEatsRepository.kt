package com.streeteats.app.network

import android.util.Log
import com.streeteats.app.model.CustomerRegistrationRequest
import com.streeteats.app.model.FoodTruck
import com.streeteats.app.model.MenuItem
import com.streeteats.app.model.OwnerRegistrationRequest
import com.streeteats.app.model.StopLocation
import com.streeteats.app.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class StreetEatsRepository(private val api: StreetEatsApi) {

    private val tag = "StreetEatsRepo"

    fun getTruckDetails(truckId: String): Flow<FoodTruck?> = flow {
        try {
            val response = api.getFoodTruckById(id = truckId)
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }

    suspend fun addMenuItem(menuItem: MenuItem, imageFile: File? = null): Boolean {
        return try {
            if (imageFile != null && imageFile.exists()) {
                val reqFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("image", imageFile.name, reqFile)
                val response = api.createMenuItemMultipart(
                    image = body,
                    itemId = menuItem.itemId.toRequestBody("text/plain".toMediaTypeOrNull()),
                    truckId = menuItem.truckId.toRequestBody("text/plain".toMediaTypeOrNull()),
                    itemName = menuItem.itemName.toRequestBody("text/plain".toMediaTypeOrNull()),
                    price = menuItem.price.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                    description = menuItem.description?.toRequestBody("text/plain".toMediaTypeOrNull())
                )
                response.isSuccessful
            } else {
                val response = api.createMenuItemJson(menuItem)
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception adding menu item", e)
            false
        }
    }

    suspend fun addStop(stop: StopLocation): Boolean {
        return try {
            val response = api.createStop(stop = stop)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createRating(truckId: String, customerId: String, score: Int, comment: String): Boolean {
        return try {
            val ratingId = "rat-" + UUID.randomUUID().toString().take(6)
            val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val body = mapOf(
                "rating_id" to ratingId,
                "truck_id" to truckId,
                "customer_id" to customerId,
                "score" to score,
                "comment" to comment,
                "created_at" to currentTime
            )
            val response = api.createRating(body)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(tag, "Exception creating rating", e)
            false
        }
    }

    suspend fun registerCustomer(fullName: String, email: String, password: String, mobileNumber: String): Boolean {
        return try {
            val customerId = "cst-" + UUID.randomUUID().toString().take(6)
            val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val request = CustomerRegistrationRequest(
                customerId = customerId,
                fullName = fullName,
                email = email,
                mobileNumber = mobileNumber,
                passwordHash = password,
                createdAt = currentTime
            )
            val response = api.registerCustomer(request)
            if (response.isSuccessful) {
                Log.d(tag, "Customer registered successfully: $customerId")
                true
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(tag, "Failed to register customer: HTTP ${response.code()} - $errorBody")
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception during customer registration", e)
            false
        }
    }

    suspend fun registerOwner(fullName: String, email: String, password: String, mobileNumber: String): Boolean {
        return try {
            val ownerId = "own-" + UUID.randomUUID().toString().take(6)
            val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val request = OwnerRegistrationRequest(
                ownerId = ownerId,
                fullName = fullName,
                email = email,
                mobileNumber = mobileNumber,
                passwordHash = password,
                createdAt = currentTime
            )
            val response = api.registerOwner(request)
            if (response.isSuccessful) {
                Log.d(tag, "Owner registered successfully: $ownerId")
                true
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(tag, "Failed to register owner: HTTP ${response.code()} - $errorBody")
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception during owner registration", e)
            false
        }
    }

    suspend fun loginUser(email: String, password: String): UserProfile? {
        return try {
            val custResponse = api.getCustomers()
            if (custResponse.isSuccessful) {
                val customers = custResponse.body()
                val match = customers?.find { it.email.equals(email, ignoreCase = true) }
                if (match != null) {
                    if (match.passwordHash == password || match.passwordHash.isNullOrBlank()) {
                        return UserProfile(match.fullName, match.email, "Customer", null)
                    }
                }
            }

            val ownerResponse = api.getOwners()
            if (ownerResponse.isSuccessful) {
                val owners = ownerResponse.body()
                val match = owners?.find { it.email.equals(email, ignoreCase = true) }
                if (match != null) {
                    if (match.passwordHash == password || match.passwordHash.isNullOrBlank()) {
                        return UserProfile(match.fullName, match.email, "Truck Owner", "Fuego Loco")
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(tag, "Exception during login verification", e)
            null
        }
    }
}
