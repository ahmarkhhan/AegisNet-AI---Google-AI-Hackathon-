package com.aegisnet.core.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Structured agent log event for the ReAct reasoning pipeline.
 * Phases: PERCEPTION, REASONING, TOOL_CALL, ACTION, RESULT, ERROR, FALLBACK
 */
public class AgentLogEvent {

    private String agentId;       // "SOCIAL_SIGNAL", "CRISIS_ANALYZER", etc.
    private String agentName;     // Human-readable name
    private String phase;         // "PERCEPTION", "REASONING", "TOOL_CALL", "ACTION", "RESULT", "ERROR", "FALLBACK"
    private String message;       // The log content
    private String toolName;      // If TOOL_CALL: "OpenMeteo", "GDELT", etc.
    private Map<String, Object> metadata; // Severity scores, coordinates, etc.
    private LocalDateTime timestamp;

    public AgentLogEvent() {}

    public AgentLogEvent(String agentId, String agentName, String phase, String message) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.phase = phase;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public AgentLogEvent(String agentId, String agentName, String phase, String message, String toolName, Map<String, Object> metadata) {
        this(agentId, agentName, phase, message);
        this.toolName = toolName;
        this.metadata = metadata;
    }

    // Getters and Setters
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
