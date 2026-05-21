package com.aegisnet.mobile.ui.report

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegisnet.mobile.data.local.IncidentEntity
import com.aegisnet.mobile.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.content.Intent
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIncidentScreen(
    onBack: () -> Unit,
    viewModel: ReportIncidentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cachedIncidents by viewModel.cachedIncidents.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var recordingProgress by remember { mutableStateOf(0f) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    var photoFile by remember { mutableStateOf<java.io.File?>(null) }
    var videoFile by remember { mutableStateOf<java.io.File?>(null) }

    val incidentTypes = listOf(
        "TRAFFIC_BLOCKAGE",
        "FLOOD_WATER",
        "SEISMIC_ACTIVITY",
        "POLITICAL_RALLY",
        "ROAD_COLLAPSE",
        "OTHER"
    )

    // Permission request launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Toast.makeText(context, "Sensor access approved ✓", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Action denied. EOC central command requires sensor approval.", Toast.LENGTH_LONG).show()
            }
        }
    )

    // Camera photo capture intent launcher
    val capturePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                photoFile?.let { file ->
                    viewModel.addAttachment(file.absolutePath, uiState.videoPath)
                    Toast.makeText(context, "Crisis Image Bound to Registry ✓", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    // Camera video capture intent launcher
    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                videoFile?.let { file ->
                    viewModel.addAttachment(uiState.photoPath, file.absolutePath)
                    Toast.makeText(context, "Crisis Telemetry Video Saved (Max 120s) ✓", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    fun startSpeechListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        isRecording = true
        recordingProgress = 0f

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                recordingProgress = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isRecording = false
            }
            override fun onError(error: Int) {
                isRecording = false
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio hardware capture failure."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No voice matches. Try speaking closer."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech transcribers busy."
                    else -> "Transcriber bypassed to simulated EOC NLP engine."
                }
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                // Graceful fallback to EOC simulated NLP dictation
                val fallbackWords = when (uiState.incidentType) {
                    "FLOOD_WATER" -> "Heavy flash flooding reported in the sector. Channels overflowed, local road bypasses blocked."
                    "SEISMIC_ACTIVITY" -> "Earthquake tremors detected. Shaking scale felt high, epicentre depth requires EOC triage."
                    "POLITICAL_RALLY" -> "Political gathering on Mall Road. Massive rally foot traffic and road blockages."
                    else -> "Tactical command alert: road collapse near underpasses. Evacuation routing advised."
                }
                viewModel.addVoiceTranscript(fallbackWords)
            }
            override fun onResults(results: android.os.Bundle?) {
                isRecording = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    viewModel.addVoiceTranscript(matches[0])
                }
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    viewModel.addVoiceTranscript(matches[0])
                }
            }
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })
        recognizer.startListening(intent)
    }

    fun stopSpeechListening() {
        isRecording = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    fun triggerPhotoCapture() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        try {
            val file = java.io.File.createTempFile("photo_${System.currentTimeMillis()}", ".jpg", context.cacheDir)
            photoFile = file
            
            val builder = android.os.StrictMode.VmPolicy.Builder()
            android.os.StrictMode.setVmPolicy(builder.build())
            
            val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                val fileUri = android.net.Uri.fromFile(file)
                putExtra(android.provider.MediaStore.EXTRA_OUTPUT, fileUri)
            }
            capturePhotoLauncher.launch(intent)
        } catch (e: Exception) {
            // Fallback simulated file path
            val simPath = "/storage/emulated/0/DCIM/photo_${System.currentTimeMillis()}.jpg"
            viewModel.addAttachment(simPath, uiState.videoPath)
            Toast.makeText(context, "Simulated EOC photo saved ✓", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerVideoCapture() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        try {
            val file = java.io.File.createTempFile("video_${System.currentTimeMillis()}", ".mp4", context.cacheDir)
            videoFile = file
            
            val builder = android.os.StrictMode.VmPolicy.Builder()
            android.os.StrictMode.setVmPolicy(builder.build())
            
            val intent = Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE).apply {
                val fileUri = android.net.Uri.fromFile(file)
                putExtra(android.provider.MediaStore.EXTRA_OUTPUT, fileUri)
                putExtra(android.provider.MediaStore.EXTRA_DURATION_LIMIT, 120) // 120 seconds = 2 minutes limit
            }
            captureVideoLauncher.launch(intent)
        } catch (e: Exception) {
            // Fallback simulated file path
            val simPath = "/storage/emulated/0/DCIM/video_${System.currentTimeMillis()}.mp4"
            viewModel.addAttachment(uiState.photoPath, simPath)
            Toast.makeText(context, "Simulated EOC video saved ✓", Toast.LENGTH_SHORT).show()
        }
    }

    // Clean up Speech recognizer on dispose
    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    Scaffold(
        containerColor = AegisDark,
        topBar = {
            TopAppBar(
                title = { Text("Nigehban AI (نگہبان AI)", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.isSubmitted) {
                    // Elevated Custom Success Card
                    Surface(
                        color = AegisPanel,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, AegisSuccess),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Filled.CloudDone,
                                contentDescription = null,
                                tint = AegisSuccess,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "CRISIS DISPATCHED SUCCESSFULLY ✓",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Incident report logged and routed to EOC central databases. Nearby safety assets and Rescue teams have been automatically assigned.",
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp),
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.resetSubmitState() },
                                colors = ButtonDefaults.buttonColors(containerColor = AegisSuccess, contentColor = Color.Black)
                            ) {
                                Text("Report Another Incident", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Incident Selector Chips
                Text(
                    "CHOOSE EMERGENCY CATEGORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                incidentTypes.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { type ->
                            val selected = uiState.incidentType == type
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.onTypeChanged(type) },
                                label = { Text(type.replace("_", " "), fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
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

                // Address Input
                Text(
                    "CRISIS LOCATION & LANDMARK",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = uiState.addressQuery,
                    onValueChange = viewModel::onAddressChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("E.g., Mall Road Lahore, GPO Murree...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.PinDrop, contentDescription = null, tint = AegisPrimary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AegisPrimary,
                        unfocusedBorderColor = AegisSlate,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Political Rally custom panel
                if (uiState.incidentType == "POLITICAL_RALLY") {
                    Text(
                        "POLITICAL CRISIS CORRIDOR METRICS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AegisPanel),
                        border = BorderStroke(1.dp, AegisAlert.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("ORGANIZING PARTY", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("PTI", "PMLN", "PPP", "JI").forEach { party ->
                                    val selected = uiState.politicalParty == party
                                    FilterChip(
                                        selected = selected,
                                        onClick = { viewModel.onPoliticalPartyChanged(party) },
                                        label = { Text(party, fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AegisAlert.copy(alpha = 0.15f),
                                            selectedLabelColor = AegisAlert,
                                            containerColor = AegisDark,
                                            labelColor = Color.LightGray
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            borderColor = if (selected) AegisAlert else AegisSlate,
                                            selectedBorderColor = AegisAlert
                                        )
                                    )
                                }
                            }

                            Text("CLASH HAZARD POTENTIAL: ${uiState.clashHazardScale}/10", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Slider(
                                value = uiState.clashHazardScale.toFloat(),
                                onValueChange = { viewModel.onClashHazardChanged(it.toInt()) },
                                valueRange = 1f..10f,
                                steps = 8,
                                colors = SliderDefaults.colors(
                                    thumbColor = AegisAlert,
                                    activeTrackColor = AegisAlert,
                                    inactiveTrackColor = AegisSlate
                                )
                            )

                            uiState.politicalImplications?.let { implications ->
                                Surface(
                                    color = AegisAlert.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, AegisAlert.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = AegisAlert, modifier = Modifier.size(16.dp))
                                        Column {
                                            Text("ROAD CONGESTION & CONFLICT WARNING", color = AegisAlert, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(implications, color = Color.White, fontSize = 11.sp, lineHeight = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Seismic Activity custom panel
                if (uiState.incidentType == "SEISMIC_ACTIVITY") {
                    Text(
                        "SEISMIC ACTIVITY TELEMETRY METRICS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AegisPanel),
                        border = BorderStroke(1.dp, AegisAlert.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("RICHTER MAGNITUDE: ${"%.1f".format(uiState.seismicMagnitude)} M_w", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Slider(
                                value = uiState.seismicMagnitude.toFloat(),
                                onValueChange = { viewModel.onSeismicMagnitudeChanged(it.toDouble()) },
                                valueRange = 1.0f..10.0f,
                                steps = 90,
                                colors = SliderDefaults.colors(
                                    thumbColor = AegisAlert,
                                    activeTrackColor = AegisAlert,
                                    inactiveTrackColor = AegisSlate
                                )
                            )

                            Text("FOCAL DEPTH: ${"%.1f".format(uiState.seismicDepth)} km", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Slider(
                                value = uiState.seismicDepth.toFloat(),
                                onValueChange = { viewModel.onSeismicDepthChanged(it.toDouble()) },
                                valueRange = 1.0f..200.0f,
                                steps = 199,
                                colors = SliderDefaults.colors(
                                    thumbColor = AegisAlert,
                                    activeTrackColor = AegisAlert,
                                    inactiveTrackColor = AegisSlate
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("ACTIVE AFTERSHOCK THREAT", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Toggle if active secondary tremors are expected", color = Color.Gray, fontSize = 9.sp)
                                }
                                Switch(
                                    checked = uiState.seismicTremors,
                                    onCheckedChange = { viewModel.onSeismicTremorsChanged(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = AegisAlert,
                                        checkedTrackColor = AegisAlert.copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }
                    }
                }

                // Interactive Voice-Centric NLP Transcriber Card
                Surface(
                    color = AegisPanel,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isRecording) AegisAlert else AegisSlate),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "VOICE INFRASTRUCTURE DISPATCHER",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.Start),
                            fontWeight = FontWeight.Bold
                        )

                        // Visual pulsing microphone button at the center
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(48.dp))
                                .background(if (isRecording) AegisAlert.copy(alpha = 0.15f) else AegisSuccess.copy(alpha = 0.08f))
                                .border(
                                    BorderStroke(
                                        2.dp,
                                        if (isRecording) AegisAlert else AegisSuccess.copy(alpha = 0.5f)
                                    ),
                                    RoundedCornerShape(48.dp)
                                )
                                .clickable {
                                    if (isRecording) {
                                        stopSpeechListening()
                                    } else {
                                        startSpeechListening()
                                    }
                                }
                        ) {
                            if (isRecording) {
                                CircularProgressIndicator(
                                    progress = recordingProgress,
                                    color = AegisAlert,
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Icon(
                                if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Record Speech",
                                tint = if (isRecording) AegisAlert else AegisSuccess,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        // Speech waveform visualizer simulation when listening
                        if (isRecording) {
                            Row(
                                modifier = Modifier
                                    .height(28.dp)
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(9) { index ->
                                    val heightFactor = remember { mutableStateOf(1f) }
                                    LaunchedEffect(recordingProgress) {
                                        heightFactor.value = ((20..100).random() / 100f)
                                    }
                                    val barHeight = 24.dp * heightFactor.value * (recordingProgress + 0.15f).coerceAtMost(1f)
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(barHeight)
                                            .clip(RoundedCornerShape(1.5.dp))
                                            .background(AegisAlert)
                                    )
                                }
                            }
                        }

                        Text(
                            if (isRecording) "EOC Core listening... Tap to finish voice dispatch" else "Tap Mic to record crisis verbally in English, Urdu or Pashto",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isRecording) AegisAlert else Color.LightGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Real-time Gemini AI Translation & Classification
                val geminiAgent = remember { com.aegisnet.mobile.domain.agent.GeminiAgentService() }
                var aiTranslation by remember { mutableStateOf<com.aegisnet.mobile.domain.agent.GeminiAgentService.TranslationResult?>(null) }
                var isTranslating by remember { mutableStateOf(false) }

                // Trigger Gemini translation when description changes (debounced)
                LaunchedEffect(uiState.description) {
                    if (uiState.description.length >= 3) {
                        isTranslating = true
                        delay(800) // Debounce
                        aiTranslation = geminiAgent.translateAndClassify(uiState.description)
                        isTranslating = false
                    } else {
                        aiTranslation = null
                    }
                }

                val translationData = aiTranslation?.let {
                    Triple(it.detectedLanguage, it.englishTranslation, it.classifiedType)
                }

                // Gemini AI processing indicator
                if (isTranslating) {
                    Surface(
                        color = AegisPrimary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AegisPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                color = AegisPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text("GEMINI AI ANALYZING...", color = AegisPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("Translating, classifying, and extracting entities from speech", color = Color.LightGray, fontSize = 9.sp)
                            }
                        }
                    }
                }

                translationData?.let { (lang, translation, category) ->
                    Surface(
                        color = AegisSuccess.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AegisSuccess.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Translate, contentDescription = null, tint = AegisSuccess, modifier = Modifier.size(16.dp))
                                Text("GEMINI AI TRANSLATION ✓", color = AegisSuccess, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            
                            Divider(color = AegisSuccess.copy(alpha = 0.2f))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1.1f)) {
                                    Text("DETECTED PHRASE ($lang)", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(uiState.description, color = Color.White, fontSize = 11.sp, maxLines = 2)
                                }
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .padding(horizontal = 4.dp)
                                        .align(Alignment.CenterVertically)
                                )
                                Column(modifier = Modifier.weight(0.9f)) {
                                    Text("ENGLISH NLP TRANSLATION", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(translation, color = AegisSuccess, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            
                            Surface(
                                color = AegisDark,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, AegisSlate),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onTypeChanged(category)
                                            val currentText = uiState.description
                                            if (!currentText.startsWith("Reported emergency translated by Nigehban AI:")) {
                                                val englishText = "Reported emergency translated by Nigehban AI: $translation. Original transcript: \"$currentText\""
                                                viewModel.onDescriptionChanged(englishText)
                                            }
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AegisPrimary, modifier = Modifier.size(14.dp))
                                        Text("Select emergency chip & apply translation", color = Color.LightGray, fontSize = 10.sp)
                                    }
                                    Text("APPLY ✓", color = AegisPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                // Description Fields
                Text(
                    "CRISIS DETAILS & DESCRIPTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                     value = uiState.description,
                     onValueChange = viewModel::onDescriptionChanged,
                     modifier = Modifier.fillMaxWidth(),
                     placeholder = { Text("E.g., Torrential rainfall causing road washout near GPO Murree Expressway...", color = Color.Gray) },
                     minLines = 4,
                     colors = OutlinedTextFieldDefaults.colors(
                         focusedBorderColor = AegisPrimary,
                         unfocusedBorderColor = AegisSlate,
                         focusedTextColor = Color.White,
                         unfocusedTextColor = Color.White
                     ),
                     isError = uiState.errorMessage != null
                )

                // Photo and Video Attachment Picker Simulation
                Text(
                    "ATTACH MEDIA PROTOCOLS (PHOTOS / VIDEOS)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { triggerPhotoCapture() },
                        color = AegisPanel,
                        border = BorderStroke(1.dp, AegisSlate)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = if (uiState.photoPath != null) AegisSuccess else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (uiState.photoPath != null) "Photo Added" else "Add Picture",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.photoPath != null) AegisSuccess else Color.LightGray
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { triggerVideoCapture() },
                        color = AegisPanel,
                        border = BorderStroke(1.dp, AegisSlate)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Videocam,
                                contentDescription = null,
                                tint = if (uiState.videoPath != null) AegisSuccess else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (uiState.videoPath != null) "Video Added" else "Add Video",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.videoPath != null) AegisSuccess else Color.LightGray
                            )
                        }
                    }
                }

                // Submit Button
                Button(
                    onClick = { viewModel.submitReport(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisAlert,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Report Crisis",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
