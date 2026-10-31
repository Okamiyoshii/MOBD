package com.streeteats.app.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.streeteats.app.model.FoodTruck
import com.streeteats.app.model.MenuItem
import com.streeteats.app.model.StopLocation
import com.streeteats.app.ui.theme.*
import com.streeteats.app.ui.viewmodel.TruckViewModel
import com.streeteats.app.ui.viewmodel.UserViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.UUID

@Composable
fun OwnerDashboardScreen(
    viewModel: TruckViewModel = viewModel(),
    userViewModel: UserViewModel,
    navController: NavController
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedNav by remember { mutableStateOf(0) }
    
    val truckData by viewModel.truckState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchTruckDetails("trk-0001")
    }

    Scaffold(
        containerColor = BackgroundCream,
        topBar = {
            if (selectedNav == 0) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = buildAnnotatedString {
                            append("My ")
                            withStyle(style = SpanStyle(color = PrimaryRed)) {
                                append("Truck")
                            }
                        },
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Manage route, direct image uploads, and stats",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CustomTabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
                }
            }
        },
        bottomBar = {
            BottomNavigationBar(selectedNav = selectedNav, onNavSelected = { selectedNav = it })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedNav) {
                0 -> {
                    when (selectedTab) {
                        0 -> OwnerRouteTab(truckData)
                        1 -> OwnerMenuTab(truckData?.menuItems ?: emptyList(), viewModel)
                        2 -> OwnerStatsTab()
                    }
                }
                1 -> AccountScreen(userViewModel = userViewModel, onLogout = {
                    navController.navigate("onboarding") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                })
            }
        }
    }
}

@Composable
fun CustomTabRow(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        val tabs = listOf("Route", "Menu", "Stats")
        tabs.forEachIndexed { index, title ->
            Column(
                modifier = Modifier
                    .padding(end = 24.dp)
                    .clickableWithoutRipple { onTabSelected(index) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedTab == index) PrimaryRed else Color.Gray
                )
                if (selectedTab == index) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(2.dp)
                            .background(PrimaryRed)
                    )
                }
            }
        }
    }
}

