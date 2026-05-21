package com.aegisnet.mobile.ui.feed

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisnet.mobile.domain.model.CrisisAlert
import com.aegisnet.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrisisFeedScreen(
    alerts: List<CrisisAlert>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("LIVE CRISIS ANALYST HUD", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Nigehban AI (نگہبان AI) Social Signals", fontSize = 10.sp, color = Color.Gray)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pinned Nigehban AI Analyst Bot Card
            item {
                NigehbanAiAnalystCard(alertsCount = alerts.size)
            }

            item {
                Text(
                    "REAL-TIME VETTED INCIDENT FEEDS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(alerts, key = { "feed-${it.id}" }) { alert ->
                CrisisFeedItem(alert)
            }
        }
    }
}

@Composable
private fun NigehbanAiAnalystCard(alertsCount: Int) {
    var expandedSummary by remember { mutableStateOf(false) }

    Surface(
        color = AegisPanel,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, AegisPrimary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AegisPrimary.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = AegisPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            "NIGEHBAN AI ANALYST BOT",
                            style = MaterialTheme.typography.labelSmall,
                            color = AegisPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            "EOC Neural Assessment Feed",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                }

                Surface(
                    color = AegisSuccess.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AegisSuccess)
                ) {
                    Text(
                        "LIVE MONITORING",
                        color = AegisSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Divider(color = AegisSlate.copy(alpha = 0.4f))

            Text(
                "Telemetry matrices report $alertsCount active incidents across localized sectors. Heavy monsoon grids in Rawalpindi and high density rally corridors in Lahore are driving EOC dispatch queues.",
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )

            AnimatedVisibility(
                visible = expandedSummary,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Divider(color = AegisSlate.copy(alpha = 0.3f))
                    
                    Text("CRITICAL STRATEGIC SUMMARY:", color = AegisWarn, fontWeight = FontWeight.Bold, fontSize = 10.sp)

                    BulletPoint("Rivers Telemetry: WASA sensors triggered alert level orange at Nullah Lai channel bypasses. Flash flooding alternate routes mapped.")
                    BulletPoint("Jalsa Logistics: Political rally on Mall Road Lahore mapped. Closest fire engines and tactical paramedics pre-assigned from nearest station depot.")
                    BulletPoint("Drone Recon: Tactical sweeps indicate high clearance zones. Ground cell-transceiver signals remain active.")

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { /* Simulated external news link */ },
                            colors = ButtonDefaults.buttonColors(containerColor = AegisSlate, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("NDMA Bulletin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { /* Simulated satellite link */ },
                            colors = ButtonDefaults.buttonColors(containerColor = AegisSlate, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.RssFeed, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("EOC Buzz Desk", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedSummary = !expandedSummary },
                color = AegisDark.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, AegisSlate.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (expandedSummary) "COLLAPSE AI SUMMARY" else "EXPAND DYNAMIC CHIPS & BULLETINS",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        if (expandedSummary) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("•", color = AegisPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text, color = Color.LightGray, fontSize = 11.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun CrisisFeedItem(alert: CrisisAlert) {
    var isExpanded by remember { mutableStateOf(false) }

    val statusColor = when (alert.severity) {
        "CRITICAL" -> AegisAlert
        "HIGH" -> AegisWarn
        else -> AegisSuccess
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = AegisPanel),
        border = BorderStroke(1.dp, if (isExpanded) statusColor else statusColor.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header with type and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(statusColor)
                    )
                    Text(getCrisisTypeLabel(alert.type), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
                Text(alert.createdAt, fontSize = 9.sp, color = Color(0xFF64748B))
            }

            // Title
            Text(
                alert.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            // Description
            Text(
                alert.description,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            // Premium Stats Row replacing generic Affected count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val population = alert.affectedPopulation
                val displacedStr = when (alert.type) {
                    "FLOOD_ESCALATION", "FLOOD_WATER" -> "Displaced: ${(population * 0.12).toInt()} households"
                    "CYBERATTACK" -> "Network: Grid 4B Outage"
                    "INFRASTRUCTURE_COLLAPSE", "ROAD_COLLAPSE" -> "Debris: Severe blockage"
                    "POLITICAL_RALLY" -> "Congestion: High density"
                    else -> "Displaced: ${(population * 0.08).toInt()} families"
                }

                StatItem("Impact Telemetry", displacedStr, Color.White)
                StatItem("Hazard Risk", "${alert.casualtyRiskScore}%", statusColor)
                StatItem("Dispatch ETA", "${alert.responseTimeMinutes} mins", AegisPrimary)
            }

            // Expandable citizen buzz and drone thumbnail gallery
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Divider(color = AegisSlate.copy(alpha = 0.3f))

                    // Citizen Sentiment & Buzz Feeds (Twitter/X style)
                    Text(
                        "LIVE VERIFIED CITIZEN BUZZ (X-DECOMMUNICATION)",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val buzzList = when (alert.type) {
                            "FLOOD_WATER", "FLOOD_ESCALATION" -> listOf(
                                Triple("Ahmad Bilal @AhmadBilal", "Nullah Lai coordinates indicate overflow, but EOC double-layer safety bypass routes redirected traffic safely.WASAs pumps en route! ✓", "148 likes • 22 retweets"),
                                Triple("Dr. Ayesha @AyeshaMD", "Emergency ambulance assets dispatched from closest sector depot. Visualized drone sweeps scanning perimeter structures.", "92 likes • 15 retweets")
                            )
                            "POLITICAL_RALLY" -> listOf(
                                Triple("Kamran Shah @Kamran_Shah", "Mall Road rally is massive. EOC Security Sector Posts are active directing traffic to grid annexes.", "310 likes • 84 retweets"),
                                Triple("Zara Khan @ZaraK_EOC", "Nearby trauma units annexes fully pre-allocated. Smart dispatch dispatcher sorting backup assets.", "108 likes • 12 retweets")
                            )
                            else -> listOf(
                                Triple("Hassan Jamil @HassanJ", "Tactical roads reported collapsed near underpass, but Nigehban safety detour vectors are fully flashing on maps. Great response!", "78 likes • 9 retweets"),
                                Triple("Saima R. @SaimaRescue", "Aegis SWAT teams secure structural perimeters. Dynamic response timers running en route.", "124 likes • 28 retweets")
                            )
                        }

                        buzzList.forEach { (user, content, metrics) ->
                            Surface(
                                color = AegisDark,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, AegisSlate.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(user, color = AegisPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Icon(Icons.Default.Verified, contentDescription = "Verified User", tint = AegisPrimary, modifier = Modifier.size(12.dp))
                                    }
                                    Text(content, color = Color.LightGray, fontSize = 11.sp, lineHeight = 15.sp)
                                    Text(metrics, color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    // Drone Recon Video Thumbnails
                    Text(
                        "DRONE RECONNAISSANCE STREAM ARCHIVE",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("Drone Sweep Alpha" to "🎥", "Thermal Infrared" to "🌡️", "Volumetric Scan" to "🛰️").forEach { (title, symbol) ->
                            Surface(
                                color = AegisDark,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, AegisSlate),
                                modifier = Modifier
                                    .size(width = 130.dp, height = 80.dp)
                                    .clickable { /* Simulate replay */ }
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Text(symbol, fontSize = 20.sp)
                                        Text(title, color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(AegisAlert))
                                            Text("REPLAY", color = AegisAlert, fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Resources deployed
            if (alert.resourcesDeployed.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("RESOURCES ASSIGNED IN FIELD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        alert.resourcesDeployed.take(3).forEach { resource ->
                            Surface(
                                modifier = Modifier.height(22.dp),
                                color = AegisPrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, AegisPrimary.copy(alpha = 0.3f))
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(resource.uppercase(), fontSize = 8.sp, color = AegisPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (alert.resourcesDeployed.size > 3) {
                            Text("+${alert.resourcesDeployed.size - 3}", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Expand HUD Toggle hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isExpanded) "⚡ CLICK CARD TO COLLAPSE BUZZ" else "⚡ CLICK CARD TO EXPAND CITIZEN BUZZ",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    modifier = Modifier.height(24.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Box(modifier = Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                        Text(alert.status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = statusColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column {
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label.uppercase(), fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
    }
}

private fun getCrisisTypeLabel(type: String): String = when (type) {
    "POLITICAL_RALLY" -> "📢 Political Rally"
    "ARMED_VIOLENCE" -> "🔫 Armed Violence"
    "STAMPEDE_RISK" -> "🏃 Stampede Risk"
    "DISEASE_OUTBREAK" -> "🦠 Disease"
    "CYBERATTACK" -> "💻 Cyber Attack"
    "INFRASTRUCTURE_COLLAPSE" -> "🏗️ Infrastructure"
    "MASS_ENTRAPMENT_RISK" -> "⛓️ Entrapment"
    "FLOOD_ESCALATION" -> "🌊 Flooding"
    else -> "⚠️ Crisis"
}


