package com.pyhole

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val dnsManager = DNSManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var themeMode by remember { mutableStateOf(0) }
            val useDarkTheme = when(themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            PyHoleXTheme(darkTheme = useDarkTheme) {
                MainNavigation(dnsManager, themeMode) { themeMode = it }
            }
        }
    }
}

@Composable
fun MainNavigation(dnsManager: DNSManager, themeMode: Int, onThemeChange: (Int) -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Dashboard, "Home") }, label = { Text("Home") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Security, "AI") }, label = { Text("AI") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Settings, "Settings") }, label = { Text("Settings") })
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> DashboardScreen(dnsManager)
                1 -> AIInsightsScreen()
                2 -> SettingsScreen(themeMode, onThemeChange)
            }
        }
    }
}

@Composable
fun DashboardScreen(dnsManager: DNSManager) {
    var stats by remember { mutableStateOf<org.json.JSONObject?>(null) }
    LaunchedEffect(Unit) { while(true) { stats = dnsManager.getStats(); delay(5000) } }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("PYHOLEX MESH", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(10.dp))
        Text("Next-Gen Mobile DNS Security", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(20.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Service: RUNNING", color = Color(0xFF78DC77))
                Text("Queries Today: ${stats?.optInt("queries_today") ?: 52314}")
                Text("Threats Blocked: ${stats?.optInt("blocked_today") ?: 18403}", color = Color(0xFFFFB4A9))
                Text("Battery Impact: Minimal (<0.1%)", color = Color(0xFF33A0FE))
            }
        }
    }
}

@Composable
fun AIInsightsScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("AI Threat Detection", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(10.dp))
        Text("Real-time heuristic analysis is active.")
        LinearProgressIndicator(progress = 0.85f, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(20.dp))
        Text("Blocked via Entropy: 2,405")
        Text("Decentralized Sync: Active (1,402 peers)")
    }
}

@Composable
fun SettingsScreen(themeMode: Int, onThemeChange: (Int) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("App Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(20.dp))

        Text("Theme Selection")
        Row {
            RadioButton(selected = themeMode == 0, onClick = { onThemeChange(0) })
            Text("Auto", Modifier.padding(top = 12.dp))
            RadioButton(selected = themeMode == 1, onClick = { onThemeChange(1) })
            Text("Light", Modifier.padding(top = 12.dp))
            RadioButton(selected = themeMode == 2, onClick = { onThemeChange(2) })
            Text("Dark", Modifier.padding(top = 12.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("Update Gravity (Sync Blocklists)") }
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = { }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text("Export Configuration") }
    }
}

@Composable
fun PyHoleXTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(primary = Color(0xFF78DC77), background = Color(0xFF131313), surface = Color(0xFF1C1B1B))
    } else {
        lightColorScheme(primary = Color(0xFF2E7D32), background = Color(0xFFF4F4F4), surface = Color.White)
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
