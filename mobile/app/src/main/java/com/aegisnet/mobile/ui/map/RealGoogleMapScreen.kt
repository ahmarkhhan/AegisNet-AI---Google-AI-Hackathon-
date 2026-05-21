package com.aegisnet.mobile.ui.map

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegisnet.mobile.domain.agent.EocAgentOrchestrator
import com.aegisnet.mobile.domain.model.CrisisAlert
import com.aegisnet.mobile.ui.dashboard.DashboardViewModel
import com.aegisnet.mobile.ui.dashboard.components.TacticalEocMap
import com.aegisnet.mobile.ui.theme.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealGoogleMapScreen(
    orchestrator: EocAgentOrchestrator,
    isOffline: Boolean = false,
    onMenuClick: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val dashboardUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val alerts = dashboardUiState.alerts

    var isSatellite by remember { mutableStateOf(false) }
    var mapTypeSelected by remember { mutableStateOf(MapType.NORMAL) }
    var isTrafficEnabled by remember { mutableStateOf(true) }
    var showRivers by remember { mutableStateOf(true) }
    var showPopulationCircles by remember { mutableStateOf(true) }
    var showThermalMap by remember { mutableStateOf(false) }
    var showBypassRoute by remember { mutableStateOf(true) }
    var selectedBypassIndex by remember { mutableStateOf(0) }
    var showLayersMenu by remember { mutableStateOf(false) }
    var selectedEpicenter by remember { mutableStateOf<LatLng?>(null) }
    var showDetailsSheet by remember { mutableStateOf(false) }
    var showEvacAlertSim by remember { mutableStateOf(false) }
    var simulatedBroadcastAlert by remember { mutableStateOf<CrisisAlert?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(33.6844, 73.0479), 10f) // Islamabad/Rawalpindi area
    }

    // Dynamic mapped alerts from Database & Mock sources
    val mappedAlerts = remember(alerts) {
        alerts.filter { it.epicenterLat != null && it.epicenterLng != null }
    }

    // Smoothly pan/zoom to any selected crisis coordinates
    LaunchedEffect(selectedEpicenter) {
        selectedEpicenter?.let { latLng ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(latLng, 13f),
                durationMs = 1200
            )
        }
    }

    val activeAlert = remember(selectedEpicenter, mappedAlerts) {
        mappedAlerts.find { LatLng(it.epicenterLat!!, it.epicenterLng!!) == selectedEpicenter }
    }

    // River Coordinate Networks (Rawalpindi Nullah Lai, Lahore Ravi River, Murree Soan Streams)
    val nullahLaiCoords = remember {
        listOf(
            LatLng(33.6425, 73.0815),
            LatLng(33.6184, 73.0641),
            LatLng(33.5984, 73.0441),
            LatLng(33.5702, 73.0234)
        )
    }

    val raviRiverCoords = remember {
        listOf(
            LatLng(31.6325, 74.2715),
            LatLng(31.6110, 74.2985),
            LatLng(31.5950, 74.3210),
            LatLng(31.5794, 74.3540),
            LatLng(31.5510, 74.3985)
        )
    }

    val soanRiverCoords = remember {
        listOf(
            LatLng(33.8820, 73.3643),
            LatLng(33.8540, 73.3210),
            LatLng(33.8115, 73.2840)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AegisDark)
    ) {
        if (isOffline) {
            // Resilient grid canvas
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = AegisAlert.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AegisAlert)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = "Offline Mode",
                            tint = AegisAlert,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "EPICENTER DISPATCH TRIAGE ACTIVE",
                                style = MaterialTheme.typography.titleSmall,
                                color = AegisAlert
                            )
                            Text(
                                "Satellite imagery offline. Resilient localized vector grid map is rendering over mesh transceiver.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    TacticalEocMap(
                        alerts = alerts,
                        drones = dashboardUiState.drones,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // Real Google Maps View with Layer selections and Thermal scanning
            val mapProperties = remember(mapTypeSelected, isTrafficEnabled, showThermalMap) {
                MapProperties(
                    mapType = mapTypeSelected,
                    isMyLocationEnabled = false,
                    isTrafficEnabled = isTrafficEnabled,
                    mapStyleOptions = if (showThermalMap) {
                        MapStyleOptions(
                            """
                            [
                              {
                                "elementType": "geometry",
                                "stylers": [
                                  {
                                    "color": "#1b0a2a"
                                  }
                                ]
                              },
                              {
                                "elementType": "labels.text.fill",
                                "stylers": [
                                  {
                                    "color": "#ff007f"
                                  }
                                ]
                              },
                              {
                                "elementType": "labels.text.stroke",
                                "stylers": [
                                  {
                                    "color": "#0d0214"
                                  }
                                ]
                              },
                              {
                                "featureType": "water",
                                "elementType": "geometry",
                                "stylers": [
                                  {
                                    "color": "#ff007f"
                                  }
                                ]
                              }
                            ]
                            """.trimIndent()
                        )
                    } else {
                        MapStyleOptions(
                            """
                            [
                              {
                                "elementType": "geometry",
                                "stylers": [
                                  {
                                    "color": "#080c14"
                                  }
                                ]
                              },
                              {
                                "elementType": "labels.text.fill",
                                "stylers": [
                                  {
                                    "color": "#00e5ff"
                                  }
                                ]
                              },
                              {
                                "elementType": "labels.text.stroke",
                                "stylers": [
                                  {
                                    "color": "#080c14"
                                  }
                                ]
                              },
                              {
                                "featureType": "administrative.country",
                                "elementType": "geometry.stroke",
                                "stylers": [
                                  {
                                    "color": "#1e293b"
                                  }
                                ]
                              },
                              {
                                "featureType": "water",
                                "elementType": "geometry",
                                "stylers": [
                                  {
                                    "color": "#0f172a"
                                  }
                                ]
                              }
                            ]
                            """.trimIndent()
                        )
                    }
                )
            }

            val mapUiSettings = remember {
                MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = true,
                    tiltGesturesEnabled = true
                )
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = mapUiSettings
            ) {
                // Plot dynamic Room + Mock alerts on the Map
                mappedAlerts.forEach { alert ->
                    val epicenter = LatLng(alert.epicenterLat!!, alert.epicenterLng!!)
                    val isSelected = selectedEpicenter == epicenter
                    val markerColor = when (alert.severity) {
                        "CRITICAL" -> AegisAlert
                        "HIGH" -> AegisWarn
                        else -> AegisPrimary
                    }
                    
                    Marker(
                        state = MarkerState(position = epicenter),
                        title = alert.title,
                        snippet = "${alert.type}: ${alert.severity}",
                        onClick = {
                            selectedEpicenter = epicenter
                            showDetailsSheet = true
                            orchestrator.triggerRescueDispatch(epicenter.latitude, epicenter.longitude, alert.type, alert.nearbyHospitals, alert.nearbyPolice)
                            orchestrator.triggerPerimeterGeometry(epicenter.latitude, epicenter.longitude)
                            orchestrator.triggerMeshBroadcast(alert.type, alert.severity)
                            false
                        }
                    )

                    // Glowing density heat signatures at epicenters
                    if (showPopulationCircles) {
                        Circle(
                            center = epicenter,
                            radius = if (alert.severity == "CRITICAL") 3200.0 else 1600.0,
                            fillColor = markerColor.copy(alpha = if (isSelected) 0.28f else 0.12f),
                            strokeColor = markerColor,
                            strokeWidth = if (isSelected) 5f else 2f
                        )
                        
                        Circle(
                            center = epicenter,
                            radius = if (alert.severity == "CRITICAL") 1200.0 else 600.0,
                            fillColor = markerColor.copy(alpha = if (isSelected) 0.4f else 0.2f),
                            strokeColor = markerColor,
                            strokeWidth = 1f
                        )
                    }
                }

                // Render Evacuation Bypass alternate routes if there's an active epicenter blockage
                if (selectedEpicenter != null && showBypassRoute) {
                    val epic = selectedEpicenter!!
                    
                    // 1. Blocked Segment: a dashed polyline centered around the epicenter
                    val blockedCoords = listOf(
                        LatLng(epic.latitude - 0.002, epic.longitude - 0.002),
                        LatLng(epic.latitude, epic.longitude),
                        LatLng(epic.latitude + 0.002, epic.longitude + 0.002)
                    )
                    
                    Polyline(
                        points = blockedCoords,
                        color = AegisAlert,
                        width = 12f,
                        pattern = listOf(Dash(30f), Gap(20f)),
                        geodesic = true
                    )
                    
                    // Route Alpha: Curves North-East around blockage
                    val bypassCoordsAlpha = listOf(
                        LatLng(epic.latitude - 0.002, epic.longitude - 0.002),
                        LatLng(epic.latitude - 0.003, epic.longitude + 0.002),
                        LatLng(epic.latitude - 0.001, epic.longitude + 0.005),
                        LatLng(epic.latitude + 0.003, epic.longitude + 0.004),
                        LatLng(epic.latitude + 0.002, epic.longitude + 0.002)
                    )
                    
                    // Route Beta: Curves South-West around blockage
                    val bypassCoordsBeta = listOf(
                        LatLng(epic.latitude - 0.002, epic.longitude - 0.002),
                        LatLng(epic.latitude + 0.003, epic.longitude - 0.004),
                        LatLng(epic.latitude + 0.005, epic.longitude - 0.001),
                        LatLng(epic.latitude + 0.004, epic.longitude + 0.003),
                        LatLng(epic.latitude + 0.002, epic.longitude + 0.002)
                    )

                    // Draw Route Alpha (Optimal Bypass)
                    val alphaColor = if (selectedBypassIndex == 0) AegisSuccess else AegisSlate.copy(alpha = 0.6f)
                    val alphaOuterColor = if (selectedBypassIndex == 0) AegisSuccess.copy(alpha = 0.35f) else AegisSlate.copy(alpha = 0.2f)
                    val alphaWidth = if (selectedBypassIndex == 0) 6f else 4f
                    val alphaOuterWidth = if (selectedBypassIndex == 0) 18f else 12f

                    Polyline(
                        points = bypassCoordsAlpha,
                        color = alphaOuterColor,
                        width = alphaOuterWidth,
                        geodesic = true
                    )
                    Polyline(
                        points = bypassCoordsAlpha,
                        color = alphaColor,
                        width = alphaWidth,
                        geodesic = true
                    )

                    // Draw Route Beta (Secondary Detour)
                    val betaColor = if (selectedBypassIndex == 1) AegisPrimary else AegisSlate.copy(alpha = 0.6f)
                    val betaOuterColor = if (selectedBypassIndex == 1) AegisPrimary.copy(alpha = 0.35f) else AegisSlate.copy(alpha = 0.2f)
                    val betaWidth = if (selectedBypassIndex == 1) 6f else 4f
                    val betaOuterWidth = if (selectedBypassIndex == 1) 18f else 12f

                    Polyline(
                        points = bypassCoordsBeta,
                        color = betaOuterColor,
                        width = betaOuterWidth,
                        geodesic = true
                    )
                    Polyline(
                        points = bypassCoordsBeta,
                        color = betaColor,
                        width = betaWidth,
                        geodesic = true
                    )
                }

                // Highlight nearby hospitals and police connected by thin vector lines to selected epicenter
                if (selectedEpicenter != null && activeAlert != null) {
                    val epic = selectedEpicenter!!
                    
                    val hospCoords = listOf(
                        LatLng(epic.latitude + 0.004, epic.longitude - 0.003) to "EOC Medical Annex A",
                        LatLng(epic.latitude - 0.003, epic.longitude + 0.005) to "Trauma Unit Alpha"
                    )
                    
                    val policeCoords = listOf(
                        LatLng(epic.latitude - 0.004, epic.longitude - 0.002) to "Security Grid Zone-4",
                        LatLng(epic.latitude + 0.003, epic.longitude + 0.004) to "EOC Police Sector B"
                    )
                    
                    hospCoords.forEach { (coord, name) ->
                        Marker(
                            state = MarkerState(position = coord),
                            title = name,
                            snippet = "Emergency Medical Hub",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                        )
                        Polyline(
                            points = listOf(epic, coord),
                            color = AegisSuccess.copy(alpha = 0.5f),
                            width = 3f,
                            pattern = listOf(Dash(10f), Gap(10f))
                        )
                    }
                    
                    policeCoords.forEach { (coord, name) ->
                        Marker(
                            state = MarkerState(position = coord),
                            title = name,
                            snippet = "EOC Security Post",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )
                        Polyline(
                            points = listOf(epic, coord),
                            color = AegisPrimary.copy(alpha = 0.5f),
                            width = 3f,
                            pattern = listOf(Dash(10f), Gap(10f))
                        )
                    }
                }

                if (showRivers) {
                    // Render Nullah Lai Channel with dynamic NDMA warning status
                    val isNullahLaiActive = alerts.any { it.type == "FLOOD_WATER" && (it.placeName?.contains("Rawalpindi", ignoreCase = true) == true || (it.epicenterLat ?: 0.0) in 33.5..33.7) }
                    Polyline(
                        points = nullahLaiCoords,
                        color = if (isNullahLaiActive) AegisAlert else AegisPrimary.copy(alpha = 0.4f),
                        width = if (isNullahLaiActive) 10f else 5f,
                        geodesic = true
                    )

                    // Render Ravi River Channel with dynamic NDMA warning status
                    val isRaviActive = alerts.any { it.type == "FLOOD_WATER" && (it.placeName?.contains("Lahore", ignoreCase = true) == true || (it.epicenterLat ?: 0.0) in 31.0..32.0) }
                    Polyline(
                        points = raviRiverCoords,
                        color = if (isRaviActive) AegisAlert else AegisPrimary.copy(alpha = 0.4f),
                        width = if (isRaviActive) 10f else 5f,
                        geodesic = true
                    )

                    // Render Soan River Channel with dynamic NDMA warning status
                    val isSoanActive = alerts.any { it.type == "FLOOD_WATER" && (it.placeName?.contains("Murree", ignoreCase = true) == true || (it.epicenterLat ?: 0.0) > 33.8) }
                    Polyline(
                        points = soanRiverCoords,
                        color = if (isSoanActive) AegisAlert else AegisPrimary.copy(alpha = 0.4f),
                        width = if (isSoanActive) 10f else 5f,
                        geodesic = true
                    )
                }
            }

            // Top Status Panel Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Menu Button
                IconButton(
                    onClick = onMenuClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = AegisPanel.copy(alpha = 0.9f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.padding(end = 8.dp).clip(RoundedCornerShape(12.dp)).size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menu"
                    )
                }

                Surface(
                    color = AegisPanel.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AegisSlate),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(AegisSuccess)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "NIGEHBAN AI EOC COMMAND HUD",
                            style = MaterialTheme.typography.labelSmall,
                            color = AegisSuccess,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Map Layers Menu button
                IconButton(
                    onClick = { showLayersMenu = !showLayersMenu },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = AegisPanel.copy(alpha = 0.85f),
                        contentColor = AegisSuccess
                    ),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = "Map Layers Selector"
                    )
                }
            }

            // Sleek Floating Layer Selector HUD Card
            AnimatedVisibility(
                visible = showLayersMenu,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 16.dp)
                    .width(220.dp)
            ) {
                Surface(
                    color = AegisPanel.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, AegisSlate)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "MAP LAYER SELECTOR HUD",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        
                        Divider(color = AegisSlate.copy(alpha = 0.5f))
                        
                        // Map Type Selection Segmented Rows
                        Text("Base Map View", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(MapType.NORMAL to "Vector", MapType.SATELLITE to "Satellite").forEach { (type, label) ->
                                val selected = mapTypeSelected == type
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { mapTypeSelected = type },
                                    color = if (selected) AegisSuccess.copy(alpha = 0.2f) else AegisDark,
                                    border = BorderStroke(1.dp, if (selected) AegisSuccess else AegisSlate),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                        Text(label, color = if (selected) AegisSuccess else Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        
                        Divider(color = AegisSlate.copy(alpha = 0.3f))
                        
                        // Overlay Switches
                        Text("Tactical Overlay Layers", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        
                        LayerSwitchRow(
                            label = "Live Traffic HUD (🚗)",
                            checked = isTrafficEnabled,
                            onCheckedChange = { isTrafficEnabled = it }
                        )
                        
                        LayerSwitchRow(
                            label = "Rivers & NDMA Canals (🌊)",
                            checked = showRivers,
                            onCheckedChange = { showRivers = it }
                        )
                        
                        LayerSwitchRow(
                            label = "Heat Hazard Areas (🔴)",
                            checked = showPopulationCircles,
                            onCheckedChange = { showPopulationCircles = it }
                        )
                        
                        LayerSwitchRow(
                            label = "Infrared Thermal Scan (🌡️)",
                            checked = showThermalMap,
                            onCheckedChange = { showThermalMap = it }
                        )
                        
                        LayerSwitchRow(
                            label = "Alternate Evac Bypass (🛡️)",
                            checked = showBypassRoute,
                            onCheckedChange = { showBypassRoute = it }
                        )
                    }
                }
            }

            // HUD Zoom Controls Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        val currentZoom = cameraPositionState.position.zoom
                        cameraPositionState.move(CameraUpdateFactory.zoomTo(currentZoom + 1f))
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = AegisPanel.copy(alpha = 0.9f), contentColor = AegisPrimary),
                    modifier = Modifier.border(BorderStroke(1.dp, AegisPrimary), RoundedCornerShape(10.dp)).size(42.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In")
                }
                IconButton(
                    onClick = {
                        val currentZoom = cameraPositionState.position.zoom
                        cameraPositionState.move(CameraUpdateFactory.zoomTo(currentZoom - 1f))
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = AegisPanel.copy(alpha = 0.9f), contentColor = AegisPrimary),
                    modifier = Modifier.border(BorderStroke(1.dp, AegisPrimary), RoundedCornerShape(10.dp)).size(42.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                }
            }

            // Tactical Coordinates HUD Display (Islamabad EOC Central Grid)
            Surface(
                color = AegisPanel.copy(alpha = 0.85f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AegisSlate),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 80.dp, start = 16.dp)
                    .width(180.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("COORDINATES HUD", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    val lat = cameraPositionState.position.target.latitude
                    val lng = cameraPositionState.position.target.longitude
                    Text(
                        "${"%.5f".format(lat)}°N\n${"%.5f".format(lng)}°E",
                        color = AegisPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Divider(color = AegisSlate.copy(alpha = 0.4f), thickness = 0.5.dp)
                    Text("ZOOM LEVEL: ${"%.1f".format(cameraPositionState.position.zoom)}", color = Color.LightGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }

    // Modal Bottom Sheet for marker click
    if (showDetailsSheet && activeAlert != null) {
        val alert = activeAlert!!
        ModalBottomSheet(
            onDismissRequest = { showDetailsSheet = false },
            containerColor = AegisPanel,
            contentColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = AegisSlate) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            alert.title.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Sector: ${alert.placeName ?: alert.zone}",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    Surface(
                        color = when (alert.severity) {
                            "CRITICAL" -> AegisAlert.copy(alpha = 0.15f)
                            "HIGH" -> AegisWarn.copy(alpha = 0.15f)
                            else -> AegisPrimary.copy(alpha = 0.15f)
                        },
                        border = BorderStroke(
                            1.dp,
                            when (alert.severity) {
                                "CRITICAL" -> AegisAlert
                                "HIGH" -> AegisWarn
                                else -> AegisPrimary
                            }
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            alert.severity,
                            color = when (alert.severity) {
                                "CRITICAL" -> AegisAlert
                                "HIGH" -> AegisWarn
                                else -> AegisPrimary
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // ⚠️ Road Blocked alternate bypass route warning badge and selector
                if (showBypassRoute) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = AegisAlert.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, AegisAlert.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = AegisAlert, modifier = Modifier.size(16.dp))
                                    Text("EPICENTER ROAD BLOCKAGE DETECTED", color = AegisAlert, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Text(
                                    "Evacuation road segments collapsed or flooded. Nigehban AI has computed two alternate safety bypass detours around the active disaster grid. Select a route below to update primary dispatch coordinates.",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        // Alternate Route Options Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Route Alpha Button
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedBypassIndex = 0 },
                                color = if (selectedBypassIndex == 0) AegisSuccess.copy(alpha = 0.15f) else AegisDark,
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedBypassIndex == 0) AegisSuccess else AegisSlate.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Navigation,
                                            contentDescription = null,
                                            tint = if (selectedBypassIndex == 0) AegisSuccess else Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            "ROUTE ALPHA",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (selectedBypassIndex == 0) AegisSuccess else Color.LightGray
                                        )
                                    }
                                    Text("Optimal Detour", color = Color.Gray, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Distance: 4.2 km", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Est: 6 mins | SAFE", color = AegisSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Route Beta Button
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedBypassIndex = 1 },
                                color = if (selectedBypassIndex == 1) AegisPrimary.copy(alpha = 0.15f) else AegisDark,
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedBypassIndex == 1) AegisPrimary else AegisSlate.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Navigation,
                                            contentDescription = null,
                                            tint = if (selectedBypassIndex == 1) AegisPrimary else Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            "ROUTE BETA",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (selectedBypassIndex == 1) AegisPrimary else Color.LightGray
                                        )
                                    }
                                    Text("Secondary Loop", color = Color.Gray, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Distance: 6.5 km", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Est: 11 mins | SLOW", color = AegisWarn, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Ticking Response Timer & Details
                Surface(
                    color = AegisDark,
                    border = BorderStroke(1.dp, AegisSlate),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = AegisSuccess, modifier = Modifier.size(16.dp))
                                Text("ACTIVE RESPONSE TIME", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            // Real ticking response timer calculation since alert.timestamp
                            var elapsedStr by remember(alert) { mutableStateOf("00:00:00") }
                            LaunchedEffect(alert) {
                                while (true) {
                                    val delta = System.currentTimeMillis() - alert.timestamp
                                    if (delta > 0) {
                                        val secs = (delta / 1000) % 60
                                        val mins = (delta / 60000) % 60
                                        val hrs = (delta / 3600000)
                                        elapsedStr = String.format("%02d:%02d:%02d", hrs, mins, secs)
                                    }
                                    delay(1000)
                                }
                            }
                            Text(
                                    elapsedStr,
                                    color = AegisSuccess,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp
                                )
                        }

                        Divider(color = AegisSlate.copy(alpha = 0.5f))

                        Text(alert.description, color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }

                // Live Drone Recon Stream showing altitude battery status and sweep lines
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("LIVE DRONE RECON STREAMING FEEDS:", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(BorderStroke(1.dp, AegisPrimary))
                    ) {
                        var altitude by remember { mutableStateOf(124) }
                        var speed by remember { mutableStateOf(44) }
                        var battery by remember { mutableStateOf(86) }
                        var recordingDot by remember { mutableStateOf(true) }
                        
                        LaunchedEffect(Unit) {
                            while (true) {
                                delay(1000)
                                altitude = 120 + (0..10).random()
                                speed = 40 + (0..8).random()
                                battery = maxOf(30, battery - (0..1).random())
                                recordingDot = !recordingDot
                            }
                        }
                        
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val gridSpacing = 20.dp.toPx()
                            for (x in 0 until (size.width / gridSpacing).toInt()) {
                                drawLine(
                                    color = AegisPrimary.copy(alpha = 0.05f),
                                    start = androidx.compose.ui.geometry.Offset(x * gridSpacing, 0f),
                                    end = androidx.compose.ui.geometry.Offset(x * gridSpacing, size.height),
                                    strokeWidth = 1f
                                )
                            }
                            for (y in 0 until (size.height / gridSpacing).toInt()) {
                                drawLine(
                                    color = AegisPrimary.copy(alpha = 0.05f),
                                    start = androidx.compose.ui.geometry.Offset(0f, y * gridSpacing),
                                    end = androidx.compose.ui.geometry.Offset(size.width, y * gridSpacing),
                                    strokeWidth = 1f
                                )
                            }
                        }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (recordingDot) AegisAlert else Color.Transparent)
                                    )
                                    Text("LIVE RECON FEED", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Text("BAT: $battery% 🔋", color = if (battery < 20) AegisAlert else AegisSuccess, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                            
                            Box(
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Adjust, contentDescription = null, tint = AegisPrimary.copy(alpha = 0.4f), modifier = Modifier.size(44.dp))
                                Text("LOCKING EPICENTER", color = AegisPrimary.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 34.dp))
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("ALT: ${altitude}m 🔺", color = AegisPrimary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text("SPD: ${speed}km/h 🏎️", color = AegisPrimary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text("HDG: 284°NW", color = AegisPrimary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                // Horizontal slideable photo gallery
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("FIELD PHOTO ARCHIVE ATTACHMENTS:", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val mockPhotos = listOf(
                            "Landslide blockage" to "🏔️",
                            "Overflowing canal channel" to "🌊",
                            "Debris collapse" to "🏚️",
                            "EOC Command dispatch team" to "🚒"
                        )
                        
                        mockPhotos.forEach { (label, emoji) ->
                            Surface(
                                color = AegisDark,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, AegisSlate),
                                modifier = Modifier.size(100.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(emoji, fontSize = 28.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(label, color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2)
                                }
                            }
                        }
                    }
                }

                // Safety Sweep resolved items
                if (alert.nearbyHospitals.isNotEmpty() || alert.nearbyPolice.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("RESOLVED NEAREST ASSETS:", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        
                        if (alert.nearbyHospitals.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🚑 MEDICAL:", color = AegisSuccess, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(80.dp))
                                Column {
                                    alert.nearbyHospitals.take(3).forEach { hosp ->
                                        Text("• $hosp", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        if (alert.nearbyPolice.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🛡️ POLICE:", color = AegisPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(80.dp))
                                Column {
                                    alert.nearbyPolice.take(3).forEach { pol ->
                                        Text("• $pol", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Rivers / Canal Flooding telemetry overlay
                if (alert.type == "FLOOD_WATER") {
                    val telemetry = viewModel.getNdmaFloodTelemetry(alert.epicenterLat!!, alert.epicenterLng!!, alert.placeName)
                    Surface(
                        color = AegisPrimary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AegisPrimary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Water, contentDescription = null, tint = AegisPrimary, modifier = Modifier.size(16.dp))
                                Text("NDMA WATER TELEMETRY", color = AegisPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(telemetry, color = Color.White, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                    }
                }

                // Broadcast Evacuation Alert button + Force Dispatch Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            orchestrator.triggerRescueDispatch(alert.epicenterLat!!, alert.epicenterLng!!, alert.type, alert.nearbyHospitals, alert.nearbyPolice)
                            showDetailsSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AegisSlate, contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Force Dispatch", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            simulatedBroadcastAlert = alert
                            showEvacAlertSim = true
                            showDetailsSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AegisAlert, contentColor = Color.White),
                        modifier = Modifier.weight(1.2f).height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Evacuation Alert", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Simulated Cell-Broadcast Flashing Alert Dialog
    if (showEvacAlertSim && simulatedBroadcastAlert != null) {
        val alert = simulatedBroadcastAlert!!
        
        // Alternating Warning Colors loop for blinking warning panel
        var isBlinkingRed by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            while (true) {
                isBlinkingRed = !isBlinkingRed
                delay(500)
            }
        }

        Dialog(onDismissRequest = { showEvacAlertSim = false }) {
            Surface(
                color = AegisDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, if (isBlinkingRed) AegisAlert else AegisWarn),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Flashing Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isBlinkingRed) AegisAlert else AegisWarn)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Emergency, contentDescription = null, tint = Color.White)
                            Text(
                                "CIVIL DEFENSE CELL BROADCAST",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Icon(
                        Icons.Default.Campaign,
                        contentDescription = null,
                        tint = if (isBlinkingRed) AegisAlert else AegisWarn,
                        modifier = Modifier.size(54.dp)
                    )

                    Text(
                        "IMMEDIATE EVACUATION DIRECTIVE",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )

                    Surface(
                        color = AegisPanel,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AegisSlate),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "CRISIS SEC: ${alert.placeName ?: alert.zone}",
                                color = AegisPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "threat type: ${alert.type}",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Nigehban AI EOC has detected high-severity danger levels in this coordinate grid sector. Citizens are instructed to evacuate immediately to the nearest designated safety zones or emergency shelter coordinates.",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Button(
                        onClick = { showEvacAlertSim = false },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isBlinkingRed) AegisAlert else AegisWarn, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("CONFIRM EVACUATION RECEIPT", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AegisSuccess,
                checkedTrackColor = AegisSuccess.copy(alpha = 0.4f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = AegisDark
            ),
            modifier = Modifier.scale(0.7f)
        )
    }
}
