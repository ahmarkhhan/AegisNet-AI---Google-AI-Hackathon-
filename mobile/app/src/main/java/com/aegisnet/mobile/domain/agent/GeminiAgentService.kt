package com.aegisnet.mobile.domain.agent

import android.util.Log
import com.aegisnet.mobile.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GeminiAgentService — Real AI Agent powered by Google Gemini 2.0 Flash.
 *
 * This service replaces all hardcoded "agent" logic with actual LLM calls to Gemini.
 * Each function represents a distinct AI agent with a specialized system prompt.
 *
 * Agents:
 * 1. Crisis Analyzer — Evaluates crisis severity and recommends actions
 * 2. Social Verifier — Scores tweet credibility (0.0-1.0)
 * 3. Escalation Predictor — Predicts if a crisis will escalate
 * 4. NLP Translator — Translates Urdu/Pashto/English and classifies incident type
 * 5. Resource Advisor — Recommends optimal depot dispatch strategy
 */
@Singleton
class GeminiAgentService @Inject constructor() {

    companion object {
        private const val TAG = "GeminiAgent"
        private const val MODEL_NAME = "gemini-2.0-flash"
    }

    private val apiKey: String = BuildConfig.GEMINI_API_KEY

    val isAvailable: Boolean get() = apiKey.isNotBlank()

    private val model: GenerativeModel? by lazy {
        if (apiKey.isBlank()) {
            Log.w(TAG, "Gemini API key not set. AI agents will use offline fallback.")
            null
        } else {
            GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.7f
                    maxOutputTokens = 1024
                }
            )
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  AGENT 1: Crisis Analyzer
    // ═══════════════════════════════════════════════════════════

    data class CrisisAnalysis(
        val severity: String,           // CRITICAL, HIGH, MEDIUM, LOW
        val confidenceScore: Int,       // 0-100
        val summary: String,            // One-line AI summary
        val recommendedActions: List<String>,
        val estimatedAffected: String,  // e.g. "~450 households"
        val reasoning: String           // AI's reasoning chain
    )

    suspend fun analyzeCrisisReport(
        description: String,
        location: String,
        type: String
    ): CrisisAnalysis = withContext(Dispatchers.IO) {
        val prompt = """
            You are the Nigehban AI Crisis Analyzer Agent for Pakistan's Emergency Operations Center.
            
            Analyze this crisis report and provide a structured assessment:
            
            CRISIS REPORT:
            - Description: $description
            - Location: $location
            - Reported Type: $type
            
            Respond in EXACTLY this format (no markdown, no extra text):
            SEVERITY: [CRITICAL/HIGH/MEDIUM/LOW]
            CONFIDENCE: [0-100]
            SUMMARY: [One line summary]
            AFFECTED: [Estimated affected population/infrastructure]
            ACTIONS: [Action 1] | [Action 2] | [Action 3]
            REASONING: [Your reasoning in 2-3 sentences]
        """.trimIndent()

        try {
            val response = model?.generateContent(prompt)
            val text = response?.text ?: throw Exception("Empty response")
            parseCrisisAnalysis(text)
        } catch (e: Exception) {
            Log.e(TAG, "Crisis analysis failed: ${e.message}")
            fallbackCrisisAnalysis(description, type)
        }
    }

