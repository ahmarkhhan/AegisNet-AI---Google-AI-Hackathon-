package com.aegisnet.mobile.ui.dashboard

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegisnet.mobile.domain.agent.AgentLog
import com.aegisnet.mobile.domain.agent.LogType
import com.aegisnet.mobile.domain.agent.SocialTweet
import com.aegisnet.mobile.domain.model.CrisisAlert
import com.aegisnet.mobile.domain.model.DroneStatus
import com.aegisnet.mobile.domain.model.EventSignal
import com.aegisnet.mobile.ui.dashboard.components.TacticalEocMap
import com.aegisnet.mobile.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onMenuClick: () -> Unit,
    onAlertClick: (CrisisAlert) -> Unit,
    onReportClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tweets by viewModel.tweets.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val location = try {
                locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (e: SecurityException) {
                null
            }
            if (location != null) {
                viewModel.updateCoordinates(location.latitude, location.longitude)
            }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    var isMapFullScreen by remember { mutableStateOf(false) }
    var selectedAlertForBottomSheet by remember { mutableStateOf<CrisisAlert?>(null) }
    var selectedTrend by remember { mutableStateOf<String?>(null) }

    val trends = listOf("#AllTrends", "#RawalpindiFlood", "#MurreeBlizzard", "#EarthquakePK", "#JalsaMallRoad")

    val filteredTweets = remember(tweets, selectedTrend) {
        if (selectedTrend.isNullOrBlank() || selectedTrend == "#AllTrends") {
            tweets
        } else {
            tweets.filter {
                it.body.contains(selectedTrend!!, ignoreCase = true) ||
                it.correlatedCrisis?.contains(selectedTrend!!.replace("#", "").substring(0, 4), ignoreCase = true) == true
            }
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onSnackbarDismissed()
        }
    }

    // Modal Bottom Sheet detail summons
    if (selectedAlertForBottomSheet != null) {
        CrisisAlertDetailBottomSheet(
            alert = selectedAlertForBottomSheet!!,
            tweets = tweets,
            viewModel = viewModel,
            onDismiss = { selectedAlertForBottomSheet = null }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AegisDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.aegisnet.mobile.R.drawable.ic_aegis_logo),
                            contentDescription = "Nigehban AI Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Column {
                            Text("NIGEHBAN AI (نگہبان AI)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.5.sp)
                            Text("EOC Command Center", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                        Box(modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(AegisSuccess))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ACTIVE SCAN", color = AegisSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AegisPanel)
            )
        },
        floatingActionButton = {
            if (!isMapFullScreen) {
                FloatingActionButton(
                    onClick = onReportClick,
                    containerColor = AegisAlert,
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = "Report Incident")
                }
            }
        }
    ) { padding ->

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AegisPrimary)
            }
            return@Scaffold
        }

        if (isMapFullScreen) {
            // Immersive Full Screen Tactical Radar Screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                TacticalEocMap(
                    alerts = uiState.alerts,
                    drones = uiState.drones,
                    modifier = Modifier.fillMaxSize(),
                    onAlertClick = { alert -> selectedAlertForBottomSheet = alert }
                )

                // Close Fullscreen Floating overlay button
                IconButton(
                    onClick = { isMapFullScreen = false },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = AegisPanel.copy(alpha = 0.9f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .border(BorderStroke(1.dp, AegisPrimary), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Filled.FullscreenExit, contentDescription = "Exit Fullscreen")
                }

                // EOC System details HUD floating overlay
                Surface(
                    color = AegisPanel.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AegisSlate),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .width(220.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(AegisSuccess))
                            Text("EOC HQ RAWAPINDI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Text("ACTIVE CRISES DETECTED: ${uiState.alerts.size}", color = Color.LightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("ACTIVE DRONE FLIGHTS: ${uiState.drones.size}", color = AegisPrimary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Divider(color = AegisSlate.copy(alpha = 0.5f), thickness = 0.5.dp)
                        Text("Tap any incident beacon to summon high fidelity drone geodetics and AI scrapers.", color = Color.Gray, fontSize = 8.sp, lineHeight = 11.sp)
                    }
                }
            }
        } else {
            // Grid EOC Standard Dashboard
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // --- Atmospheric Weather Telemetry Widget ---
                uiState.weather?.let { weather ->
                    item {
                        LiveWeatherWidget(weather = weather)
                    }
                }

                // --- Interactive Tactical Radar Map ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(title = "Tactical EOC Area Map", icon = Icons.Filled.MyLocation, count = uiState.alerts.size)
                        
                        // Toggle Fullscreen trigger
                        TextButton(
                            onClick = { isMapFullScreen = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = AegisPrimary)
                        ) {
                            Icon(Icons.Filled.Fullscreen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("FULL SCREEN MONITOR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item {
                    TacticalEocMap(
                        alerts = uiState.alerts,
                        drones = uiState.drones,
                        modifier = Modifier.fillMaxWidth(),
                        onAlertClick = { alert -> selectedAlertForBottomSheet = alert }
                    )
                }

                // --- Drone Fleet Status ---
                if (uiState.drones.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Drone Fleet Status", icon = Icons.Filled.Air, count = uiState.drones.size)
                    }
                    items(uiState.drones, key = { "drone-${it.droneId}" }) { drone ->
                        DroneStatusCard(drone = drone)
                    }
                }

                // --- Active Crisis Alerts ---
                item {
                    SectionHeader(title = "Active Crisis Alerts", icon = Icons.Filled.Notifications, count = uiState.alerts.size)
                }
                if (uiState.alerts.isEmpty()) {
                    item { EmptyState("No active alerts. System nominal.") }
                } else {
                    items(uiState.alerts, key = { "alert-${it.id}" }) { alert ->
                        AlertCard(alert = alert, onClick = { selectedAlertForBottomSheet = alert })
                    }
                }

                // --- AI Filtered Live Social Telemetry Feed & Filters ---
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader(title = "AI-Filtered Social Telemetry", icon = Icons.Filled.Share, count = filteredTweets.size)
                        
                        // Trends filter chips horizontal stack
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(trends) { trend ->
                                val selected = selectedTrend == trend || (trend == "#AllTrends" && selectedTrend == null)
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedTrend = if (trend == "#AllTrends") null else trend },
                                    label = { Text(trend, fontSize = 9.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AegisPrimary.copy(alpha = 0.15f),
                                        selectedLabelColor = AegisPrimary,
                                        containerColor = AegisPanel,
                                        labelColor = Color.LightGray
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (selected) AegisPrimary else AegisSlate.copy(alpha = 0.4f),
                                        selectedBorderColor = AegisPrimary
                                    )
                                )
                            }
                        }
                    }
                }
                if (filteredTweets.isEmpty()) {
                    item { EmptyState("No panic signals matching this hashtag filter.") }
                } else {
                    items(filteredTweets, key = { "tweet-${it.handle}-${it.timeAgo}" }) { tweet ->
                        SocialTweetCard(tweet = tweet)
                    }
                }

                // --- Signal Ingestion Feed ---
                item {
                    SectionHeader(title = "Signal Ingestion Feed", icon = Icons.Filled.Info, count = uiState.signals.size)
                }
                items(uiState.signals, key = { "signal-${it.id}" }) { signal ->
                    SignalCard(signal = signal)
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}

