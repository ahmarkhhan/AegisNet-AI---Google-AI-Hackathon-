package com.aegisnet.mobile.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisnet.mobile.domain.model.CrisisAlert
import com.aegisnet.mobile.ui.theme.AegisAlert
import com.aegisnet.mobile.ui.theme.AegisDark
import com.aegisnet.mobile.ui.theme.AegisPanel
import com.aegisnet.mobile.ui.theme.AegisPrimary
import com.aegisnet.mobile.ui.theme.AegisSuccess
import com.aegisnet.mobile.ui.theme.AegisWarn
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrisisMapScreen(
    alerts: List<CrisisAlert>,
    onBack: () -> Unit,
    onAlertClick: (CrisisAlert) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(31.5204, 74.3587), 7f) // Pakistan center
    }
    var selectedAlert by remember { mutableStateOf<CrisisAlert?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crisis Hotspot Map", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AegisPanel)
            )
        },
        containerColor = AegisDark
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            // Google Map with crisis markers
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                alerts.forEach { alert ->
                    if (alert.epicenterLat != null && alert.epicenterLng != null) {
                        val markerColor = when (alert.severity) {
                            "CRITICAL" -> Color.Red
                            "HIGH" -> Color(0xFFFF9800)
                            else -> Color(0xFFFFeb3b)
                        }
                        Marker(
                            state = MarkerState(position = LatLng(alert.epicenterLat, alert.epicenterLng)),
                            title = alert.title,
                            snippet = alert.type,
                            onClick = {
                                selectedAlert = alert
                                false
                            }
                        )
                    }
                }
            }

            // Crisis info bottom sheet
            if (selectedAlert != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(AegisPanel, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedAlert!!.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            Button(onClick = { onAlertClick(selectedAlert!!) }, modifier = Modifier.height(32.dp)) {
                                Text("View Details", fontSize = 11.sp)
                            }
                        }
                        Text(selectedAlert!!.description, fontSize = 12.sp, color = Color(0xFF94A3B8), maxLines = 2)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip(label = { Text("Risk: ${selectedAlert!!.casualtyRiskScore}%", fontSize = 10.sp) })
                            Chip(label = { Text("Pop: ${selectedAlert!!.affectedPopulation}", fontSize = 10.sp) })
                        }
                    }
                }
            }

            // Legend
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(AegisPanel, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.width(120.dp)) {
                    Text("Severity", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White)
                    LegendItem("CRITICAL", Color.Red)
                    LegendItem("HIGH", Color(0xFFFF9800))
                    LegendItem("MEDIUM", Color(0xFFFFeb3b))
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(label, fontSize = 9.sp, color = Color(0xFF94A3B8))
    }
}

@Composable
private fun Chip(label: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.height(24.dp),
        color = AegisPrimary.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
            label()
        }
    }
}

