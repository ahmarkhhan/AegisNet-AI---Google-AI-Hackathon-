package com.aegisnet.mobile.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegisnet.mobile.data.repository.AlertRepository
import com.aegisnet.mobile.domain.agent.AgentLog
import com.aegisnet.mobile.domain.agent.LocalCrisisOrchestratorAgent
import com.aegisnet.mobile.domain.agent.LogType
import com.aegisnet.mobile.domain.agent.EocAgentOrchestrator
import com.aegisnet.mobile.domain.model.CrisisAlert
import com.aegisnet.mobile.domain.model.DroneStatus
import com.aegisnet.mobile.domain.model.EventSignal
import com.aegisnet.mobile.domain.model.WeatherTelemetry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val alerts: List<CrisisAlert> = emptyList(),
    val signals: List<EventSignal> = emptyList(),
    val drones: List<DroneStatus> = emptyList(),
    val weather: WeatherTelemetry? = null,
    val agentLogs: List<AgentLog> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: AlertRepository,
    private val localAgent: LocalCrisisOrchestratorAgent,
    private val orchestrator: EocAgentOrchestrator
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // Expose verified tweets scraper stream
    val tweets = orchestrator.tweets

    private val _coordinates = MutableStateFlow(Pair(33.6007, 73.0679))
    val coordinates = _coordinates.asStateFlow()

    fun updateCoordinates(lat: Double, lng: Double) {
        _coordinates.value = Pair(lat, lng)
    }

    init {
        // Collect agent logs flow
        viewModelScope.launch {
            localAgent.agentLogs.collect { logs ->
                _uiState.update { it.copy(agentLogs = logs) }
            }
        }
        loadData()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            _coordinates.flatMapLatest { coords ->
                combine(
                    repository.getActiveAlerts(),
                    repository.getRecentSignals(),
                    repository.getDroneStatuses(),
                    repository.getLiveWeather(coords.first, coords.second)
                ) { alerts, signals, drones, weather ->
                    // Run on-device offline reasoning based on incoming signal telemetry
                    localAgent.runOfflineReasoning(alerts, signals, drones)
                    
                    DashboardUiState(
                        alerts = alerts,
                        signals = signals,
                        drones = drones,
                        weather = weather,
                        agentLogs = _uiState.value.agentLogs,
                        isLoading = false
                    )
                }
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }.collect { state ->
                _uiState.update { state }
            }
        }
    }

    fun triggerWeatherAnomaly() {
        viewModelScope.launch {
            localAgent.addLog(LogType.INGESTION, "Simulation Ingestion Command: Inject Weather Anomaly")
            localAgent.addLog(LogType.REASONING, "Executing offline Weather Escalation: Wind threshold set to 85km/h.")
            repository.triggerWeatherAnomaly()
            _uiState.update { it.copy(snackbarMessage = "Weather Anomaly Injected ✓") }
        }
    }

    fun triggerSocialPanic() {
        viewModelScope.launch {
            localAgent.addLog(LogType.INGESTION, "Simulation Ingestion Command: Inject Social Panic")
            localAgent.addLog(LogType.REASONING, "Executing offline Social Panic rule check: Scanning keyword density.")
            repository.triggerSocialPanic()
            _uiState.update { it.copy(snackbarMessage = "Social Panic Injected ✓") }
        }
    }

    fun getNdmaFloodTelemetry(lat: Double, lng: Double, placeName: String?): String {
        return repository.getNdmaFloodTelemetry(lat, lng, placeName)
    }

    fun onSnackbarDismissed() = _uiState.update { it.copy(snackbarMessage = null) }
}
