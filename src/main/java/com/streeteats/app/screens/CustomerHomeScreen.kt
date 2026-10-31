package com.streeteats.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StarHalf
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import com.streeteats.app.model.StopLocation
import com.streeteats.app.ui.theme.*
import com.streeteats.app.ui.viewmodel.TruckViewModel

import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.streeteats.app.model.MenuItem
import com.streeteats.app.model.Review
import com.streeteats.app.ui.viewmodel.UserViewModel
import org.osmdroid.views.overlay.Marker
import java.util.UUID

// Mock data for trucks matching updated database schema (logo_path mapped to imageUrl, image_path in menu items)
data class TruckData(
    val id: String,
    val name: String,
    val imageUrl: String?, // Food truck owner uploaded logo (null = blank image)
    val cuisine: String,
    val location: GeoPoint,
    val status: TruckStatus,
    val menu: List<MenuItem>,
    val stops: List<StopLocation>
)

enum class TruckStatus(val label: String, val color: Color) {
    OPEN("Open", Color(0xFF38A169)),
    CLOSED("Closed", Color(0xFFE53935)),
    MOVING("Moving", Color(0xFFD69E2E))
}

val availableTrucks = listOf(
    TruckData(
        id = "trk-0001",
        name = "Fuego Loco",
        imageUrl = "https://cdn.streeteats.com/t/fuego.jpg",
        cuisine = "Mexican Street Food",
        location = GeoPoint(34.0522, -118.2437),
        status = TruckStatus.OPEN,
        menu = listOf(
            MenuItem("itm-0001", "trk-0001", "Al Pastor Taco", 50.00, "Marinated pork, pineapple, cilantro, onion", "https://cdn.streeteats.com/m/alpastor.jpg"),
            MenuItem("itm-0002", "trk-0001", "Carne Asada Burrito", 85.00, "Grilled steak, beans, rice, salsa", "https://cdn.streeteats.com/m/burrito.jpg")
        ),
        stops = listOf(
            StopLocation("stp-0001", "trk-0001", 1, "Venice Boardwalk", "1800 Ocean Front Walk", 33.9874, -118.4739, "Sunset Festival", "7 PM — 10 PM", "Here")
        )
    ),
    TruckData(
        id = "trk-0002",
        name = "Taco Express",
        imageUrl = null, // Blank image as requested
        cuisine = "Mexican Street Food",
        location = GeoPoint(34.076, -118.258),
        status = TruckStatus.CLOSED,
        menu = listOf(
            MenuItem("itm-0003", "trk-0002", "Double Smashburger", 110.50, "Two patties, cheese, secret sauce", null)
        ),
        stops = listOf(
            StopLocation("stp-0002", "trk-0002", 1, "Santa Monica Pier", "200 Santa Monica Pier", 34.0099, -118.4960, "Lunch Rush", "11 AM — 2 PM", "Moving")
        )
    ),
    TruckData(
        id = "trk-0003",
        name = "Smash & Grab",
        imageUrl = "https://cdn.streeteats.com/t/smash.jpg",
        cuisine = "Burgers & Fries",
        location = GeoPoint(34.048, -118.258),
        status = TruckStatus.OPEN,
        menu = listOf(
            MenuItem("itm-0004", "trk-0003", "Truffle Fries", 65.00, "Crispy fries with truffle oil", "https://cdn.streeteats.com/m/fries.jpg")
        ),
        stops = listOf(
            StopLocation("stp-0003", "trk-0003", 1, "Echo Park Lake", "751 Echo Park Ave", 34.0760, -118.2580, null, "4 PM — 6 PM", "Here")
        )
    )
)

