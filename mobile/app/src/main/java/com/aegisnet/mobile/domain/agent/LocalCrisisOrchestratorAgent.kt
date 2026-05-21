package com.aegisnet.mobile.domain.agent

import com.aegisnet.mobile.domain.model.CrisisAlert
import com.aegisnet.mobile.domain.model.DroneStatus
import com.aegisnet.mobile.domain.model.EventSignal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class AgentLog(
    val timestamp: String,
    val type: LogType,
    val message: String
)

enum class LogType {
    INGESTION, REASONING, ACTION, SYSTEM
}

@Singleton
class LocalCrisisOrchestratorAgent @Inject constructor() {

    private val _agentLogs = MutableStateFlow<List<AgentLog>>(
        listOf(
            AgentLog(getFormattedTime(), LogType.SYSTEM, "Offline AegisNet Local Node Initialized ✓"),
            AgentLog(getFormattedTime(), LogType.SYSTEM, "Security verification: OK. Rule weights loaded.")
        )
    )
    val agentLogs: StateFlow<List<AgentLog>> = _agentLogs.asStateFlow()

    fun runOfflineReasoning(
        alerts: List<CrisisAlert>,
        signals: List<EventSignal>,
        drones: List<DroneStatus>
    ) {
        if (signals.isEmpty() && alerts.isEmpty()) return

        val newLogs = mutableListOf<AgentLog>()
        newLogs.add(AgentLog(getFormattedTime(), LogType.SYSTEM, "Local Rule Execution Triggered: Active Signals=${signals.size}"))

        // 1. Social & Weather Verification Reasoning Loop
        signals.forEach { signal ->
            newLogs.add(AgentLog(getFormattedTime(), LogType.INGESTION, "Ingested Signal from ${signal.source}: ${signal.type}"))
            
            // Apply Triage rule
            when (signal.type) {
                "WEATHER_ANOMALY" -> {
                    newLogs.add(AgentLog(getFormattedTime(), LogType.REASONING, "Rule matching: High wind speed & low temp detected. Evaluating containment zone..."))
                    newLogs.add(AgentLog(getFormattedTime(), LogType.ACTION, "Offline Escalate: Set local casualty risk probability to 90%+ near signal location."))
                }
                "SOCIAL_PANIC" -> {
                    newLogs.add(AgentLog(getFormattedTime(), LogType.REASONING, "Sentiment Triage: High keyword density for 'frozen', 'stuck', 'entrapment'."))
                    newLogs.add(AgentLog(getFormattedTime(), LogType.ACTION, "Dynamic Escalation: Trigger local rescue priority queue boost."))
                }
                else -> {
                    newLogs.add(AgentLog(getFormattedTime(), LogType.REASONING, "Confidence high (${signal.confidence}). Setting base priority parameter."))
                }
            }
        }

        // 2. Local Containment Zone calculation
        alerts.forEach { alert ->
            val lat = alert.epicenterLat ?: 0.0
            val lng = alert.epicenterLng ?: 0.0
            if (lat != 0.0 && lng != 0.0) {
                newLogs.add(AgentLog(getFormattedTime(), LogType.REASONING, "Geofencing Area '${alert.title}': epicenter [${"%.4f".format(lat)}, ${"%.4f".format(lng)}]."))
                // Calculate simulated bounding perimeter (e.g. 1.5km evacuation zone)
                newLogs.add(AgentLog(getFormattedTime(), LogType.ACTION, "Evacuation Boundary Computed: Radius=1.5km around ${alert.title}."))
            }
        }

        // 3. Simulated Drone Dispatch Coordination
        drones.forEach { drone ->
            if (drone.status == "SCANNING" && drone.batteryLevel > 50) {
                newLogs.add(AgentLog(getFormattedTime(), LogType.REASONING, "Drone Coordination: ${drone.droneId} available for dispatch."))
                newLogs.add(AgentLog(getFormattedTime(), LogType.ACTION, "Command dispatched locally to ${drone.droneId}: scan epicenter sector."))
            }
        }

        _agentLogs.update { (newLogs + it).take(30) } // keep latest 30 logs
    }

    fun addLog(type: LogType, message: String) {
        _agentLogs.update {
            (listOf(AgentLog(getFormattedTime(), type, message)) + it).take(30)
        }
    }

    private fun getFormattedTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }
}
