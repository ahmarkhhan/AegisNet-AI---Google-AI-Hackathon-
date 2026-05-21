package com.aegisnet.mobile.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Welcome to AEGISNET AI", fontSize = 16.sp) })
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("AEGISNET AI", style = MaterialTheme.typography.headlineMedium)
                Text("Crisis Intelligence at your fingertips.", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onContinue) { Text("Get Started") }
            }
        }
    }
}