// Haversine calculator
private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Earth radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

// Crisis detailed specs standard bottom sheet in compose
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrisisAlertDetailBottomSheet(
    alert: CrisisAlert,
    tweets: List<SocialTweet>,
    viewModel: DashboardViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Default EOC HQ Station geodetics
    val baseLat = 33.6844
    val baseLng = 73.0479
    val distance = if (alert.epicenterLat != null && alert.epicenterLng != null) {
        calculateDistanceKm(baseLat, baseLng, alert.epicenterLat, alert.epicenterLng)
    } else 0.0
    val droneEtaMinutes = (distance / 50.0) * 60.0 // drone speed ~ 50kmh

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AegisDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AegisSlate) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp, start = 18.dp, end = 18.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Central Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ALERT STATUS: ACTIVE DISPATCH",
                        color = if (alert.severity == "CRITICAL") AegisAlert else AegisWarn,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = alert.title.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                
                Surface(
                    color = if (alert.severity == "CRITICAL") AegisAlert.copy(alpha = 0.2f) else AegisWarn.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (alert.severity == "CRITICAL") AegisAlert else AegisWarn)
                ) {
                    Text(
                        text = alert.severity,
                        color = if (alert.severity == "CRITICAL") AegisAlert else AegisWarn,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // 2. Geodetic Details
            Surface(
                color = AegisPanel,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AegisSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("DISTANCE FROM HQ", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (distance > 0.0) "%.2f KM".format(distance) else "HQ Center Grid",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("AERIAL RECON ETA", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (distance > 0.0) "%.1f MINS".format(droneEtaMinutes) else "Immediate",
                            color = AegisSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 3. Media Captures Card
            Text(
                "TACTICAL MEDIA MONITOR FEED",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )

            val photoBitmap = remember(alert.photoPath) {
                if (alert.photoPath != null) {
                    try {
                        val file = File(alert.photoPath)
                        if (file.exists()) {
                            BitmapFactory.decodeFile(file.absolutePath)
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                } else null
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Picture box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(BorderStroke(1.dp, AegisSlate)),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoBitmap != null) {
                        Image(
                            bitmap = photoBitmap.asImageBitmap(),
                            contentDescription = "Active Recon Capture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // High fidelity cyber EOC camera simulation overlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Grid lines
                            val lines = 4
                            for (i in 1..lines) {
                                drawLine(
                                    color = AegisSlate.copy(alpha = 0.15f),
                                    start = Offset(0f, size.height * (i / lines.toFloat())),
                                    end = Offset(size.width, size.height * (i / lines.toFloat())),
                                    strokeWidth = 1f
                                )
                                drawLine(
                                    color = AegisSlate.copy(alpha = 0.15f),
                                    start = Offset(size.width * (i / lines.toFloat()), 0f),
                                    end = Offset(size.width * (i / lines.toFloat()), size.height),
                                    strokeWidth = 1f
                                )
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                            Text("AERIAL RECON PHOTO", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("System Active", color = AegisSuccess, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Simulated Drone Video Player Overlay
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(BorderStroke(1.dp, AegisSlate)),
                    contentAlignment = Alignment.Center
                ) {
                    // Simulated timer counting down or loop progress
                    var droneVideoSeconds by remember { mutableStateOf(0) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(1000)
                            droneVideoSeconds = (droneVideoSeconds + 1) % 121 // loops 120s limit
                        }
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Scanlines sweep simulation
                        val scanlineY = (System.currentTimeMillis() % 4000) / 4000f * size.height
                        drawLine(
                            color = AegisPrimary.copy(alpha = 0.3f),
                            start = Offset(0f, scanlineY),
                            end = Offset(size.width, scanlineY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(AegisAlert))
                            Text("LIVE FEED TRK", color = AegisPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = "%02d:%02d / 02:00".format(droneVideoSeconds / 60, droneVideoSeconds % 60),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        
                        Text("SPD: 48.5km/h | ALT: 120m", color = Color.LightGray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        LinearProgressIndicator(
                            progress = droneVideoSeconds / 120f,
                            color = AegisPrimary,
                            trackColor = AegisSlate,
                            modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp))
                        )
                    }
                }
            }

            // 4. Incident details descriptions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AegisPanel),
                border = BorderStroke(1.dp, AegisSlate)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("CRISIS REPORT DEBRIEF", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = alert.description,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }

            // 5. Seismic Activity detailed layout
            if (alert.type == "SEISMIC_ACTIVITY") {
                Text(
                    "RICHTER CRISIS GEODETICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AegisPanel),
                    border = BorderStroke(1.dp, AegisAlert.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("RICHTER MAGNITUDE", color = Color.LightGray, fontSize = 11.sp)
                            Text(
                                "${"%.1f".format(alert.seismicMagnitude ?: 5.0)} M_w",
                                color = AegisAlert,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("FOCAL DEPTH EPICENTER", color = Color.LightGray, fontSize = 11.sp)
                            Text(
                                "${"%.1f".format(alert.seismicDepth ?: 10.0)} km",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Divider(color = AegisSlate.copy(alpha = 0.5f), thickness = 0.5.dp)

                        val aftershockWarning = alert.seismicTremors ?: false
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (aftershockWarning) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (aftershockWarning) AegisAlert else AegisSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = if (aftershockWarning) "ACTIVE AFTERSHOCK WARNING" else "SECONDARY TREMORS STABLE",
                                    color = if (aftershockWarning) AegisAlert else AegisSuccess,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = if (aftershockWarning) {
                                        "EOC seismologists advice immediate suspension of structure entry. Higher secondary tremors probable."
                                    } else {
                                        "No major secondary aftershocks registered within EOC telemetry bounds."
                                    },
                                    color = Color.LightGray,
                                    fontSize = 9.sp,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // 6. Political rally implicants
            if (alert.politicalParty != null) {
                Text(
                    "POLITICAL STRIKE CORRIDORS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AegisPanel),
                    border = BorderStroke(1.dp, AegisAlert.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ORGANIZING PARTY", color = Color.LightGray, fontSize = 11.sp)
                            Text(
                                alert.politicalParty,
                                color = AegisAlert,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Divider(color = AegisSlate.copy(alpha = 0.5f), thickness = 0.5.dp)

                        Text(
                            text = alert.politicalImplications ?: "",
                            color = Color.White,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 7. NDMA Warnings & Rivers statuses
            val ndmaWarning = if (alert.epicenterLat != null && alert.epicenterLng != null) {
                viewModel.getNdmaFloodTelemetry(alert.epicenterLat, alert.epicenterLng, alert.placeName)
            } else ""

            if (ndmaWarning.isNotBlank()) {
                Text(
                    "NDMA FLOOD TELEMETRY",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    color = AegisPrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AegisPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Water, contentDescription = null, tint = AegisPrimary, modifier = Modifier.size(20.dp))
                        Column {
                            Text("RIVER CORRIDOR DISCHARGE HIGH", color = AegisPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(ndmaWarning, color = Color.White, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }

            // 8. Correlated Social Tweets
            val correlatedTweets = remember(tweets, alert) {
                tweets.filter {
                    it.correlatedCrisis?.contains(alert.type.replace("Emergency: ", "").substring(0, 4), ignoreCase = true) == true ||
                    (alert.placeName != null && it.body.contains(alert.placeName.split(" ").first(), ignoreCase = true))
                }
            }

            Text(
                "COOPERATIVE AI VERIFIED SIGNALS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )

            if (correlatedTweets.isEmpty()) {
                EmptyState("No matching social signal streams found in this EOC sector.")
            } else {
                correlatedTweets.forEach { tweet ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AegisPanel),
                        border = BorderStroke(1.dp, AegisSlate.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(tweet.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("@${tweet.handle}", color = Color.Gray, fontSize = 10.sp)
                                }
                                Surface(
                                    color = AegisAlert.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "VETTED MATCH",
                                        color = AegisAlert,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Text(tweet.body, color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp)
                            Text(
                                text = "AI vetting logic: ${tweet.correlationReason ?: "matches active crisis sector telemetry"}",
                                color = AegisSuccess,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveWeatherWidget(weather: com.aegisnet.mobile.domain.model.WeatherTelemetry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AegisPanel),
        border = BorderStroke(1.dp, AegisSlate)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("📡  ATMOSPHERIC TELEMETRY", color = AegisPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Box(modifier = Modifier.background(AegisPrimary.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(weather.condition.uppercase(), color = AegisPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WeatherMetricItem("TEMP", "${"%.1f".format(weather.temperature)}°C", Icons.Default.Thermostat)
                WeatherMetricItem("WIND", "${"%.1f".format(weather.windSpeed)} km/h", Icons.Default.Air)
                WeatherMetricItem("PRECIP", "${"%.1f".format(weather.precipitation)} mm", Icons.Default.WaterDrop)
                WeatherMetricItem("HUMIDITY", "${weather.humidity}%", Icons.Default.Cloud)
            }
        }
    }
}

@Composable
private fun WeatherMetricItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
        Column {
            Text(label, color = Color(0xFF64748B), fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
            Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AgentTracePanel(logs: List<AgentLog>) {
    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = BorderStroke(1.dp, AegisSlate.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("🧠  OFFLINE AGENT ORCHESTRATION TRACE", color = AegisPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { log ->
                    val color = when (log.type) {
                        LogType.INGESTION -> AegisPrimary
                        LogType.REASONING -> AegisWarn
                        LogType.ACTION -> AegisSuccess
                        LogType.SYSTEM -> Color(0xFF94A3B8)
                    }
                    Text(
                        text = "[${log.timestamp}] [${log.type}] ${log.message}",
                        color = color,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SimulationPanel(onWeather: () -> Unit, onSocial: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AegisPanel),
        border = BorderStroke(1.dp, AegisSlate)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🎮  DEMO SIMULATION", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onWeather,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, AegisPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisPrimary)
                ) { Text("Inject Weather", fontSize = 11.sp) }
                OutlinedButton(
                    onClick = onSocial,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, AegisWarn),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisWarn)
                ) { Text("Inject Panic", fontSize = 11.sp) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
        Text(title.uppercase(), color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Badge(containerColor = AegisSlate, contentColor = Color.White) { Text("$count") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertCard(alert: CrisisAlert, onClick: () -> Unit) {
    val riskColor = when {
        alert.casualtyRiskScore >= 80 -> AegisAlert
        alert.casualtyRiskScore >= 50 -> AegisWarn
        else -> AegisSuccess
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AegisPanel),
        border = BorderStroke(1.dp, riskColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(alert.type.replace("_", " "), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = riskColor.copy(alpha = 0.15f), labelColor = riskColor)
                )
                Text(alert.createdAt, color = Color(0xFF94A3B8), fontSize = 10.sp)
            }
            Text(alert.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(alert.description, color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 18.sp)

            if (alert.placeName != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.PinDrop, contentDescription = null, tint = AegisPrimary, modifier = Modifier.size(14.dp))
                    Text(alert.placeName, color = AegisPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (alert.politicalParty != null) {
                Surface(
                    color = AegisAlert.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, AegisAlert.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("📢 Political Jalsa Implication (${alert.politicalParty})", color = AegisAlert, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(alert.politicalImplications ?: "", color = Color.White, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RiskIndicator("CASUALTY RISK", alert.casualtyRiskScore, riskColor)
                RiskIndicator("ESCALATION", alert.escalationProbability, riskColor)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("View Full EOC Command Details", color = AegisPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = AegisPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun RiskIndicator(label: String, value: Int, color: Color) {
    Column {
        Text(label, color = Color(0xFF94A3B8), fontSize = 9.sp, letterSpacing = 0.5.sp)
        LinearProgressIndicator(
            progress = value / 100f,
            modifier = Modifier.width(100.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = AegisSlate
        )
        Text("$value%", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SocialTweetCard(tweet: SocialTweet) {
    val sentimentColor = when (tweet.sentiment) {
        "PANIC" -> AegisAlert
        "SAFE" -> AegisSuccess
        else -> AegisPrimary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AegisPanel),
        border = BorderStroke(1.dp, AegisSlate)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(sentimentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tweet.sentiment == "PANIC") "🔥" else "✓",
                            fontSize = 11.sp
                        )
                    }
                    Column {
                        Text(tweet.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("@${tweet.handle}", color = Color.Gray, fontSize = 10.sp)
                    }
                }
                Surface(
                    color = sentimentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (tweet.sentiment == "PANIC") "AI-VERIFIED PANIC" else "AI-FILTERED NOMINAL",
                        color = sentimentColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Text(tweet.body, color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = AegisSuccess,
                        modifier = Modifier.size(12.dp)
                    )
                    Text("Vetted by SocialSignal-Agent", color = AegisSuccess, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(tweet.timeAgo, color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SignalCard(signal: EventSignal) {
    val accentColor = when (signal.source) {
        "SUPARCO_WEATHER" -> AegisPrimary
        "SOCIAL_X" -> AegisWarn
        else -> AegisSuccess
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(AegisPanel)
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(70.dp)
                .background(accentColor)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(signal.source, color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(signal.timestamp, color = Color(0xFF94A3B8), fontSize = 10.sp)
            }
            Text(signal.rawPayload, color = Color(0xFFCBD5E1), fontSize = 12.sp, maxLines = 2)
            Text("Confidence: ${"%.2f".format(signal.confidence)}", color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DroneStatusCard(drone: DroneStatus) {
    val statusColor = when (drone.status) {
        "SCANNING" -> AegisSuccess
        "IN_TRANSIT" -> AegisPrimary
        else -> Color(0xFF94A3B8)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AegisPanel),
        border = BorderStroke(1.dp, AegisSlate)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(drone.droneId, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("🛖  ${drone.strandedHumansDetected} humans · 🚗 ${drone.strandedVehiclesDetected} vehicles detected", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Badge(containerColor = statusColor.copy(alpha = 0.2f), contentColor = statusColor) {
                    Text(drone.status, fontSize = 9.sp)
                }
                Text("🔋 ${drone.batteryLevel}%", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, color = Color(0xFF64748B), fontSize = 13.sp)
    }
}
