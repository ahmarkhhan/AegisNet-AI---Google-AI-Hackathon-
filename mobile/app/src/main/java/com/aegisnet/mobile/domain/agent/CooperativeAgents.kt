package com.aegisnet.mobile.domain.agent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class AgentRole {
    SOCIAL_SIGNAL,
    RESCUE_DISPATCH,
    PERIMETER_GEOMETRY,
    MESH_BROADCAST
}

data class EocAgent(
    val role: AgentRole,
    val name: String,
    val description: String,
    val avatarEmoji: String,
    val status: String, // "IDLE", "ANALYZING", "DISPATCHING", "OFFLINE_ACTIVE"
    val isOfflineCapable: Boolean = true,
    val processedCount: Int = 0,
    val logs: List<String> = emptyList()
)

data class SocialTweet(
    val username: String,
    val handle: String,
    val body: String,
    val timeAgo: String,
    val sentiment: String, // "PANIC", "NEUTRAL", "SAFE"
    val correlatedCrisis: String? = null,
    val correlationReason: String? = null,
    val aiCredibilityScore: Double? = null,
    val aiVerdict: String? = null
)

@Singleton
class EocAgentOrchestrator @Inject constructor(
    private val geminiAgent: GeminiAgentService
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val isAiOnline: Boolean get() = geminiAgent.isAvailable

    private val _agents = MutableStateFlow<Map<AgentRole, EocAgent>>(
        mapOf(
            AgentRole.SOCIAL_SIGNAL to EocAgent(
                role = AgentRole.SOCIAL_SIGNAL,
                name = "Gemini Social Verifier Agent",
                description = "AI-powered social signal credibility analysis using Gemini 2.0 Flash.",
                avatarEmoji = "🤖",
                status = "IDLE",
                logs = listOf("Gemini AI Agent initialized.", "Ready to verify social signals with LLM reasoning.")
            ),
            AgentRole.RESCUE_DISPATCH to EocAgent(
                role = AgentRole.RESCUE_DISPATCH,
                name = "Gemini Crisis Analyzer Agent",
                description = "Analyzes crisis reports and recommends severity, actions, and affected estimates.",
                avatarEmoji = "🧠",
                status = "IDLE",
                logs = listOf("Crisis Analyzer online.", "Awaiting incident reports for AI triage.")
            ),
            AgentRole.PERIMETER_GEOMETRY to EocAgent(
                role = AgentRole.PERIMETER_GEOMETRY,
                name = "Gemini Escalation Predictor",
                description = "Predicts crisis escalation using correlated multi-signal analysis.",
                avatarEmoji = "⚡",
                status = "IDLE",
                logs = listOf("Escalation model loaded.", "Monitoring for multi-signal correlation patterns.")
            ),
            AgentRole.MESH_BROADCAST to EocAgent(
                role = AgentRole.MESH_BROADCAST,
                name = "Gemini NLP Translator Agent",
                description = "Real-time Urdu/Pashto/English translation and incident classification.",
                avatarEmoji = "🌐",
                status = "IDLE",
                logs = listOf("Multilingual NLP agent ready.", "Supports English, Urdu, Pashto crisis transcription.")
            )
        )
    )
    val agents: StateFlow<Map<AgentRole, EocAgent>> = _agents.asStateFlow()

    private val _tweets = MutableStateFlow<List<SocialTweet>>(
        listOf(
            SocialTweet("@RawalpindiAlerts", "pindi_eoc", "Severe pooling on Murree Road! Water levels rising rapidly, avoid low underpasses! #RawalpindiFlood", "2m ago", "PANIC", "Emergency: FLOOD WATER", "Vetted geolocation: matches Rawalpindi incident within 1.2km."),
            SocialTweet("@MurreeExpress", "murree_exp", "Unbelievable heavy snow near GPO. Over 2 feet accumulated. Snow blowers deployed. Keep warm.", "10m ago", "NEUTRAL", "Murree Snowstorm Entrapment", "Topic matches Murree snow warning corridor."),
            SocialTweet("@CitizenPak", "cit_pak", "Rescue 1122 team just arrived here. Super fast response even during this storm! Thank you!", "15m ago", "SAFE")
        )
    )
    val tweets: StateFlow<List<SocialTweet>> = _tweets.asStateFlow()

    // ── AI Analysis results exposed as state ──
    private val _latestCrisisAnalysis = MutableStateFlow<GeminiAgentService.CrisisAnalysis?>(null)
    val latestCrisisAnalysis: StateFlow<GeminiAgentService.CrisisAnalysis?> = _latestCrisisAnalysis.asStateFlow()

    private val _latestEscalation = MutableStateFlow<GeminiAgentService.EscalationPrediction?>(null)
    val latestEscalation: StateFlow<GeminiAgentService.EscalationPrediction?> = _latestEscalation.asStateFlow()

    private val _latestTranslation = MutableStateFlow<GeminiAgentService.TranslationResult?>(null)
    val latestTranslation: StateFlow<GeminiAgentService.TranslationResult?> = _latestTranslation.asStateFlow()

    private val _latestResourceAdvice = MutableStateFlow<GeminiAgentService.ResourceRecommendation?>(null)
    val latestResourceAdvice: StateFlow<GeminiAgentService.ResourceRecommendation?> = _latestResourceAdvice.asStateFlow()

    private val _isAgentProcessing = MutableStateFlow(false)
    val isAgentProcessing: StateFlow<Boolean> = _isAgentProcessing.asStateFlow()

    /**
     * Trigger real Gemini AI social signal verification.
     */
    fun triggerSocialAnalysis(anomalyType: String, areaName: String) {
        val timestamp = getFormattedTime()

        val generatedTweet = SocialTweet(
            username = if (anomalyType.contains("FLOOD")) "@ResiliencePindi" else "@SeismicPulsePK",
            handle = if (anomalyType.contains("FLOOD")) "res_pindi" else "seismic_pk",
            body = "Emergency alert in $areaName: $anomalyType situation developing rapidly. Stay safe! #CrisisPK",
            timeAgo = "Just now",
            sentiment = "PANIC",
            correlatedCrisis = "Emergency: $anomalyType",
            correlationReason = "Matches $anomalyType incident sector $areaName."
        )

        _tweets.update { listOf(generatedTweet) + it }

        // Launch real Gemini verification
        scope.launch {
            updateAgentStatus(AgentRole.SOCIAL_SIGNAL, "ANALYZING")
            addAgentLog(AgentRole.SOCIAL_SIGNAL, "[$timestamp] 🧠 Gemini AI: Verifying social signal credibility...")

            val verification = geminiAgent.verifySocialSignal(
                tweetText = generatedTweet.body,
                handle = generatedTweet.handle,
                location = areaName
            )

            // Update the tweet with AI verification results
            _tweets.update { currentTweets ->
                currentTweets.map { tweet ->
                    if (tweet == generatedTweet) {
                        tweet.copy(
                            aiCredibilityScore = verification.credibilityScore,
                            aiVerdict = verification.verdict,
                            correlationReason = "🤖 AI: ${verification.reasoning}"
                        )
                    } else tweet
                }
            }

            addAgentLog(AgentRole.SOCIAL_SIGNAL,
                "[$timestamp] ✅ AI Verdict: ${verification.verdict} (${String.format("%.0f", verification.credibilityScore * 100)}% credible)")
            addAgentLog(AgentRole.SOCIAL_SIGNAL,
                "[$timestamp] 💬 AI Reasoning: ${verification.reasoning}")
            updateAgentStatus(AgentRole.SOCIAL_SIGNAL, "IDLE", incrementCount = true)
        }
    }

    /**
     * Trigger real Gemini AI crisis analysis on rescue dispatch.
     */
    fun triggerRescueDispatch(
        latitude: Double,
        longitude: Double,
        incidentName: String,
        nearbyHospitals: List<String> = emptyList(),
        nearbyPolice: List<String> = emptyList()
    ) {
        val timestamp = getFormattedTime()

        scope.launch {
            _isAgentProcessing.value = true
            updateAgentStatus(AgentRole.RESCUE_DISPATCH, "ANALYZING")
            addAgentLog(AgentRole.RESCUE_DISPATCH,
                "[$timestamp] 🧠 Gemini AI: Analyzing crisis at [${"%.4f".format(latitude)}, ${"%.4f".format(longitude)}]...")

            val analysis = geminiAgent.analyzeCrisisReport(
                description = "$incidentName at coordinates $latitude, $longitude. Nearby hospitals: ${nearbyHospitals.joinToString()}. Police: ${nearbyPolice.joinToString()}.",
                location = "${"%.4f".format(latitude)}°N, ${"%.4f".format(longitude)}°E",
                type = incidentName
            )

            _latestCrisisAnalysis.value = analysis

            addAgentLog(AgentRole.RESCUE_DISPATCH,
                "[$timestamp] ✅ AI Severity: ${analysis.severity} (${analysis.confidenceScore}% confidence)")
            addAgentLog(AgentRole.RESCUE_DISPATCH,
                "[$timestamp] 📊 AI Summary: ${analysis.summary}")
            addAgentLog(AgentRole.RESCUE_DISPATCH,
                "[$timestamp] 🎯 AI Actions: ${analysis.recommendedActions.joinToString(" | ")}")
            addAgentLog(AgentRole.RESCUE_DISPATCH,
                "[$timestamp] 👥 Estimated Affected: ${analysis.estimatedAffected}")

            updateAgentStatus(AgentRole.RESCUE_DISPATCH, "DISPATCHING", incrementCount = true)
            _isAgentProcessing.value = false
        }
    }

    /**
     * Trigger real Gemini AI escalation prediction.
     */
    fun triggerPerimeterGeometry(latitude: Double, longitude: Double) {
        val timestamp = getFormattedTime()

        scope.launch {
            updateAgentStatus(AgentRole.PERIMETER_GEOMETRY, "ANALYZING")
            addAgentLog(AgentRole.PERIMETER_GEOMETRY,
                "[$timestamp] 🧠 Gemini AI: Running escalation prediction model...")

            val prediction = geminiAgent.predictEscalation(
                activeAlertsSummary = "Active crisis at ${"%.4f".format(latitude)}°N, ${"%.4f".format(longitude)}°E",
                recentSignalsSummary = "Multiple social panic signals, weather anomaly detected in area."
            )

            _latestEscalation.value = prediction

            val escIcon = if (prediction.willEscalate) "🔴" else "🟢"
            addAgentLog(AgentRole.PERIMETER_GEOMETRY,
                "[$timestamp] $escIcon Escalation: ${if (prediction.willEscalate) "YES" else "NO"} (${prediction.escalationProbability}% probability)")
            addAgentLog(AgentRole.PERIMETER_GEOMETRY,
                "[$timestamp] 📈 Scenario: ${prediction.predictedScenario}")
            addAgentLog(AgentRole.PERIMETER_GEOMETRY,
                "[$timestamp] 🛡️ Preventive: ${prediction.preventiveActions.joinToString(" | ")}")

            updateAgentStatus(AgentRole.PERIMETER_GEOMETRY, "IDLE", incrementCount = true)
        }
    }

    /**
     * Trigger real Gemini AI NLP translation and classification.
     */
    fun triggerMeshBroadcast(incidentType: String, severity: String) {
        val timestamp = getFormattedTime()

        scope.launch {
            updateAgentStatus(AgentRole.MESH_BROADCAST, "ANALYZING")
            addAgentLog(AgentRole.MESH_BROADCAST,
                "[$timestamp] 🧠 Gemini AI: Translating and classifying verbal report...")

            val translation = geminiAgent.translateAndClassify(incidentType)
            _latestTranslation.value = translation

            addAgentLog(AgentRole.MESH_BROADCAST,
                "[$timestamp] 🌐 Language: ${translation.detectedLanguage}")
            addAgentLog(AgentRole.MESH_BROADCAST,
                "[$timestamp] 🇬🇧 English: ${translation.englishTranslation}")
            addAgentLog(AgentRole.MESH_BROADCAST,
                "[$timestamp] 🇵🇰 Urdu: ${translation.urduTranslation}")
            addAgentLog(AgentRole.MESH_BROADCAST,
                "[$timestamp] 📋 Type: ${translation.classifiedType} | Urgency: ${translation.urgencyLevel}")

            updateAgentStatus(AgentRole.MESH_BROADCAST, "IDLE", incrementCount = true)
        }
    }

    /**
     * Trigger Gemini AI resource allocation recommendation.
     */
    fun triggerResourceAdvice(crisisType: String, severity: String, location: String, depots: List<String>) {
        scope.launch {
            _isAgentProcessing.value = true
            val recommendation = geminiAgent.recommendResourceAllocation(crisisType, severity, location, depots)
            _latestResourceAdvice.value = recommendation
            _isAgentProcessing.value = false
        }
    }

    // ── Helper functions ──

    private fun updateAgentStatus(role: AgentRole, status: String, incrementCount: Boolean = false) {
        _agents.update { currentMap ->
            val agent = currentMap[role] ?: return@update currentMap
            currentMap + (role to agent.copy(
                status = status,
                processedCount = if (incrementCount) agent.processedCount + 1 else agent.processedCount
            ))
        }
    }

    private fun addAgentLog(role: AgentRole, log: String) {
        _agents.update { currentMap ->
            val agent = currentMap[role] ?: return@update currentMap
            currentMap + (role to agent.copy(
                logs = (listOf(log) + agent.logs).take(25)
            ))
        }
    }

    private fun getFormattedTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }
}