@Composable
fun CustomerHomeScreen(
    viewModel: TruckViewModel = viewModel(),
    userViewModel: UserViewModel,
    navController: NavController
) {
    var selectedNav by remember { mutableStateOf(0) }
    var selectedTruckForMenu by remember { mutableStateOf<String?>(null) }
    
    val heartedTruckIds = remember { mutableStateListOf("trk-0001") }
    val followedTruckIds = remember { mutableStateListOf("trk-0001") }
    val followedUserNames = remember { mutableStateListOf<String>() }
    var currentTruckId by remember { mutableStateOf("trk-0001") }

    val reviewsList = remember {
        mutableStateListOf(
            Review("rat-0001", "cst-0001", "trk-0001", "Sarah Jenkins", "Venice Boardwalk", 5, "Amazing tacos! The al pastor sauce is incredible.", "2026-08-19 15:30"),
            Review("rat-0002", "cst-0001", "trk-0001", "Marcus Brody", "Santa Monica Pier", 4, "Great food and fast service!", "2026-09-07 17:06")
        )
    }

    var selectedTruckForDetails by remember { mutableStateOf<TruckData?>(null) }
    var showRateDialogForTruck by remember { mutableStateOf<TruckData?>(null) }

    Scaffold(
        containerColor = BackgroundCream,
        bottomBar = {
            CustomerBottomBar(selectedNav = selectedNav, onNavSelected = { 
                selectedNav = it 
                selectedTruckForMenu = null
            })
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (selectedTruckForMenu != null) {
                val truck = availableTrucks.find { it.id == selectedTruckForMenu }
                CustomerMenuView(
                    truckData = truck, 
                    onBack = { selectedTruckForMenu = null }
                )
            } else {
                when (selectedNav) {
                    0 -> MapTabView(
                        currentTruckId = currentTruckId,
                        onTruckSelect = { currentTruckId = it },
                        onOpenMenu = { selectedTruckForMenu = it },
                        heartedTruckIds = heartedTruckIds,
                        followedTruckIds = followedTruckIds,
                        reviewsList = reviewsList,
                        onToggleHeart = { id ->
                            if (heartedTruckIds.contains(id)) heartedTruckIds.remove(id) else heartedTruckIds.add(id)
                        },
                        onToggleFollow = { id ->
                            if (followedTruckIds.contains(id)) {
                                followedTruckIds.remove(id)
                                userViewModel.updateFollowingCount(-1)
                            } else {
                                followedTruckIds.add(id)
                                userViewModel.updateFollowingCount(1)
                            }
                        },
                        onOpenRestaurantDetails = { truck ->
                            selectedTruckForDetails = truck
                        },
                        onOpenRateDialog = { truck ->
                            showRateDialogForTruck = truck
                        }
                    )
                    1 -> SavedTabView(
                        heartedTruckIds = heartedTruckIds,
                        followedTruckIds = followedTruckIds,
                        onOpenMenu = { selectedTruckForMenu = it },
                        onToggleHeart = { id ->
                            if (heartedTruckIds.contains(id)) heartedTruckIds.remove(id) else heartedTruckIds.add(id)
                        },
                        onToggleFollow = { id ->
                            if (followedTruckIds.contains(id)) {
                                followedTruckIds.remove(id)
                                userViewModel.updateFollowingCount(-1)
                            } else {
                                followedTruckIds.add(id)
                                userViewModel.updateFollowingCount(1)
                            }
                        }
                    )
                    2 -> AccountScreen(userViewModel = userViewModel, onLogout = { 
                        navController.navigate("onboarding") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    })
                }
            }
        }
    }

    // Restaurant Details, Reviews, and Truck's Specific Stops Dialog
    if (selectedTruckForDetails != null) {
        val truck = selectedTruckForDetails!!
        val isFollowing = followedTruckIds.contains(truck.id)
        val isHearted = heartedTruckIds.contains(truck.id)
        val avgRating = if (reviewsList.isNotEmpty()) reviewsList.map { it.rating.toDouble() }.average() else 4.7

        RestaurantDetailsDialog(
            truck = truck,
            reviews = reviewsList,
            avgRating = avgRating,
            isFollowing = isFollowing,
            isHearted = isHearted,
            followedUserNames = followedUserNames,
            onFollowToggle = {
                if (isFollowing) {
                    followedTruckIds.remove(truck.id)
                    userViewModel.updateFollowingCount(-1)
                } else {
                    followedTruckIds.add(truck.id)
                    userViewModel.updateFollowingCount(1)
                }
            },
            onHeartToggle = {
                if (isHearted) heartedTruckIds.remove(truck.id) else heartedTruckIds.add(truck.id)
            },
            onToggleUserFollow = { userName ->
                if (followedUserNames.contains(userName)) {
                    followedUserNames.remove(userName)
                    userViewModel.updateFollowingCount(-1)
                } else {
                    followedUserNames.add(userName)
                    userViewModel.updateFollowingCount(1)
                }
            },
            onDismiss = { selectedTruckForDetails = null },
            onTapToRate = {
                showRateDialogForTruck = truck
                selectedTruckForDetails = null
            },
            onOpenMenu = {
                selectedTruckForMenu = truck.id
                selectedTruckForDetails = null
            }
        )
    }

    // Tap to Rate Dialog
    if (showRateDialogForTruck != null) {
        val truck = showRateDialogForTruck!!
        RateStopDialog(
            truck = truck,
            onDismiss = { showRateDialogForTruck = null },
            onSubmitReview = { stopName, rating, comment ->
                val newReview = Review(
                    id = "rat-" + UUID.randomUUID().toString().take(6),
                    customerId = "cst-0001",
                    truckId = truck.id,
                    userName = userViewModel.userProfile.value?.name ?: "Customer",
                    stopName = stopName,
                    rating = rating,
                    comment = comment,
                    timestamp = "Just now"
                )
                reviewsList.add(0, newReview)
                showRateDialogForTruck = null
            }
        )
    }
}