    private fun parseCrisisAnalysis(text: String): CrisisAnalysis {
        val lines = text.lines().associate { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) parts[0].trim().uppercase() to parts[1].trim()
            else "" to ""
        }
        return CrisisAnalysis(
            severity = lines["SEVERITY"] ?: "HIGH",
            confidenceScore = lines["CONFIDENCE"]?.toIntOrNull() ?: 75,
            summary = lines["SUMMARY"] ?: "Crisis detected — AI analysis pending.",
            recommendedActions = (lines["ACTIONS"] ?: "Evacuate area | Deploy rescue teams | Alert hospitals")
                .split("|").map { it.trim() },
            estimatedAffected = lines["AFFECTED"] ?: "Unknown — requires field verification",
            reasoning = lines["REASONING"] ?: "Insufficient data for full reasoning chain."
        )
    }

    private fun fallbackCrisisAnalysis(description: String, type: String): CrisisAnalysis {
        val severity = when {
            type.contains("SEISMIC", ignoreCase = true) -> "CRITICAL"
            type.contains("FLOOD", ignoreCase = true) -> "CRITICAL"
            type.contains("FIRE", ignoreCase = true) -> "HIGH"
            else -> "MEDIUM"
        }
        return CrisisAnalysis(
            severity = severity,
            confidenceScore = 60,
            summary = "[Offline] Rule-based triage: $type incident detected.",
            recommendedActions = listOf("Deploy nearest rescue team", "Alert hospitals", "Establish perimeter"),
            estimatedAffected = "Requires field assessment",
            reasoning = "[Offline fallback] Gemini unavailable. Using rule-based classification for $type."
        )
    }

    // ═══════════════════════════════════════════════════════════
    //  AGENT 2: Social Signal Verifier
    // ═══════════════════════════════════════════════════════════

    data class SocialVerification(
        val credibilityScore: Double,   // 0.0 to 1.0
        val verdict: String,            // VERIFIED, LIKELY_TRUE, UNCERTAIN, LIKELY_FALSE, SPAM
        val reasoning: String,
        val sentiment: String           // PANIC, NEUTRAL, SAFE
    )

    suspend fun verifySocialSignal(
        tweetText: String,
        handle: String,
        location: String? = null
    ): SocialVerification = withContext(Dispatchers.IO) {
        val prompt = """
            You are the Nigehban AI Social Signal Verification Agent.
            
            Evaluate this social media post for crisis credibility:
            
            POST: "$tweetText"
            AUTHOR: $handle
            LOCATION CONTEXT: ${location ?: "Unknown"}
            
            Consider: Is this a real emergency report? Is it panic exaggeration? Is it spam/bot?
            
            Respond in EXACTLY this format (no markdown):
            CREDIBILITY: [0.0 to 1.0]
            VERDICT: [VERIFIED/LIKELY_TRUE/UNCERTAIN/LIKELY_FALSE/SPAM]
            SENTIMENT: [PANIC/NEUTRAL/SAFE]
            REASONING: [2-3 sentence explanation]
        """.trimIndent()

        try {
            val response = model?.generateContent(prompt)
            val text = response?.text ?: throw Exception("Empty response")
            parseSocialVerification(text)
        } catch (e: Exception) {
            Log.e(TAG, "Social verification failed: ${e.message}")
            SocialVerification(
                credibilityScore = 0.5,
                verdict = "UNCERTAIN",
                reasoning = "[Offline] Cannot verify — Gemini unavailable.",
                sentiment = if (tweetText.contains("!") || tweetText.uppercase() == tweetText) "PANIC" else "NEUTRAL"
            )
        }
    }

    private fun parseSocialVerification(text: String): SocialVerification {
        val lines = text.lines().associate { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) parts[0].trim().uppercase() to parts[1].trim()
            else "" to ""
        }
        return SocialVerification(
            credibilityScore = lines["CREDIBILITY"]?.toDoubleOrNull() ?: 0.5,
            verdict = lines["VERDICT"] ?: "UNCERTAIN",
            reasoning = lines["REASONING"] ?: "Unable to parse AI reasoning.",
            sentiment = lines["SENTIMENT"] ?: "NEUTRAL"
        )
    }

    // ═══════════════════════════════════════════════════════════
    //  AGENT 3: Escalation Predictor
    // ═══════════════════════════════════════════════════════════

    data class EscalationPrediction(
        val willEscalate: Boolean,
        val escalationProbability: Int,  // 0-100
        val predictedScenario: String,
        val preventiveActions: List<String>,
        val reasoning: String
    )

    suspend fun predictEscalation(
        activeAlertsSummary: String,
        recentSignalsSummary: String
    ): EscalationPrediction = withContext(Dispatchers.IO) {
        val prompt = """
            You are the Nigehban AI Predictive Escalation Agent for Pakistan's EOC.
            
            Based on the following active crisis data, predict if the situation will escalate:
            
            ACTIVE ALERTS: $activeAlertsSummary
            RECENT SIGNALS: $recentSignalsSummary
            
            Consider: secondary effects (aftershocks, dam overflow, traffic gridlock), weather forecasts, population density, and historical patterns in Pakistan.
            
            Respond in EXACTLY this format (no markdown):
            WILL_ESCALATE: [YES/NO]
            PROBABILITY: [0-100]
            SCENARIO: [Predicted escalation scenario in one sentence]
            ACTIONS: [Preventive action 1] | [Preventive action 2] | [Preventive action 3]
            REASONING: [2-3 sentence explanation]
        """.trimIndent()

        try {
            val response = model?.generateContent(prompt)
            val text = response?.text ?: throw Exception("Empty response")
            parseEscalationPrediction(text)
        } catch (e: Exception) {
            Log.e(TAG, "Escalation prediction failed: ${e.message}")
            EscalationPrediction(
                willEscalate = false,
                escalationProbability = 40,
                predictedScenario = "[Offline] Unable to predict — using conservative estimate.",
                preventiveActions = listOf("Monitor situation", "Keep rescue teams on standby"),
                reasoning = "[Offline fallback] Gemini unavailable."
            )
        }
    }

    private fun parseEscalationPrediction(text: String): EscalationPrediction {
        val lines = text.lines().associate { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) parts[0].trim().uppercase() to parts[1].trim()
            else "" to ""
        }
        return EscalationPrediction(
            willEscalate = lines["WILL_ESCALATE"]?.uppercase()?.contains("YES") == true,
            escalationProbability = lines["PROBABILITY"]?.toIntOrNull() ?: 40,
            predictedScenario = lines["SCENARIO"] ?: "Unable to predict.",
            preventiveActions = (lines["ACTIONS"] ?: "Monitor closely")
                .split("|").map { it.trim() },
            reasoning = lines["REASONING"] ?: "No reasoning available."
        )
    }

    // ═══════════════════════════════════════════════════════════
    //  AGENT 4: NLP Translator & Classifier
    // ═══════════════════════════════════════════════════════════

    data class TranslationResult(
        val originalText: String,
        val detectedLanguage: String,   // English, Urdu, Pashto
        val englishTranslation: String,
        val urduTranslation: String,
        val classifiedType: String,     // SEISMIC_ACTIVITY, FLOOD_WATER, etc.
        val extractedEntities: List<String>,  // location names, numbers, etc.
        val urgencyLevel: String        // IMMEDIATE, HIGH, MODERATE, LOW
    )

    suspend fun translateAndClassify(
        spokenText: String
    ): TranslationResult = withContext(Dispatchers.IO) {
        val prompt = """
            You are the Nigehban AI Multilingual Crisis NLP Agent for Pakistan.
            
            A citizen has reported a crisis verbally. Analyze this speech transcript:
            
            TRANSCRIPT: "$spokenText"
            
            Tasks:
            1. Detect the language (English, Urdu, Pashto, or mixed)
            2. Translate to both English and Urdu
            3. Classify the incident type
            4. Extract key entities (locations, numbers, severity keywords)
            5. Assess urgency
            
            Valid incident types: SEISMIC_ACTIVITY, FLOOD_WATER, FIRE, POLITICAL_RALLY, TRAFFIC_BLOCKAGE, MEDICAL_EMERGENCY, BUILDING_COLLAPSE, SNOW_STORM, LANDSLIDE, OTHER
            
            Respond in EXACTLY this format (no markdown):
            LANGUAGE: [detected language]
            ENGLISH: [English translation]
            URDU: [Urdu translation in Urdu script]
            TYPE: [incident type from list above]
            ENTITIES: [entity1] | [entity2] | [entity3]
            URGENCY: [IMMEDIATE/HIGH/MODERATE/LOW]
        """.trimIndent()

        try {
            val response = model?.generateContent(prompt)
            val text = response?.text ?: throw Exception("Empty response")
            parseTranslationResult(spokenText, text)
        } catch (e: Exception) {
            Log.e(TAG, "Translation failed: ${e.message}")
            fallbackTranslation(spokenText)
        }
    }

    private fun parseTranslationResult(original: String, text: String): TranslationResult {
        val lines = text.lines().associate { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) parts[0].trim().uppercase() to parts[1].trim()
            else "" to ""
        }
        return TranslationResult(
            originalText = original,
            detectedLanguage = lines["LANGUAGE"] ?: "Unknown",
            englishTranslation = lines["ENGLISH"] ?: original,
            urduTranslation = lines["URDU"] ?: "",
            classifiedType = lines["TYPE"] ?: "OTHER",
            extractedEntities = (lines["ENTITIES"] ?: "")
                .split("|").map { it.trim() }.filter { it.isNotBlank() },
            urgencyLevel = lines["URGENCY"] ?: "MODERATE"
        )
    }

    private fun fallbackTranslation(spokenText: String): TranslationResult {
        val lower = spokenText.lowercase()
        val type = when {
            lower.contains("zalzala") || lower.contains("earthquake") || lower.contains("tremor") -> "SEISMIC_ACTIVITY"
            lower.contains("selab") || lower.contains("flood") || lower.contains("pani") -> "FLOOD_WATER"
            lower.contains("aag") || lower.contains("fire") -> "FIRE"
            lower.contains("jalsa") || lower.contains("dharna") || lower.contains("protest") -> "POLITICAL_RALLY"
            lower.contains("jam") || lower.contains("accident") || lower.contains("traffic") -> "TRAFFIC_BLOCKAGE"
            else -> "OTHER"
        }
        return TranslationResult(
            originalText = spokenText,
            detectedLanguage = "Unknown (offline)",
            englishTranslation = spokenText,
            urduTranslation = "[Offline — Gemini unavailable for translation]",
            classifiedType = type,
            extractedEntities = emptyList(),
            urgencyLevel = "MODERATE"
        )
    }

    // ═══════════════════════════════════════════════════════════
    //  AGENT 5: Resource Allocation Advisor
    // ═══════════════════════════════════════════════════════════

    data class ResourceRecommendation(
        val primaryDepot: String,
        val backupDepot: String?,
        val recommendedUnits: String,
        val strategy: String,
        val reasoning: String
    )

    suspend fun recommendResourceAllocation(
        crisisType: String,
        severity: String,
        location: String,
        availableDepots: List<String>
    ): ResourceRecommendation = withContext(Dispatchers.IO) {
        val prompt = """
            You are the Nigehban AI Resource Allocation Advisor for Pakistan's EOC.
            
            A crisis requires resource dispatch. Recommend the optimal strategy:
            
            CRISIS TYPE: $crisisType
            SEVERITY: $severity
            LOCATION: $location
            AVAILABLE DEPOTS: ${availableDepots.joinToString(", ")}
            
            Consider: proximity to crisis, depot capacity, required equipment for this crisis type, backup contingency.
            
            Respond in EXACTLY this format (no markdown):
            PRIMARY: [Primary depot name]
            BACKUP: [Backup depot name or NONE]
            UNITS: [Recommended units/equipment to dispatch]
            STRATEGY: [One sentence dispatch strategy]
            REASONING: [2-3 sentence explanation]
        """.trimIndent()

        try {
            val response = model?.generateContent(prompt)
            val text = response?.text ?: throw Exception("Empty response")
            parseResourceRecommendation(text)
        } catch (e: Exception) {
            Log.e(TAG, "Resource recommendation failed: ${e.message}")
            ResourceRecommendation(
                primaryDepot = availableDepots.firstOrNull() ?: "Nearest available",
                backupDepot = availableDepots.getOrNull(1),
                recommendedUnits = "Standard rescue team + ambulance",
                strategy = "[Offline] Deploy nearest available resources.",
                reasoning = "[Offline fallback] Gemini unavailable."
            )
        }
    }

    private fun parseResourceRecommendation(text: String): ResourceRecommendation {
        val lines = text.lines().associate { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) parts[0].trim().uppercase() to parts[1].trim()
            else "" to ""
        }
        return ResourceRecommendation(
            primaryDepot = lines["PRIMARY"] ?: "Unknown",
            backupDepot = lines["BACKUP"]?.takeIf { it.uppercase() != "NONE" },
            recommendedUnits = lines["UNITS"] ?: "Standard team",
            strategy = lines["STRATEGY"] ?: "Deploy nearest resources.",
            reasoning = lines["REASONING"] ?: "No reasoning available."
        )
    }
}
