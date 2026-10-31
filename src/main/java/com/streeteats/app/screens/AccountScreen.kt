package com.streeteats.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streeteats.app.ui.theme.BackgroundCream
import com.streeteats.app.ui.theme.PrimaryRed
import com.streeteats.app.ui.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(userViewModel: UserViewModel, onLogout: () -> Unit) {
    val profile by userViewModel.userProfile.collectAsState()
    
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showEditTruckNameDialog by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var newTruckName by remember { mutableStateOf(profile?.foodTruckName ?: "Fuego Loco") }

    val isOwner = profile?.role?.contains("Owner", ignoreCase = true) == true
    val userRoleLabel = if (isOwner) "Food Truck Owner" else "Customer"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar with Settings Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Account",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            IconButton(
                onClick = { showSettingsDialog = true },
                modifier = Modifier
                    .background(Color.White, CircleShape)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.DarkGray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Avatar
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color.LightGray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // User Name & Email
        Text(text = profile?.name ?: "Guest User", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(text = profile?.email ?: "guest@streeteats.com", color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(12.dp))

        // User Role Badge (Food Truck Owner / Customer)
        Surface(
            color = if (isOwner) Color(0xFFFFF3E0) else Color(0xFFE6FFFA),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = userRoleLabel,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = if (isOwner) Color(0xFFE65100) else Color(0xFF38A169),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // If Food Truck Owner: Show Food Truck Name
        if (isOwner) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFF1F1),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = PrimaryRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Food Truck",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = profile?.foodTruckName ?: "Fuego Loco",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = {
                        newTruckName = profile?.foodTruckName ?: "Fuego Loco"
                        showEditTruckNameDialog = true
                    }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Food Truck Name",
                            tint = PrimaryRed
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Followers and Following Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.weight(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${profile?.followersCount ?: 0}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Followers",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.weight(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${profile?.followingCount ?: 0}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD69E2E)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Following",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Settings Button Tile (also accessible from top right)
        OutlinedButton(
            onClick = { showSettingsDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.DarkGray)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Account Settings", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logout Button
        Button(
            onClick = {
                userViewModel.logout()
                onLogout()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Settings Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = PrimaryRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Settings", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Push Notifications", fontSize = 14.sp)
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryRed)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    Text("App Version: 1.0.0 (Build 101)", color = Color.Gray, fontSize = 12.sp)
                    Text("Privacy Policy & Terms of Service", color = PrimaryRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Close", color = PrimaryRed, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    // Edit Food Truck Name Dialog
    if (showEditTruckNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditTruckNameDialog = false },
            title = { Text("Edit Food Truck Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newTruckName,
                    onValueChange = { newTruckName = it },
                    label = { Text("Food Truck Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTruckName.isNotBlank()) {
                            userViewModel.updateFoodTruckName(newTruckName)
                        }
                        showEditTruckNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTruckNameDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }
}
