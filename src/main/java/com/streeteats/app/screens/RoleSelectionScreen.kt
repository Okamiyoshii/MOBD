package com.streeteats.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streeteats.app.ui.theme.BackgroundCream
import com.streeteats.app.ui.theme.SecondaryCoral
import com.streeteats.app.ui.theme.PrimaryRed

@Composable
fun RoleSelectionScreen(
    onCustomerSelected: () -> Unit,
    onOwnerSelected: () -> Unit,
    onSignInSelected: () -> Unit
) {
    var selectedRole by remember { mutableStateOf("customer") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = "Who are you?",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
            Text(
                text = "Choose your role or sign in",
                color = Color.Gray,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            RoleOptionCard(
                title = "I'm a Customer",
                subtitle = "Follow trucks, track live routes, rate stops, discover food near you.",
                icon = Icons.Filled.Restaurant,
                iconColor = Color(0xFFB39DDB),
                isSelected = selectedRole == "customer",
                onClick = { 
                    selectedRole = "customer"
                    onCustomerSelected()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            RoleOptionCard(
                title = "I'm a Truck Owner",
                subtitle = "Manage your route, upload your menu, advertise stops, grow your audience.",
                icon = Icons.Filled.LocalShipping,
                iconColor = Color(0xFFFFB74D),
                isSelected = selectedRole == "owner",
                onClick = { 
                    selectedRole = "owner"
                    onOwnerSelected()
                }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            Button(
                onClick = {
                    if (selectedRole == "customer") onCustomerSelected() else onOwnerSelected()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryCoral),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Register as ${if (selectedRole == "customer") "Customer" else "Truck Owner"}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onSignInSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryRed),
                border = BorderStroke(2.dp, PrimaryRed)
            ) {
                Text(text = "Sign In with Existing Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun RoleOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = if (isSelected) BorderStroke(2.dp, PrimaryRed) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = subtitle, color = Color.Gray, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}