@Composable
fun MapTabView(
    currentTruckId: String,
    onTruckSelect: (String) -> Unit,
    onOpenMenu: (String) -> Unit,
    heartedTruckIds: List<String>,
    followedTruckIds: List<String>,
    reviewsList: List<Review>,
    onToggleHeart: (String) -> Unit,
    onToggleFollow: (String) -> Unit,
    onOpenRestaurantDetails: (TruckData) -> Unit,
    onOpenRateDialog: (TruckData) -> Unit
) {
    var showNearMeMenu by remember { mutableStateOf(false) }
    val avgRating = if (reviewsList.isNotEmpty()) reviewsList.map { it.rating.toDouble() }.average() else 4.7

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CustomerHeader()
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Food Trucks Near You",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                IconButton(onClick = { showNearMeMenu = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter Near Me", tint = PrimaryRed)
                }
                DropdownMenu(expanded = showNearMeMenu, onDismissRequest = { showNearMeMenu = false }) {
                    Text("All Active Trucks", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                    availableTrucks.forEach { truck ->
                        DropdownMenuItem(
                            text = { Text(truck.name) },
                            onClick = {
                                onTruckSelect(truck.id)
                                onOpenRestaurantDetails(truck)
                                showNearMeMenu = false
                            }
                        )
                    }
                }
            }
        }

        // List of Trucks Near You (Owner logo or blank image box if none)
        items(availableTrucks) { truck ->
            val truckIsHearted = heartedTruckIds.contains(truck.id)
            val truckIsFollowed = followedTruckIds.contains(truck.id)

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        onTruckSelect(truck.id)
                        onOpenRestaurantDetails(truck) 
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Food truck owner uploaded logo (or blank image if none)
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF5F5F5)
                            ) {
                                if (!truck.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = truck.imageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(modifier = Modifier.background(Color(0xFFEEEEEE)))
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = truck.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(text = truck.cuisine, color = Color.Gray, fontSize = 13.sp)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { onToggleHeart(truck.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (truckIsHearted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Bookmark",
                                    tint = if (truckIsHearted) PrimaryRed else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Button(
                                onClick = { onToggleFollow(truck.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (truckIsFollowed) Color(0xFF38A169) else PrimaryRed
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (truckIsFollowed) "Following" else "Follow",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${"%.1f".format(avgRating)} / 5.0",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.DarkGray
                            )
                        }

                        TextButton(
                            onClick = { 
                                onTruckSelect(truck.id)
                                onOpenRestaurantDetails(truck) 
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = "Menu & Reviews ›", color = PrimaryRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = PrimaryRed)) {
                        append("Street")
                    }
                    append("Eats")
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "3 trucks active near you",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
        Surface(
            shape = CircleShape,
            color = Color.White,
            modifier = Modifier.size(48.dp),
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Notifications, contentDescription = null, tint = Color.Black)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(PrimaryRed, CircleShape)
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}

@Composable
fun SavedTabView(
    heartedTruckIds: List<String>,
    followedTruckIds: List<String>,
    onOpenMenu: (String) -> Unit,
    onToggleHeart: (String) -> Unit,
    onToggleFollow: (String) -> Unit
) {
    val savedTrucks = availableTrucks.filter { heartedTruckIds.contains(it.id) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = PrimaryRed)) {
                            append("Saved ")
                        }
                        append("Trucks")
                    },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(text = "${savedTrucks.size} trucks bookmarked", color = Color.Gray, fontSize = 14.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (savedTrucks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No saved trucks yet", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.DarkGray)
                    Text("Heart trucks on the map to save them here", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(savedTrucks) { truck ->
                    val isFollowed = followedTruckIds.contains(truck.id)
                    SavedTruckCard(
                        truck = truck,
                        isHearted = true,
                        isFollowed = isFollowed,
                        onOpenMenu = { onOpenMenu(truck.id) },
                        onToggleHeart = { onToggleHeart(truck.id) },
                        onToggleFollow = { onToggleFollow(truck.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SavedTruckCard(
    truck: TruckData,
    isHearted: Boolean,
    isFollowed: Boolean,
    onOpenMenu: () -> Unit,
    onToggleHeart: () -> Unit,
    onToggleFollow: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().clickable { onOpenMenu() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(70.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (!truck.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = truck.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(modifier = Modifier.background(Color(0xFFEEEEEE)))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = truck.name, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 18.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        IconButton(
                            onClick = onToggleHeart,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isHearted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Remove Bookmark",
                                tint = PrimaryRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(text = truck.cuisine, color = Color.Gray, fontSize = 14.sp)
                    
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(14.dp))
                            }
                        }

                        Button(
                            onClick = onToggleFollow,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowed) Color(0xFF38A169) else PrimaryRed
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (isFollowed) "✓ Following" else "+ Follow",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Menu view showing owner-uploaded images for menu items (or blank image if none)
@Composable
fun CustomerMenuView(truckData: TruckData?, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(BackgroundCream).padding(24.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }
        
        Text(text = "${truckData?.name ?: "Truck"}'s Menu", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val menu = truckData?.menu ?: emptyList()
            items(menu) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column {
                        // Display the image uploaded by the Food Truck Owner, or blank image box if none
                        if (!item.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(Color(0xFFEEEEEE))
                            )
                        }
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = item.itemName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(text = "$${"%.2f".format(item.price)}", color = PrimaryRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            item.description?.let {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = it, color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dialog displaying Restaurant Details, Owner Logo, Stops, Reviews, and click reviewer to follow them
@Composable
fun RestaurantDetailsDialog(
    truck: TruckData,
    reviews: List<Review>,
    avgRating: Double,
    isFollowing: Boolean,
    isHearted: Boolean,
    followedUserNames: List<String>,
    onFollowToggle: () -> Unit,
    onHeartToggle: () -> Unit,
    onToggleUserFollow: (String) -> Unit,
    onDismiss: () -> Unit,
    onTapToRate: () -> Unit,
    onOpenMenu: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (!truck.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = truck.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = truck.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onHeartToggle, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (isHearted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Bookmark",
                            tint = if (isHearted) PrimaryRed else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Button(
                        onClick = onFollowToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowing) Color(0xFF38A169) else PrimaryRed
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(text = if (isFollowing) "Following" else "Follow", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = truck.cuisine, color = Color.Gray, fontSize = 13.sp)
                    Button(
                        onClick = onOpenMenu,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Menu", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Truck's Own Stops (decided by the food truck)
                Text(text = "Today's Stops", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                if (truck.stops.isEmpty()) {
                    Text("No stops scheduled for today.", fontSize = 12.sp, color = Color.Gray)
                } else {
                    truck.stops.forEach { stop ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F1)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = stop.locationName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = stop.timeWindow, fontSize = 11.sp, color = PrimaryRed, fontWeight = FontWeight.Bold)
                                }
                                Text(text = stop.streetAddress, fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Average Rating Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F1)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Average Rating", fontSize = 11.sp, color = Color.Gray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "%.1f".format(avgRating),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryRed
                                )
                                Text(text = " / 5.0", fontSize = 13.sp, color = Color.Gray)
                            }
                        }
                        Row {
                            repeat(5) { index ->
                                val starValue = index + 1
                                val icon = if (avgRating >= starValue) Icons.Default.Star
                                else if (avgRating >= starValue - 0.5) Icons.AutoMirrored.Outlined.StarHalf
                                else Icons.Outlined.StarOutline

                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = PrimaryRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customer Reviews (${reviews.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    TextButton(onClick = onTapToRate, contentPadding = PaddingValues(0.dp)) {
                        Text("Tap to Rate", color = PrimaryRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(reviews) { review ->
                        val isUserFollowed = followedUserNames.contains(review.userName)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Clickable Reviewer Name to Follow them
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { onToggleUserFollow(review.userName) }
                                    ) {
                                        Text(
                                            text = review.userName, 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 13.sp,
                                            color = PrimaryRed
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = if (isUserFollowed) Color(0xFF38A169) else PrimaryRed.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (isUserFollowed) "Following" else "+ Follow",
                                                fontSize = 9.sp,
                                                color = if (isUserFollowed) Color.White else PrimaryRed,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Row {
                                        repeat(review.rating) {
                                            Icon(
                                                Icons.Default.Star,
                                                contentDescription = null,
                                                tint = PrimaryRed,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                                Text(text = "Stop: ${review.stopName}", fontSize = 10.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = review.comment, fontSize = 12.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onTapToRate,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text("Leave a Rating & Review", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.Gray, fontSize = 13.sp)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

// Dialog for "Tap to Rate" showing Dropdown of all Stops, 0-5 Star Rating system, and Comment input
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateStopDialog(
    truck: TruckData,
    onDismiss: () -> Unit,
    onSubmitReview: (stopName: String, rating: Int, comment: String) -> Unit
) {
    var selectedStopName by remember { mutableStateOf(truck.stops.firstOrNull()?.locationName ?: "Venice Boardwalk") }
    var stopsMenuExpanded by remember { mutableStateOf(false) }
    var selectedRating by remember { mutableStateOf(5) } // 0 to 5
    var commentText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RateReview, contentDescription = null, tint = PrimaryRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Rate & Review ${truck.name}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Dropdown menu of truck's stops
                Column {
                    Text("Select Stop Location *", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        OutlinedTextField(
                            value = selectedStopName,
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { stopsMenuExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Stop")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { stopsMenuExpanded = true },
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = stopsMenuExpanded,
                            onDismissRequest = { stopsMenuExpanded = false }
                        ) {
                            truck.stops.forEach { stop ->
                                DropdownMenuItem(
                                    text = {
                                        CustomStyledStopText(stop.locationName, stop.timeWindow)
                                    },
                                    onClick = {
                                        selectedStopName = stop.locationName
                                        stopsMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Interactive 0-5 Star System
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Your Rating: $selectedRating / 5 Stars", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (star in 1..5) {
                            IconButton(
                                onClick = {
                                    selectedRating = if (selectedRating == 1 && star == 1) 0 else star
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (star <= selectedRating) Icons.Default.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "$star Stars",
                                    tint = if (star <= selectedRating) PrimaryRed else Color.LightGray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    if (selectedRating == 0) {
                        Text("0 Stars Selected", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                // Comment input
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text("Leave a comment...") },
                    placeholder = { Text("How was the food, service, and vibe?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (commentText.isNotBlank()) {
                        onSubmitReview(selectedStopName, selectedRating, commentText)
                    }
                },
                enabled = commentText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Submit Review", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
fun CustomStyledStopText(name: String, time: String) {
    Column {
        Text(name, fontWeight = FontWeight.Bold)
        Text(time, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun CustomerBottomBar(selectedNav: Int, onNavSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        NavigationBarItem(
            selected = selectedNav == 0,
            onClick = { onNavSelected(0) },
            icon = { Icon(Icons.Default.Room, contentDescription = null) },
            label = { Text("Near Me") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryRed,
                selectedTextColor = PrimaryRed,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color(0xFFFFF1F1)
            )
        )
        NavigationBarItem(
            selected = selectedNav == 1,
            onClick = { onNavSelected(1) },
            icon = { Icon(if (selectedNav == 1) Icons.Filled.Favorite else Icons.Default.FavoriteBorder, contentDescription = null) },
            label = { Text("Saved") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryRed,
                selectedTextColor = PrimaryRed,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color(0xFFFFF1F1)
            )
        )
        NavigationBarItem(
            selected = selectedNav == 2,
            onClick = { onNavSelected(2) },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Account") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryRed,
                selectedTextColor = PrimaryRed,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color(0xFFFFF1F1)
            )
        )
    }
}
