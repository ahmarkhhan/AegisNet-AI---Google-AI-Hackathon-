package com.aegisnet.mobile.ui.dashboard.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisnet.mobile.domain.agent.AgentRole
import com.aegisnet.mobile.domain.agent.EocAgent
import com.aegisnet.mobile.domain.agent.EocAgentOrchestrator
import com.aegisnet.mobile.domain.agent.SocialTweet

@Composable
fun AgentOperationsHubScreen(
    orchestrator: EocAgentOrchestrator
) {
    val agentsState by orchestrator.agents.collectAsState()
    val tweetsState by orchestrator.tweets.collectAsState()
    var selectedRole by remember { mutableStateOf(AgentRole.SOCIAL_SIGNAL) }

    val gradientBg = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF070A13))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBg)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Elite Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.SupportAgent,
                contentDescription = "Agent Hub",
                tint = Color(0xFF00E676),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "GEMINI AI AGENT NETWORK",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (orchestrator.isAiOnline) Color(0xFF00E676) else Color(0xFFFF9800))
                    )
                    Text(
                        if (orchestrator.isAiOnline) "Gemini 2.0 Flash Online • 5 AI Agents Active"
                        else "Offline Mode • Using rule-based fallback agents",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (orchestrator.isAiOnline) Color(0xFF00E676) else Color(0xFFFF9800)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Agents Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            agentsState.values.take(2).forEach { agent ->
                Box(modifier = Modifier.weight(1f)) {
                    AgentCard(
                        agent = agent,
                        isSelected = agent.role == selectedRole,
                        onClick = { selectedRole = agent.role }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            agentsState.values.drop(2).forEach { agent ->
                Box(modifier = Modifier.weight(1f)) {
                    AgentCard(
                        agent = agent,
                        isSelected = agent.role == selectedRole,
                        onClick = { selectedRole = agent.role }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reasoning Console of Selected Agent
        val activeAgent = agentsState[selectedRole]!!
        Text(
            "${activeAgent.name.uppercase()} REASONING CONSOLE",
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            color = Color(0xFF050811),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder(true)
        ) {
            LazyColumn(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (activeAgent.logs.isEmpty()) {
                    item {
                        Text(
                            "Ready. Executing idle loop telemetry sweeps...",
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    items(activeAgent.logs) { log ->
                        Text(
                            "> $log",
                            color = Color(0xFF00E676),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Twitter/X Public Reactions Card Feed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = "Social Activity",
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "DYNAMIC PUBLIC REACTION FEEDS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Surface(
                color = Color(0x330284C7),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Simulated X/Twitter Scraper",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF0284C7),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(tweetsState) { tweet ->
                TweetItem(tweet)
            }
        }
    }
}

@Composable
fun AgentCard(
    agent: EocAgent,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF00E676) else Color(0xFF1E293B)
    val cardBg = if (isSelected) Color(0xFF0E1A2F) else Color(0xFF0B1324)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = cardBg,
        border = CardDefaults.outlinedCardBorder(true).copy(
            brush = Brush.horizontalGradient(listOf(borderColor, borderColor))
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    agent.avatarEmoji,
                    fontSize = 24.sp
                )
                Surface(
                    color = if (agent.status == "OFFLINE_ACTIVE" || agent.status == "DISPATCHING") Color(0x2200E676) else Color(0x2294A3B8),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        agent.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (agent.status == "OFFLINE_ACTIVE" || agent.status == "DISPATCHING") Color(0xFF00E676) else Color.LightGray,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                agent.name,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                agent.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 2,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun TweetItem(tweet: SocialTweet) {
    val sentimentColor = when (tweet.sentiment) {
        "PANIC" -> Color(0xFFEF5350)
        "SAFE" -> Color(0xFF00E676)
        else -> Color(0xFFFDD835)
    }

    Surface(
        color = Color(0xFF0B1324),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder(true),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            tweet.username.take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            tweet.username,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "@${tweet.handle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                Surface(
                    color = sentimentColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        tweet.sentiment,
                        style = MaterialTheme.typography.labelSmall,
                        color = sentimentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                tweet.body,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    tweet.timeAgo,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}
