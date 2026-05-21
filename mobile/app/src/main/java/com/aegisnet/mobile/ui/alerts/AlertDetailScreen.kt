package com.aegisnet.mobile.ui.alerts

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisnet.mobile.domain.model.CrisisAlert
import com.aegisnet.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDetailScreen(
    alert: CrisisAlert,
    onBack: () -> Unit
) {
    val riskColor = when {
        alert.casualtyRiskScore >= 80 -> AegisAlert
        alert.casualtyRiskScore >= 50 -> AegisWarn
        else -> AegisSuccess
    }

    Scaffold(
        containerColor = AegisDark,
        topBar = {
            TopAppBar(
                title = { Text("AI Prediction Detail", color = Color.White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AegisPanel)
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Alert Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AegisPanel),
                border = BorderStroke(1.dp, riskColor.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(alert.type.replace("_", " "), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = riskColor.copy(alpha = 0.15f), labelColor = riskColor)
                    )
                    Text(alert.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(alert.description, color = Color(0xFF94A3B8), fontSize = 13.sp, lineHeight = 20.sp)

                    Divider(color = AegisSlate)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        RiskMetric("CASUALTY RISK", "${alert.casualtyRiskScore}%", riskColor)
                        Box(modifier = Modifier.height(40.dp).width(1.dp).background(AegisSlate))
                        RiskMetric("ESCALATION PROB.", "${alert.escalationProbability}%", riskColor)
                        Box(modifier = Modifier.height(40.dp).width(1.dp).background(AegisSlate))
                        RiskMetric("STATUS", alert.status, AegisPrimary)
                    }
                }
            }

            // Agent Reasoning Chain
            SectionLabel("AI AGENT REASONING CHAIN")
            AgentTraceCard(
                agent = "Agent_1: Signal Intake",
                color = AegisPrimary,
                trace = "Ingested SUPARCO_WEATHER signal. Precipitation: 55mm/hr, Temp: -6°C. Normalized & stored."
            )
            AgentTraceCard(
                agent = "Agent_2: Social Verification",
                color = Color(0xFF60A5FA),
                trace = "Analyzed 3 social reports in Murree region. Account verification passed. Credibility scored: 0.94. Spam probability: 0.03."
            )
            AgentTraceCard(
                agent = "Agent_3: Crisis Correlation",
                color = AegisWarn,
                trace = "Cluster detected: Weather anomaly overlaps with verified social panic in grid [33.90, 73.39]. Triggering escalation agent."
            )
            AgentTraceCard(
                agent = "Agent_4: Predictive Escalation",
                color = AegisAlert,
                trace = "Historical pattern match: 82% similarity to Murree 2022 incident.\nEntrapment probability: 95% within 45 minutes.\nRecommended: Road closure + Drone dispatch."
            )
            AgentTraceCard(
                agent = "Agent_5: Drone Coordination",
                color = AegisSuccess,
                trace = "Dispatched DRONE_MRE_01 to grid [33.90, 73.39]. Thermal imaging active. 34 humans detected across 127 vehicles."
            )

            // Recommended Preventive Actions
            SectionLabel("RECOMMENDED PREVENTIVE ACTIONS")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AegisPanel),
                border = BorderStroke(1.dp, AegisSlate)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionItem("🚧", "Close Murree Expressway immediately")
                    ActionItem("🚁", "Deploy thermal drone swarm")
                    ActionItem("🚨", "Alert Rescue 1122 and NDMA")
                    ActionItem("📢", "Issue citizen evacuation alerts")
                    ActionItem("🏥", "Pre-position mobile medical units")
                }
            }

            // Coordinate display
            if (alert.epicenterLat != null && alert.epicenterLng != null) {
                SectionLabel("EPICENTER COORDINATES")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AegisPanel),
                    border = BorderStroke(1.dp, AegisSlate)
                ) {
                    Text(
                        text = "LAT: ${alert.epicenterLat}°N  |  LNG: ${alert.epicenterLng}°E",
                        modifier = Modifier.padding(14.dp),
                        color = AegisPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(label, color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
}

@Composable
private fun RiskMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = Color(0xFF64748B), fontSize = 9.sp)
    }
}

@Composable
private fun AgentTraceCard(agent: String, color: Color, trace: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AegisPanel),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("> [$agent]", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(trace, color = Color(0xFFCBD5E1), fontSize = 12.sp, lineHeight = 18.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun ActionItem(emoji: String, action: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 16.sp)
        Text(action, color = Color(0xFFE2E8F0), fontSize = 13.sp)
    }
}
