package com.aegisnet.mobile.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
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
import com.aegisnet.mobile.ui.theme.AegisAlert
import com.aegisnet.mobile.ui.theme.AegisDark
import com.aegisnet.mobile.ui.theme.AegisPanel
import com.aegisnet.mobile.ui.theme.AegisPrimary
import com.aegisnet.mobile.ui.theme.AegisWarn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedReportIncidentScreen(
    onBack: () -> Unit,
    onSubmit: (type: String, title: String, description: String, severity: String, affectedCount: Int) -> Unit
) {
    var selectedType by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSeverity by remember { mutableStateOf("HIGH") }
    var affectedCount by remember { mutableStateOf("") }
    var showTypeDropdown by remember { mutableStateOf(false) }

    val crisisTypes = listOf(
        "ARMED_VIOLENCE" to "🔫 Active Shooting/Armed Violence",
        "POLITICAL_RALLY" to "📢 Political Rally/Jalsa",
        "STAMPEDE_RISK" to "🏃 Stampede/Crowd Crush",
        "DISEASE_OUTBREAK" to "🦠 Disease Outbreak",
        "INFRASTRUCTURE_COLLAPSE" to "🏗️ Infrastructure Collapse",
        "CYBERATTACK" to "💻 Cyber Attack",
        "MASS_ENTRAPMENT_RISK" to "⛓️ Mass Entrapment",
        "FLOOD_ESCALATION" to "🌊 Flooding/Water Emergency",
        "TRAFFIC_ACCIDENT" to "🚗 Major Traffic Accident",
        "FIRE_OUTBREAK" to "🔥 Fire Outbreak"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Crisis Incident", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AegisPanel)
            )
        },
        containerColor = AegisDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Crisis Type Selector
            Text("1. Select Crisis Type", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AegisPrimary)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showTypeDropdown = !showTypeDropdown },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(
                        if (selectedType.isEmpty()) "Select a crisis type..." else crisisTypes.find { it.first == selectedType }?.second ?: selectedType,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                    )
                }

                // Dropdown menu
                DropdownMenu(
                    expanded = showTypeDropdown,
                    onDismissRequest = { showTypeDropdown = false },
                    modifier = Modifier
                        .background(AegisPanel)
                        .fillMaxWidth(0.95f)
                ) {
                    crisisTypes.forEach { (type, label) ->
                        DropdownMenuItem(
                            text = { Text(label, fontSize = 12.sp) },
                            onClick = {
                                selectedType = type
                                showTypeDropdown = false
                            }
                        )
                    }
                }
            }

            // Title
            Text("2. Incident Title", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AegisPrimary)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("e.g., Shooting at Defence Mall", color = Color(0xFF64748B)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AegisPrimary,
                    unfocusedBorderColor = Color(0xFF475569)
                ),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                singleLine = true
            )

            // Description
            Text("3. Detailed Description", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AegisPrimary)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Describe what you see, heard, or know about the incident...", color = Color(0xFF64748B)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AegisPrimary,
                    unfocusedBorderColor = Color(0xFF475569)
                ),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                maxLines = 5
            )

            // Severity Level
            Text("4. Severity Assessment", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AegisPrimary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("LOW", "MEDIUM", "HIGH", "CRITICAL").forEach { severity ->
                    FilterChip(
                        selected = selectedSeverity == severity,
                        onClick = { selectedSeverity = severity },
                        label = { Text(severity, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (severity) {
                                "CRITICAL" -> AegisAlert
                                "HIGH" -> AegisWarn
                                else -> AegisPrimary
                            },
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Affected Count
            Text("5. Estimated Affected People", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AegisPrimary)
            OutlinedTextField(
                value = affectedCount,
                onValueChange = { affectedCount = it },
                placeholder = { Text("e.g., 50, 100, 500...", color = Color(0xFF64748B)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AegisPrimary,
                    unfocusedBorderColor = Color(0xFF475569)
                ),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                singleLine = true
            )

            // Additional info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AegisPrimary.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, AegisPrimary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("📍 Location", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = AegisPrimary)
                    Text("Your GPS location will be automatically included with your report.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }

            // Submit button
            Button(
                onClick = {
                    if (selectedType.isNotEmpty() && title.isNotEmpty() && description.isNotEmpty()) {
                        onSubmit(
                            selectedType,
                            title,
                            description,
                            selectedSeverity,
                            affectedCount.toIntOrNull() ?: 0
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AegisPrimary)
            ) {
                Text("Submit Incident Report", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

