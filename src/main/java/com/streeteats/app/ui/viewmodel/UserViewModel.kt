package com.streeteats.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streeteats.app.model.UserProfile
import com.streeteats.app.network.RetrofitClient
import com.streeteats.app.network.StreetEatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {
    private val repository = StreetEatsRepository(RetrofitClient.api)

    private val _userProfile = MutableStateFlow<UserProfile?>(
        UserProfile(
            name = "Juan Dela Cruz",
            email = "juan@example.com",
            role = "Customer",
            foodTruckName = null,
            followersCount = 42,
            followingCount = 12
        )
    )
    val userProfile: StateFlow<UserProfile?> = _userProfile

    suspend fun loginUser(email: String, password: String): UserProfile? {
        val profile = repository.loginUser(email, password)
        if (profile != null) {
            _userProfile.value = profile
        }
        return profile
    }

    fun registerAndLogin(name: String, email: String, role: String, password: String, mobileNumber: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isOwner = role.contains("Owner", ignoreCase = true)
            val success = if (isOwner) {
                repository.registerOwner(name, email, password, mobileNumber)
            } else {
                repository.registerCustomer(name, email, password, mobileNumber)
            }
            
            login(name, email, role)
            onComplete(success)
        }
    }

    fun login(name: String, email: String, role: String, foodTruckName: String? = null) {
        val isOwner = role.contains("Owner", ignoreCase = true)
        _userProfile.value = UserProfile(
            name = name,
            email = email,
            role = if (isOwner) "Truck Owner" else "Customer",
            foodTruckName = if (isOwner) (foodTruckName ?: "Fuego Loco") else null,
            followersCount = if (isOwner) 241 else 42,
            followingCount = if (isOwner) 18 else 12
        )
    }

    fun updateFollowingCount(delta: Int) {
        _userProfile.update { current ->
            current?.copy(followingCount = (current.followingCount + delta).coerceAtLeast(0))
        }
    }

    fun updateFoodTruckName(newName: String) {
        _userProfile.update { current ->
            current?.copy(foodTruckName = newName)
        }
    }

    fun logout() {
        _userProfile.value = null
    }
}