@Composable
fun OwnerRouteTab(truck: FoodTruck?) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var mapCenter by remember { mutableStateOf<GeoPoint?>(null) }
    var lastPingedLocation by remember { mutableStateOf<GeoPoint?>(null) }

    val stopsList = remember {
        mutableStateListOf(
            StopLocation("stp-0001", "trk-0001", 1, "Venice Boardwalk", "1800 Ocean Front Walk", 33.9874, -118.4739, "Sunset Festival", "7 PM — 10 PM", "Here")
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val defaultPoint = GeoPoint(34.0522, -118.2437)
        if (isGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val point = GeoPoint(location.latitude, location.longitude)
                        mapCenter = point
                        lastPingedLocation = point
                    } else {
                        mapCenter = defaultPoint
                        lastPingedLocation = defaultPoint
                    }
                }
            } catch (e: SecurityException) {
                mapCenter = defaultPoint
                lastPingedLocation = defaultPoint
            }
        } else {
            mapCenter = defaultPoint
            lastPingedLocation = defaultPoint
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TruckInfoCard(truck)
        }
        item {
            PingLocationButton(
                lastPingedLocation = lastPingedLocation,
                onPing = {
                    val defaultPoint = GeoPoint(34.0522, -118.2437)
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) -> {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                if (location != null) {
                                    val point = GeoPoint(location.latitude, location.longitude)
                                    mapCenter = point
                                    lastPingedLocation = point
                                } else {
                                    mapCenter = defaultPoint
                                    lastPingedLocation = defaultPoint
                                }
                            }.addOnFailureListener {
                                mapCenter = defaultPoint
                                lastPingedLocation = defaultPoint
                            }
                        }
                        else -> {
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                }
            )
        }
        item {
            MapCard(centerPoint = mapCenter)
        }
        item {
            StopsSection(
                stops = stopsList,
                onAddStop = { newStop ->
                    stopsList.add(newStop)
                }
            )
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TruckInfoCard(truck: FoodTruck?) {
    var truckLogoUri by remember { mutableStateOf<Uri?>(null) }
    var showLogoEditDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            truckLogoUri = uri
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F1)),
        modifier = Modifier.fillMaxWidth().clickable { showLogoEditDialog = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                if (truckLogoUri != null) {
                    AsyncImage(
                        model = truckLogoUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!truck?.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = truck?.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(28.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = truck?.truckName ?: "Fuego Loco",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = truck?.cuisineTag ?: "Mexican Street Food",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            Surface(
                color = Color(0xFFE6FFFA),
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF38A169), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = truck?.currentStatus ?: "Open",
                        color = Color(0xFF38A169),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showLogoEditDialog) {
        AlertDialog(
            onDismissRequest = { showLogoEditDialog = false },
            title = { Text("Upload Truck Logo", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    if (truckLogoUri != null) {
                        AsyncImage(
                            model = truckLogoUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select Logo from Device")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoEditDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun PingLocationButton(lastPingedLocation: GeoPoint?, onPing: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onPing,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Ping My Location Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                
                if (lastPingedLocation != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "📍 ${"%.4f".format(lastPingedLocation.latitude)}, ${"%.4f".format(lastPingedLocation.longitude)}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        if (lastPingedLocation != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = PrimaryRed,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Last Ping Coordinates: Lat ${"%.5f".format(lastPingedLocation.latitude)}, Long ${"%.5f".format(lastPingedLocation.longitude)}",
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun MapCard(centerPoint: GeoPoint? = null) {
    val mapView = remember { mutableStateOf<MapView?>(null) }
    
    LaunchedEffect(centerPoint) {
        centerPoint?.let {
            mapView.value?.controller?.animateTo(it)
            mapView.value?.controller?.setZoom(16.0)
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        AndroidView(
            factory = { context ->
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(14.0)
                    
                    val points = listOf(
                        GeoPoint(34.048, -118.258),
                        GeoPoint(34.076, -118.258),
                        GeoPoint(33.988, -118.472)
                    )
                    
                    points.forEachIndexed { index, point ->
                        val marker = Marker(this)
                        marker.position = point
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        marker.title = "Stop ${index + 1}"
                        overlays.add(marker)
                    }
                    controller.setCenter(points[0])
                    mapView.value = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopsSection(
    stops: List<StopLocation>,
    onAddStop: (StopLocation) -> Unit
) {
    var showAddStopDialog by remember { mutableStateOf(false) }

    var locationName by remember { mutableStateOf("") }
    var streetAddress by remember { mutableStateOf("") }
    var eventTag by remember { mutableStateOf("") }
    var timeWindow by remember { mutableStateOf("11 AM — 3 PM") }

    var timeMenuExpanded by remember { mutableStateOf(false) }
    val timeOptions = listOf(
        "11 AM — 3 PM",
        "4 PM — 6 PM",
        "7 PM — 10 PM",
        "8 AM — 12 PM",
        "12 PM — 5 PM",
        "Custom..."
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Today's Stops", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            TextButton(onClick = { showAddStopDialog = true }) {
                Text(text = "+ Add Stop", color = PrimaryRed, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        stops.forEachIndexed { index, stop ->
            StopItem(
                name = stop.locationName,
                address = stop.streetAddress,
                time = stop.timeWindow,
                tag = stop.eventTag,
                isCurrent = stop.stopStatus.equals("Here", ignoreCase = true),
                isCompleted = !stop.stopStatus.equals("Here", ignoreCase = true)
            )
            if (index < (stops.size - 1)) Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showAddStopDialog) {
        AlertDialog(
            onDismissRequest = { showAddStopDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = PrimaryRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Stop", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = locationName,
                        onValueChange = { locationName = it },
                        label = { Text("Location Name *") },
                        placeholder = { Text("e.g. Arts District Plaza") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = streetAddress,
                        onValueChange = { streetAddress = it },
                        label = { Text("Street Address *") },
                        placeholder = { Text("e.g. 800 E 3rd St, Los Angeles") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = eventTag,
                        onValueChange = { eventTag = it },
                        label = { Text("Optional Event Tag") },
                        placeholder = { Text("e.g. Food Truck Friday") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Box {
                        OutlinedTextField(
                            value = timeWindow,
                            onValueChange = { timeWindow = it },
                            label = { Text("Time Window") },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { timeMenuExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Time Window")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { timeMenuExpanded = true },
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = timeMenuExpanded,
                            onDismissRequest = { timeMenuExpanded = false }
                        ) {
                            timeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        timeWindow = option
                                        timeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (locationName.isNotBlank() && streetAddress.isNotBlank()) {
                            val newStop = StopLocation(
                                stopId = "stp-" + UUID.randomUUID().toString().take(6),
                                truckId = "trk-0001",
                                stopOrder = stops.size + 1,
                                locationName = locationName,
                                streetAddress = streetAddress,
                                latitude = 34.0522,
                                longitude = -118.2437,
                                eventTag = eventTag.ifBlank { null },
                                timeWindow = timeWindow,
                                stopStatus = "Here"
                            )
                            onAddStop(newStop)
                            locationName = ""
                            streetAddress = ""
                            eventTag = ""
                            timeWindow = "11 AM — 3 PM"
                            showAddStopDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    shape = RoundedCornerShape(12.dp),
                    enabled = locationName.isNotBlank() && streetAddress.isNotBlank()
                ) {
                    Text("Add Stop", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStopDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun StopItem(
    name: String,
    time: String,
    tag: String?,
    address: String = "",
    isCompleted: Boolean = false,
    isCurrent: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) Color(0xFFFFF1F1) else Color.White
        ),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCompleted) {
                Surface(
                    color = Color(0xFFE6FFFA),
                    shape = CircleShape,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF38A169), modifier = Modifier.size(16.dp))
                    }
                }
            } else if (isCurrent) {
                Surface(
                    color = PrimaryRed,
                    shape = CircleShape,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "3", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Surface(
                    color = Color(0xFFF5F5F5),
                    shape = CircleShape,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (address.isNotBlank()) {
                    Text(text = address, color = Color.Gray, fontSize = 12.sp)
                }
                Text(text = time, color = Color.Gray, fontSize = 12.sp)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                if (tag != null) {
                    Surface(
                        color = Color(0xFFFFFBE6),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = tag,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD69E2E)
                        )
                    }
                }
                if (isCurrent) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "HERE", color = PrimaryRed, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun OwnerMenuTab(menuItems: List<MenuItem>, viewModel: TruckViewModel) {
    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemName by remember { mutableStateOf("") }
    var itemPrice by remember { mutableStateOf("") }
    var itemDescription by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    val displayMenu = if (menuItems.isEmpty()) {
        listOf(
            MenuItem("itm-0001", "trk-0001", "Al Pastor Taco", 50.00, "Marinated pork, pineapple, cilantro, onion on house-made tortilla", "https://cdn.streeteats.com/m/alpastor.jpg"),
            MenuItem("itm-0002", "trk-0001", "Carne Asada Burrito", 85.00, "Slow-braised beef, Oaxacan cheese, served with consommé", "https://cdn.streeteats.com/m/burrito.jpg")
        )
    } else menuItems

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Menu Items (${displayMenu.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                TextButton(onClick = { showAddItemDialog = true }) {
                    Text(text = "+ Add Item", color = PrimaryRed, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(displayMenu) { item ->
            MenuCard(item)
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showAddItemDialog) {
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = PrimaryRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Menu Item & Photo", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Item Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = itemPrice,
                        onValueChange = { itemPrice = it },
                        label = { Text("Price * (e.g. 50.00)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = itemDescription,
                        onValueChange = { itemDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Direct Image Uploader picker button & thumbnail preview
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (selectedImageUri == null) "Select Photo from Device" else "Change Photo")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val priceVal = itemPrice.toDoubleOrNull() ?: 0.0
                        if (itemName.isNotBlank() && priceVal > 0) {
                            val newItem = MenuItem(
                                itemId = "itm-" + UUID.randomUUID().toString().take(6),
                                truckId = "trk-0001",
                                itemName = itemName,
                                price = priceVal,
                                description = itemDescription.ifBlank { null },
                                imageUrl = selectedImageUri?.toString()
                            )
                            
                            val imageFile = if (selectedImageUri != null) {
                                try {
                                    val inputStream = context.contentResolver.openInputStream(selectedImageUri!!)
                                    val tempFile = File.createTempFile("menu_", ".jpg", context.cacheDir)
                                    inputStream?.use { input ->
                                        tempFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    tempFile
                                } catch (e: Exception) {
                                    null
                                }
                            } else null

                            viewModel.addMenuItem(newItem, imageFile)
                            
                            itemName = ""
                            itemPrice = ""
                            itemDescription = ""
                            selectedImageUri = null
                            showAddItemDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    shape = RoundedCornerShape(12.dp),
                    enabled = itemName.isNotBlank() && itemPrice.isNotBlank()
                ) {
                    Text("Add Item", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun MenuCard(item: MenuItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFFEEEEEE))
                )
            }
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = item.itemName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = "$${"%.2f".format(item.price)}", color = PrimaryRed, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = item.description ?: "", color = Color.Gray, fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { /* TODO */ },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Remove", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun OwnerStatsTab() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(title = "Total Reviews", value = "128", color = StatPink, valueColor = PrimaryRed, modifier = Modifier.weight(1f))
                StatCard(title = "Overall Rating", value = "4.7", color = StatYellow, valueColor = Color(0xFFB7791F), modifier = Modifier.weight(1f))
            }
        }
        item {
            EngagementCard()
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatCard(title: String, value: String, color: Color, valueColor: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
            Text(text = title, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun EngagementCard() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Engagement", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            
            EngagementItem(label = "Followers", value = "241", progress = 0.6f, color = PrimaryRed)
            Spacer(modifier = Modifier.height(16.dp))
            EngagementItem(label = "Profile Views (7d)", value = "1,420", progress = 0.4f, color = Color(0xFFD69E2E))
            Spacer(modifier = Modifier.height(16.dp))
            EngagementItem(label = "Route Checks (7d)", value = "876", progress = 0.8f, color = Color(0xFF38A169))
        }
    }
}

@Composable
fun EngagementItem(label: String, value: String, progress: Float, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, color = Color.Gray, fontSize = 12.sp)
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = Color(0xFFF5F5F5)
        )
    }
}

@Composable
fun BottomNavigationBar(selectedNav: Int, onNavSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        NavigationBarItem(
            selected = selectedNav == 0,
            onClick = { onNavSelected(0) },
            icon = { Icon(Icons.Default.LocalShipping, contentDescription = null) },
            label = { Text("My Truck & Logo") },
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

@Composable
fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick
)
