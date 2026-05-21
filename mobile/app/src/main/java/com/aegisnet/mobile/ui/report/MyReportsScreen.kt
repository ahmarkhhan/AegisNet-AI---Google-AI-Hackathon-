package com.aegisnet.mobile.ui.report

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegisnet.mobile.data.local.IncidentEntity
import com.aegisnet.mobile.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportIncidentViewModel = hiltViewModel()
) {
    val cachedIncidents by viewModel.cachedIncidents.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = AegisDark,
        topBar = {
            TopAppBar(
                title = { Text("MY REPORTED CRISES", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AegisPanel)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (cachedIncidents.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.ReportGmailerrorred,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No reported crises registry found.",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "All reports you log inside the Nigehban AI EOC registry will be displayed here, including full media attachments.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        lineHeight = 18.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "EOC SECURED INCIDENT LOGS (${cachedIncidents.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )

                    cachedIncidents.forEach { incident ->
                        MyIncidentItem(incident)
                    }
                }
            }
        }
    }
}

@Composable
fun MyIncidentItem(incident: IncidentEntity) {
    val dateStr = SimpleDateFormat("MMM dd yyyy, HH:mm:ss", Locale.getDefault()).format(Date(incident.timestamp))
    val photoBitmap = rememberFileImage(incident.photoPath)

    Surface(
        color = AegisPanel,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AegisSlate),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    incident.title.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = AegisAlert.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AegisAlert)
                ) {
                    Text(
                        incident.severity.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = AegisAlert,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Description
            Text(
                incident.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                lineHeight = 20.sp
            )

            // Local permanent photo
            if (photoBitmap != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AegisDark)
                ) {
                    Image(
                        bitmap = photoBitmap,
                        contentDescription = "Crisis Evidence Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        Row(
                            modifier = Modifier.padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = AegisSuccess, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("EOC Secure Image Evidence", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (!incident.photoPath.isNullOrBlank()) {
                // Photo exists but failed to decode or file missing, show telemetry path
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Media registry: ${File(incident.photoPath).name}", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // Video attachment info
            if (!incident.videoPath.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = AegisPrimary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AegisPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = AegisPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Telemetry Video Attached", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("File: ${File(incident.videoPath).name}", color = Color.LightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // Infrastructure Sweep Results
            if (incident.nearbyHospitalsJson?.isNotBlank() == true || incident.nearbyPoliceJson?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = AegisSlate.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Text("EOC SAFETY ASSETS IN SECTOR GRID:", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                if (incident.nearbyHospitalsJson?.isNotBlank() == true) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("🚑 HOSPITALS:", color = AegisSuccess, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.width(90.dp))
                        Column {
                            incident.nearbyHospitalsJson.split("||").forEach { hospital ->
                                Text("• $hospital", color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                }

                if (incident.nearbyPoliceJson?.isNotBlank() == true) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("🛡️ POLICE ZONE:", color = AegisPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.width(90.dp))
                        Column {
                            incident.nearbyPoliceJson.split("||").forEach { police ->
                                Text("• $police", color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // Political party custom details
            if (incident.politicalParty != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = AegisAlert.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AegisAlert.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = AegisAlert, modifier = Modifier.size(16.dp))
                            Text("Political Rally Corridor: ${incident.politicalParty}", color = AegisAlert, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(incident.politicalImplications ?: "", color = Color.White, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                }
            }

            // Seismic metrics
            if (incident.seismicMagnitude != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = AegisWarn.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AegisWarn.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Waves, contentDescription = null, tint = AegisWarn, modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                "Seismic Shock: ${incident.seismicMagnitude} Mw | Focal Depth: ${incident.seismicDepth} km",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            if (incident.seismicTremors == true) {
                                Text("⚠️ WARNING: Secondary aftershocks detected in EOC system.", color = AegisWarn, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = AegisSlate.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Footer coordinates and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PinDrop,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${incident.placeName} (${"%.4f".format(incident.latitude)}°N, ${"%.4f".format(incident.longitude)}°E)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Text(
                    dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun rememberFileImage(path: String?): androidx.compose.ui.graphics.ImageBitmap? {
    if (path.isNullOrBlank()) return null
    return remember(path) {
        try {
            val file = File(path)
            if (file.exists()) {
                BitmapFactory.decodeFile(path)?.asImageBitmap()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
