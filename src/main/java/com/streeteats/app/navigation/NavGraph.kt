package com.streeteats.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.streeteats.app.screens.*
import com.streeteats.app.ui.viewmodel.UserViewModel

@Composable
fun StreetEatsNavGraph(userViewModel: UserViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "onboarding") {
        composable("onboarding") {
            RoleSelectionScreen(
                onCustomerSelected = { navController.navigate("create_account_customer") },
                onOwnerSelected = { navController.navigate("create_account_owner") },
                onSignInSelected = { navController.navigate("sign_in") }
            )
        }
        
        composable("sign_in") {
            SignInScreen(
                onBack = { navController.popBackStack() },
                onNavigateToRegister = { navController.navigate("create_account_customer") },
                signInAction = { email, password ->
                    userViewModel.loginUser(email, password)
                },
                onSignInSuccess = { role ->
                    if (role.contains("Owner", ignoreCase = true)) {
                        navController.navigate("owner_dashboard") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    } else {
                        navController.navigate("customer_home") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("create_account_customer") {
            CreateAccountScreen(
                role = "Customer",
                onBack = { navController.popBackStack() },
                onSignInClicked = { navController.navigate("sign_in") },
                onAccountCreated = { name, email, password, mobileNumber ->
                    userViewModel.registerAndLogin(name, email, "Customer", password, mobileNumber) { _ ->
                        navController.navigate("customer_home") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable("create_account_owner") {
            CreateAccountScreen(
                role = "Truck Owner",
                onBack = { navController.popBackStack() },
                onSignInClicked = { navController.navigate("sign_in") },
                onAccountCreated = { name, email, password, mobileNumber ->
                    userViewModel.registerAndLogin(name, email, "Truck Owner", password, mobileNumber) { _ ->
                        navController.navigate("owner_dashboard") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("customer_home") {
            CustomerHomeScreen(userViewModel = userViewModel, navController = navController)
        }
        
        composable("owner_dashboard") {
            OwnerDashboardScreen(userViewModel = userViewModel, navController = navController)
        }
    }
}
