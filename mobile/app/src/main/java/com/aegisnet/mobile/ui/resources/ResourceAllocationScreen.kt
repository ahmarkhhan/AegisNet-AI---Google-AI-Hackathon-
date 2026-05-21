package com.aegisnet.mobile.ui.resources

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisnet.mobile.domain.model.CrisisAlert
import com.aegisnet.mobile.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ResourceAsset(
    val name: String,
    val total: Int,
    val baseInUse: Int,
    val icon: String
)

data class DepartmentRoster(
    val departmentName: String,
    val color: Color,
    val assets: List<ResourceAsset>
)

data class EocDepot(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val capacity: Int,
    var currentResources: Int // Available vehicles/rigs
)

fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Earth's radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceAllocationScreen(
    alerts: List<CrisisAlert>,
    onBack: () -> Unit
) {
    var selectedRegion by remember { mutableStateOf("Lahore") }
    val regions = listOf("Lahore", "Rawalpindi", "Murree", "Islamabad")

    // State for manual "Force Dispatch" simulation
    val forceDispatchedMap = remember { mutableStateMapOf<Long, Boolean>() }
    val dispatchedTimerMap = remember { mutableStateMapOf<Long, Int>() }
    val expandedAllocationBoard = remember { mutableStateMapOf<Long, Boolean>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Regional Depot Locations Database
    val regionalDepots = remember {
        listOf(
            EocDepot("Lahore Central Emergency Depot A", 31.5204, 74.3587, 5, 0), // CAPACITY EXCEEDED SIMULATOR!
            EocDepot("Lahore West Auxiliary Depot B", 31.5587, 74.3024, 8, 4),
            EocDepot("Rawalpindi Saddar Emergency Depot A", 33.5984, 73.0441, 6, 0), // CAPACITY EXCEEDED SIMULATOR!
            EocDepot("Rawalpindi West Station B", 33.6110, 73.0180, 10, 8),
            EocDepot("Murree Mall Road Sector A", 33.9070, 73.3943, 4, 0), // CAPACITY EXCEEDED SIMULATOR!
            EocDepot("Murree Expressway Station B", 33.8840, 73.4150, 6, 3),
            EocDepot("Islamabad Blue Area Annex A", 33.7184, 73.0641, 10, 0), // CAPACITY EXCEEDED SIMULATOR!
            EocDepot("Islamabad Sector I-9 Station B", 33.6625, 73.0515, 12, 9)
        )
    }

    // Background response timer ticker for dispatched alerts
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            dispatchedTimerMap.keys.forEach { id ->
                if (forceDispatchedMap[id] == true) {
                    dispatchedTimerMap[id] = (dispatchedTimerMap[id] ?: 0) + 1
                }
            }
        }
    }

    // Filter alerts belonging to selected region
    val regionalAlerts = remember(alerts, selectedRegion) {
        alerts.filter {
            it.zone.contains(selectedRegion, ignoreCase = true) ||
            it.title.contains(selectedRegion, ignoreCase = true)
        }
    }

    // Determine the dynamic department assets based on region
    val rosters = remember(selectedRegion, regionalAlerts, forceDispatchedMap.size) {
        val hasFlood = regionalAlerts.any { it.title.contains("Flood", ignoreCase = true) || it.title.contains("Rain", ignoreCase = true) }
        val hasSeismic = regionalAlerts.any { it.title.contains("Earthquake", ignoreCase = true) || it.title.contains("Seismic", ignoreCase = true) }
        val activeDispatches = regionalAlerts.count { forceDispatchedMap[it.id] == true }

        when (selectedRegion) {
            "Lahore" -> listOf(
                DepartmentRoster(
                    departmentName = "Rescue 1122 Emergency Services",
                    color = AegisSuccess,
                    assets = listOf(
                        ResourceAsset("Rescue Inflatable Boats", 12, 2 + (if (hasFlood) 6 else 0) + (activeDispatches * 2), "🛶"),
                        ResourceAsset("Paramedic Ambulances", 30, 8 + (if (hasSeismic) 10 else 0) + (activeDispatches * 3), "🚑")
                    )
                ),
                DepartmentRoster(
                    departmentName = "WASA Drainage & Engineering",
                    color = AegisPrimary,
                    assets = listOf(
                        ResourceAsset("Dewatering Heavy Pumps", 25, 4 + (if (hasFlood) 15 else 0) + (activeDispatches * 4), "🌀"),
                        ResourceAsset("Hydraulic Excavators", 10, 2 + (if (hasSeismic) 5 else 0) + (activeDispatches * 1), "🚜")
                    )
                ),
                DepartmentRoster(
                    departmentName = "SWAT & Tactical Police Command",
                    color = AegisWarn,
                    assets = listOf(
                        ResourceAsset("SWAT Rapid Response Squads", 8, 2 + (activeDispatches * 2), "🛡️"),
                        ResourceAsset("Highway Patrol Cars", 40, 12 + (if (hasFlood) 8 else 0) + (activeDispatches * 4), "🚔")
                    )
                )
            )
            "Rawalpindi" -> listOf(
                DepartmentRoster(
                    departmentName = "Rescue 1122 Emergency Services",
                    color = AegisSuccess,
                    assets = listOf(
                        ResourceAsset("Lai Nullah Rescue Boats", 15, 4 + (if (hasFlood) 9 else 0) + (activeDispatches * 2), "🛶"),
                        ResourceAsset("Rapid Response Ambulances", 20, 6 + (activeDispatches * 3), "🚑")
                    )
                ),
                DepartmentRoster(
                    departmentName = "WASA Drainage & Engineering",
                    color = AegisPrimary,
                    assets = listOf(
                        ResourceAsset("Dewatering Siphon Engines", 20, 8 + (if (hasFlood) 10 else 0) + (activeDispatches * 3), "🌀"),
                        ResourceAsset("Debris Shovel Crawlers", 8, 3 + (if (hasSeismic) 4 else 0) + (activeDispatches * 1), "🚜")
                    )
                ),
                DepartmentRoster(
                    departmentName = "SWAT & Tactical Police Command",
                    color = AegisWarn,
                    assets = listOf(
                        ResourceAsset("SWAT Anti-Riot Platoons", 6, 1 + (activeDispatches * 1), "🛡️"),
                        ResourceAsset("Sector Ward Patrol Units", 25, 10 + (activeDispatches * 3), "🚔")
                    )
                )
            )
            "Murree" -> listOf(
                DepartmentRoster(
                    departmentName = "Rescue 1122 Emergency Services",
                    color = AegisSuccess,
                    assets = listOf(
                        ResourceAsset("Heavy Snow-Plow Blades", 15, 6 + (if (hasFlood || hasSeismic) 5 else 0) + (activeDispatches * 3), "🚜"),
                        ResourceAsset("Mountain Ambulance Crawlers", 10, 3 + (activeDispatches * 2), "🚑")
                    )
                ),
                DepartmentRoster(
                    departmentName = "Forestry & High-Pressure Cleaners",
                    color = AegisPrimary,
                    assets = listOf(
                        ResourceAsset("Highland Debris Flushers", 8, 2 + (if (hasFlood) 4 else 0) + (activeDispatches * 1), "🌀"),
                        ResourceAsset("Tree Clearing Saw Rigs", 12, 4 + (activeDispatches * 2), "🌲")
                    )
                ),
                DepartmentRoster(
                    departmentName = "Highland Rangers & SWAT",
                    color = AegisWarn,
                    assets = listOf(
                        ResourceAsset("Glacier Rescue Rangers", 10, 4 + (activeDispatches * 2), "🛡️"),
                        ResourceAsset("4x4 Blizzard Command Jeeps", 20, 8 + (activeDispatches * 3), "🚔")
                    )
                )
            )
            // Islamabad
            else -> listOf(
                DepartmentRoster(
                    departmentName = "CDA & Fire Fighting Unit",
                    color = AegisSuccess,
                    assets = listOf(
                        ResourceAsset("High-Rise Fire Engines", 12, 2 + (activeDispatches * 2), "🚒"),
                        ResourceAsset("Disaster Recovery Rigs", 8, 1 + (if (hasSeismic) 4 else 0) + (activeDispatches * 1), "🚛")
                    )
                ),
                DepartmentRoster(
                    departmentName = "Capital Dewatering Command",
                    color = AegisPrimary,
                    assets = listOf(
                        ResourceAsset("Urban Submersible Pumps", 15, 3 + (if (hasFlood) 8 else 0) + (activeDispatches * 3), "🌀"),
                        ResourceAsset("Heavy Cranes & Backhoes", 6, 1 + (if (hasSeismic) 3 else 0) + (activeDispatches * 1), "🚜")
                    )
                ),
                DepartmentRoster(
                    departmentName = "Capital Tactical SWAT Command",
                    color = AegisWarn,
                    assets = listOf(
                        ResourceAsset("Islamabad SWAT Elite Units", 10, 3 + (activeDispatches * 2), "🛡️"),
                        ResourceAsset("Diplomatic Enclave Interceptors", 30, 10 + (activeDispatches * 2), "🚔")
                    )
                )
            )
        }
    }

    // Dynamic calculations for Active Region KPIs
    val totalAssets = rosters.sumOf { roster -> roster.assets.sumOf { it.total } }
    val totalInUse = rosters.sumOf { roster -> roster.assets.sumOf { minOf(it.total, it.baseInUse) } }
    val totalAvailable = totalAssets - totalInUse

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = AegisAlert
                        )
                        Column {
                            Text("TACTICAL RESOURCE HUB", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Nigehban AI (نگہبان AI) EOC Operations", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AegisPanel)
            )
        },
        containerColor = AegisDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Region Selection Tabs
            ScrollableTabRow(
                selectedTabIndex = regions.indexOf(selectedRegion),
                containerColor = AegisPanel,
                contentColor = AegisSuccess,
                edgePadding = 12.dp,
                divider = { Divider(color = AegisSlate.copy(alpha = 0.4f)) }
            ) {
                regions.forEachIndexed { index, region ->
                    Tab(
                        selected = selectedRegion == region,
                        onClick = { selectedRegion = region },
                        text = {
                            Text(
                                region.uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (selectedRegion == region) AegisSuccess else Color.Gray
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tactical KPI Grid for Selected Region
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "$selectedRegion Region Sector HUD".uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KpiMetricCard(
                                label = "Total Rigs",
                                value = totalAssets.toString(),
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricCard(
                                label = "Active In Field",
                                value = totalInUse.toString(),
                                color = AegisAlert,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricCard(
                                label = "Standby Ready",
                                value = totalAvailable.toString(),
                                color = AegisSuccess,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Department Matrix Cards
                item {
                    Text(
                        "DEPARTMENT SECURE ROSTER MATRIX",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(rosters, key = { roster: DepartmentRoster -> roster.departmentName }) { roster ->
                    DepartmentRosterCard(roster)
                }

                // Manual Force Dispatch Command Zone
                item {
                    Text(
                        "EOC FIELD FORCE DISPATCH HUB (NEAREST DEPOT PRIORITY)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (regionalAlerts.isEmpty()) {
                    item {
                        Surface(
                            color = AegisPanel,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AegisSlate),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier.padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AegisSuccess, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Region Sector Stable", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("No active reports requiring force dispatch sweeps.", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                } else {
                    items(regionalAlerts, key = { alert: CrisisAlert -> alert.id }) { alert ->
                        val isDispatched = forceDispatchedMap[alert.id] == true
                        val tickingTimer = dispatchedTimerMap[alert.id] ?: 0
                        val isBoardExpanded = expandedAllocationBoard[alert.id] == true

                        // Resolve Coordinates for Haversine Calculations
                        val crisisLat = alert.epicenterLat ?: when (selectedRegion) {
                            "Lahore" -> 31.5204
                            "Rawalpindi" -> 33.5984
                            "Murree" -> 33.9070
                            else -> 33.6844
                        }
                        val crisisLng = alert.epicenterLng ?: when (selectedRegion) {
                            "Lahore" -> 74.3587
                            "Rawalpindi" -> 73.0441
                            "Murree" -> 73.3943
                            else -> 73.0479
                        }

                        // Calculate and Sort depots by distance from crisis epicenter
                        val sortedDepots = remember(selectedRegion, alert) {
                            regionalDepots
                                .filter { it.name.contains(selectedRegion, ignoreCase = true) }
                                .map { depot ->
                                    val dist = calculateHaversineDistance(depot.latitude, depot.longitude, crisisLat, crisisLng)
                                    depot to dist
                                }
                                .sortedBy { it.second }
                        }

                        Surface(
                            color = AegisPanel,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isDispatched) AegisSuccess.copy(alpha = 0.5f) else AegisSlate),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                        Text(alert.zone, fontSize = 10.sp, color = Color.Gray)
                                    }

                                    Surface(
                                        color = if (isDispatched) AegisSuccess.copy(alpha = 0.15f) else AegisAlert.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, if (isDispatched) AegisSuccess else AegisAlert)
                                    ) {
                                        Text(
                                            text = if (isDispatched) "EN ROUTE" else "PENDING DISPATCH",
                                            color = if (isDispatched) AegisSuccess else AegisAlert,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassEmpty,
                                            contentDescription = null,
                                            tint = if (isDispatched) AegisSuccess else Color.Gray,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (isDispatched) {
                                                val h = tickingTimer / 3600
                                                val m = (tickingTimer % 3600) / 60
                                                val s = tickingTimer % 60
                                                "Active Timer: ${"%02d:%02d:%02d".format(h, m, s)}"
                                            } else {
                                                "Response Timer: Paused"
                                            },
                                            color = if (isDispatched) AegisSuccess else Color.Gray,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (!isDispatched) {
                                                expandedAllocationBoard[alert.id] = !isBoardExpanded
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDispatched) Color(0xFF1E293B) else AegisAlert,
                                            contentColor = if (isDispatched) Color.Gray else Color.Black
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        enabled = !isDispatched,
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isBoardExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(if (isBoardExpanded) "Close Board" else "Force Dispatch", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Interactive Nearest Depot Dispatcher Panel
                                AnimatedVisibility(
                                    visible = isBoardExpanded && !isDispatched,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp)
                                            .background(AegisDark, RoundedCornerShape(8.dp))
                                            .border(BorderStroke(1.dp, AegisSlate), RoundedCornerShape(8.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "GEMINI AI RESOURCE ADVISOR",
                                            color = AegisPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )

                                        // Real-time Gemini AI Resource Recommendation
                                        val geminiAgent = remember { com.aegisnet.mobile.domain.agent.GeminiAgentService() }
                                        var aiRecommendation by remember { mutableStateOf<com.aegisnet.mobile.domain.agent.GeminiAgentService.ResourceRecommendation?>(null) }
                                        var isAiThinking by remember { mutableStateOf(false) }

                                        LaunchedEffect(alert.id, isBoardExpanded) {
                                            if (isBoardExpanded && aiRecommendation == null) {
                                                isAiThinking = true
                                                val depotsList = sortedDepots.map { "${it.first.name} (${it.first.capacity - it.first.currentResources}/${it.first.capacity} available)" }
                                                aiRecommendation = geminiAgent.recommendResourceAllocation(
                                                    crisisType = alert.title,
                                                    severity = alert.severity,
                                                    location = alert.zone,
                                                    availableDepots = depotsList
                                                )
                                                isAiThinking = false
                                            }
                                        }

                                        if (isAiThinking) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                CircularProgressIndicator(color = AegisPrimary, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                                                Text("AI analyzing optimal dispatch strategy...", color = Color.Gray, fontSize = 10.sp)
                                            }
                                        }

                                        aiRecommendation?.let { rec ->
                                            Surface(
                                                color = AegisPrimary.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, AegisPrimary.copy(alpha = 0.3f)),
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AegisPrimary, modifier = Modifier.size(12.dp))
                                                        Text("AI RECOMMENDED STRATEGY", color = AegisPrimary, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                    }
                                                    Text(rec.strategy, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                    Text("Units: ${rec.recommendedUnits}", color = Color.LightGray, fontSize = 10.sp)
                                                    Divider(color = AegisPrimary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 2.dp))
                                                    Text("Reasoning: ${rec.reasoning}", color = Color.Gray, fontSize = 9.sp, lineHeight = 12.sp)
                                                }
                                            }
                                        }

                                        // Sorting nearest stations
                                        sortedDepots.forEachIndexed { index, (depot, distance) ->
                                            val isPrimary = index == 0
                                            val hasCapacity = depot.currentResources < depot.capacity

                                            Surface(
                                                color = AegisPanel,
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, if (isPrimary && !hasCapacity) AegisAlert.copy(alpha = 0.5f) else AegisSlate.copy(alpha = 0.4f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.LocationOn,
                                                            contentDescription = null,
                                                            tint = if (isPrimary) AegisSuccess else Color.Gray,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Column {
                                                            Text(
                                                                depot.name,
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp
                                                            )
                                                            Text(
                                                                "Proximity: ${"%.2f".format(distance)} km away",
                                                                color = Color.LightGray,
                                                                fontSize = 9.sp
                                                            )
                                                        }
                                                    }

                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            if (hasCapacity) "${depot.capacity - depot.currentResources}/${depot.capacity} Available" else "0/${depot.capacity} Available",
                                                            color = if (hasCapacity) AegisSuccess else AegisAlert,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        if (isPrimary) {
                                                            Text(
                                                                "PRIMARY STATION",
                                                                color = AegisPrimary,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.ExtraBold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Capacity Limit Alarm Warning Overlay
                                        val primaryDepot = sortedDepots.firstOrNull()?.first
                                        if (primaryDepot != null && primaryDepot.currentResources >= primaryDepot.capacity) {
                                            val backupDepot = sortedDepots.getOrNull(1)?.first
                                            Surface(
                                                color = AegisAlert.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, AegisAlert.copy(alpha = 0.3f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Warning,
                                                        contentDescription = null,
                                                        tint = AegisAlert,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        "Primary Depot capacity exceeded! Nigehban AI has automatically rerouted and dispatched backup reinforcements from the next closest station: ${backupDepot?.name ?: "Auxiliary Station B"}",
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        lineHeight = 14.sp
                                                    )
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                forceDispatchedMap[alert.id] = true
                                                dispatchedTimerMap[alert.id] = 0
                                                expandedAllocationBoard[alert.id] = false
                                                scope.launch {
                                                    val primaryName = sortedDepots.firstOrNull()?.first?.name ?: "Primary Depot"
                                                    val backupName = sortedDepots.getOrNull(1)?.first?.name ?: "Backup Depot"
                                                    val msg = if (primaryDepot != null && primaryDepot.currentResources >= primaryDepot.capacity) {
                                                        "[BACKUP DEPOT ACTIVE] Primary Depot full! Reinforcements dispatched from $backupName ✓"
                                                    } else {
                                                        "[TACTICAL ALLOCATION] SWAT & Rescue dispatched from closest station: $primaryName ✓"
                                                    }
                                                    snackbarHostState.showSnackbar(msg)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AegisSuccess, contentColor = Color.Black),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(38.dp)
                                        ) {
                                            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Confirm Dispatch Sweep", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun KpiMetricCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = AegisPanel,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier.height(72.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label.uppercase(), fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DepartmentRosterCard(roster: DepartmentRoster) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AegisPanel),
        border = BorderStroke(1.dp, AegisSlate),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Department Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(roster.color)
                )
                Text(
                    roster.departmentName.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Roster details
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                roster.assets.forEach { asset ->
                    val progressFraction = if (asset.total > 0) asset.baseInUse.toFloat() / asset.total.toFloat() else 0f
                    val clampedProgress = progressFraction.coerceIn(0f, 1f)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(asset.icon, fontSize = 14.sp)
                                Text(asset.name, fontSize = 12.sp, color = Color.LightGray)
                            }
                            Text(
                                "${asset.baseInUse}/${asset.total} Dispatched",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (clampedProgress > 0.8f) AegisAlert else Color.White
                            )
                        }

                        // Progress Meter
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(AegisDark)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(clampedProgress)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (clampedProgress > 0.8f) AegisAlert else roster.color
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
