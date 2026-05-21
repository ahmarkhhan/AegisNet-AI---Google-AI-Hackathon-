package com.aegisnet.mobile.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.navigation.compose.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegisnet.mobile.ui.dashboard.DashboardViewModel
import com.aegisnet.mobile.domain.agent.EocAgentOrchestrator
import com.aegisnet.mobile.domain.agent.GeminiAgentService
import com.aegisnet.mobile.ui.alerts.AlertDetailScreen
import com.aegisnet.mobile.ui.dashboard.DashboardScreen
import com.aegisnet.mobile.ui.dashboard.components.AgentOperationsHubScreen
import com.aegisnet.mobile.ui.feed.CrisisFeedScreen
import com.aegisnet.mobile.ui.map.CrisisMapScreen
import com.aegisnet.mobile.ui.map.RealGoogleMapScreen
import com.aegisnet.mobile.ui.report.EnhancedReportIncidentScreen
import com.aegisnet.mobile.ui.report.ReportIncidentScreen
import com.aegisnet.mobile.ui.report.MyReportsScreen
import com.aegisnet.mobile.ui.resources.ResourceAllocationScreen
import com.aegisnet.mobile.ui.settings.SettingsScreen
import com.aegisnet.mobile.ui.theme.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Tactical Dashboard", Icons.Default.Dashboard)
    object TacticalMap : Screen("tactical_map", "Live Map HUD", Icons.Default.Map)
    object AgentHub : Screen("agent_hub", "Agent Ops", Icons.Default.SupportAgent)
    object ReportIncident : Screen("report_incident", "Report Crisis", Icons.Default.AddAlert)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object CrisisFeed : Screen("crisis_feed", "Crisis Feed", Icons.Default.Notifications)
    object ResourceAllocation : Screen("resource_allocation", "Resource Hub", Icons.Default.LocalFireDepartment)
    object MyReports : Screen("my_reports", "My Reported Crises", Icons.Default.History)
}

@Composable
fun AegisNavGraph(
    orchestrator: EocAgentOrchestrator = EocAgentOrchestrator(GeminiAgentService())
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val menuItems = listOf(
        Screen.Dashboard,
        Screen.TacticalMap,
        Screen.CrisisFeed,
        Screen.ResourceAllocation,
        Screen.MyReports,
        Screen.ReportIncident
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = AegisPanel,
                drawerContentColor = Color.White,
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                windowInsets = WindowInsets(0)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                // Rebranded EOC Drawer Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = com.aegisnet.mobile.R.drawable.ic_aegis_logo),
                        contentDescription = "Nigehban AI Logo",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Column {
                        Text(
                            "نگہبان AI",
                            color = AegisSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Nigehban AI EOC Center",
                            color = Color.LightGray,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }
                
                Divider(color = AegisSlate.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                
                // Drawer Items
                menuItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null, tint = if (isSelected) AegisSuccess else Color.Gray) },
                        label = { Text(item.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        selected = isSelected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = AegisSlate,
                            selectedTextColor = AegisSuccess,
                            unselectedTextColor = Color.LightGray,
                            unselectedIconColor = Color.Gray
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Footer
                Text(
                    "Security Level: CLASS-1 EOC",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) {
        Scaffold(
            containerColor = AegisDark,
            bottomBar = {
                val showBottomBar = currentRoute in listOf(Screen.TacticalMap.route, Screen.CrisisFeed.route, Screen.Dashboard.route)
                if (showBottomBar) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                            .navigationBarsPadding()
                    ) {
                        Surface(
                            color = AegisPanel.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.5.dp, AegisSlate),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val tabs = listOf(
                                    Pair(Screen.TacticalMap, "Live Map HUD"),
                                    Pair(Screen.CrisisFeed, "Live Feed")
                                )
                                tabs.forEach { (screen, label) ->
                                    val isSelected = currentRoute == screen.route
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable {
                                                if (currentRoute != screen.route) {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.startDestinationId) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = label,
                                                tint = if (isSelected) AegisSuccess else Color.Gray,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = label,
                                                color = if (isSelected) AegisSuccess else Color.Gray,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onAlertClick = { /* Navigate locally or show detail dialog */ },
                        onReportClick = { navController.navigate(Screen.ReportIncident.route) }
                    )
                }

                composable(Screen.TacticalMap.route) {
                    RealGoogleMapScreen(
                        orchestrator = orchestrator,
                        isOffline = false,
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.AgentHub.route) {
                    AgentOperationsHubScreen(
                        orchestrator = orchestrator
                    )
                }

                composable(Screen.ReportIncident.route) {
                    ReportIncidentScreen(
                        onBack = { navController.navigate(Screen.Dashboard.route) }
                    )
                }

                composable(Screen.MyReports.route) {
                    MyReportsScreen(
                        onBack = { navController.navigate(Screen.Dashboard.route) }
                    )
                }

                composable(Screen.CrisisFeed.route) {
                    val dashboardViewModel: DashboardViewModel = hiltViewModel()
                    val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
                    CrisisFeedScreen(
                        alerts = uiState.alerts,
                        onBack = { navController.navigate(Screen.Dashboard.route) }
                    )
                }

                composable(Screen.ResourceAllocation.route) {
                    val dashboardViewModel: DashboardViewModel = hiltViewModel()
                    val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
                    ResourceAllocationScreen(
                        alerts = uiState.alerts,
                        onBack = { navController.navigate(Screen.Dashboard.route) }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onBack = { navController.navigate(Screen.Dashboard.route) }
                    )
                }
            }
        }
    }
}
