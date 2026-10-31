package com.streeteats.app.model

import com.google.gson.annotations.SerializedName

data class FoodTruck(
    @SerializedName("truck_id") val truckId: String,
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("truck_name") val truckName: String,
    @SerializedName("cuisine_tag") val cuisineTag: String,
    @SerializedName("current_status") val currentStatus: String,
    @SerializedName("logo_url") val imageUrl: String?, // mapped from logo_path
    @SerializedName("last_pinged_at") val lastPingedAt: String?,
    @SerializedName("menu_items") val menuItems: List<MenuItem>? = emptyList(),
    @SerializedName("stops") val stops: List<StopLocation>? = emptyList()
)

data class MenuItem(
    @SerializedName("item_id") val itemId: String,
    @SerializedName("truck_id") val truckId: String,
    @SerializedName("item_name") val itemName: String,
    @SerializedName("price") val price: Double,
    @SerializedName("description") val description: String?,
    @SerializedName("image_url") val imageUrl: String? // mapped from image_path
)

data class StopLocation(
    @SerializedName("stop_id") val stopId: String,
    @SerializedName("truck_id") val truckId: String,
    @SerializedName("stop_order") val stopOrder: Int = 1,
    @SerializedName("location_name") val locationName: String,
    @SerializedName("street_address") val streetAddress: String,
    @SerializedName("latitude") val latitude: Double = 34.0522,
    @SerializedName("longitude") val longitude: Double = -118.2437,
    @SerializedName("event_tag") val eventTag: String? = null,
    @SerializedName("time_window") val timeWindow: String,
    @SerializedName("stop_status") val stopStatus: String = "Here"
)

data class UserProfile(
    val name: String,
    val email: String,
    val role: String, // "Truck Owner" or "Customer"
    val foodTruckName: String? = null,
    val foodTruckLogoUrl: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0
)

data class Review(
    @SerializedName("rating_id") val id: String,
    @SerializedName("customer_id") val customerId: String?,
    @SerializedName("truck_id") val truckId: String?,
    val userName: String = "Customer",
    val stopName: String = "General",
    @SerializedName("score") val rating: Int, // 0 to 5 stars
    @SerializedName("comment") val comment: String,
    @SerializedName("created_at") val timestamp: String = "Just now"
)

data class CustomerRegistrationRequest(
    @SerializedName("customer_id") val customerId: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("mobile_number") val mobileNumber: String,
    @SerializedName("password_hash") val passwordHash: String,
    @SerializedName("created_at") val createdAt: String
)

data class OwnerRegistrationRequest(
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("mobile_number") val mobileNumber: String,
    @SerializedName("password_hash") val passwordHash: String,
    @SerializedName("created_at") val createdAt: String
)

data class CustomerRecord(
    @SerializedName("customer_id") val customerId: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("password_hash") val passwordHash: String?
)

data class OwnerRecord(
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("password_hash") val passwordHash: String?
)
