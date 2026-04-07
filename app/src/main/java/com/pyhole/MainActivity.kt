package com.pyhole

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PyHoleXTheme {
                MainNavigation()
            }
        }
    }
}

@Composable
fun MainNavigation() {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Dashboard, "Home") }, label = { Text("Home") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.BatteryChargingFull, "Stats") }, label = { Text("Savings") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Security, "AI") }, label = { Text("Threats") })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.Settings, "Settings") }, label = { Text("Mesh") })
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> DashboardScreen()
                1 -> SavingsScreen()
                2 -> AIInsightsScreen()
                3 -> MeshSettingsScreen()
            }
        }
    }
}

@Composable
fun DashboardScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("PYHOLEX MESH", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF78DC77))
        Spacer(modifier = Modifier.height(20.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Status: RUNNING", color = Color(0xFF78DC77))
                Text("Queries Today: 52,314")
                Text("Mesh Peers: 1,402 Active", color = Color(0xFF33A0FE))
            }
        }
    }
}

@Composable
fun SavingsScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Mobile Optimization", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(20.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Estimated Energy Saved", style = MaterialTheme.typography.titleMedium)
                Text("154.2 mAh", style = MaterialTheme.typography.displayMedium)
                Text("Equivalent to ~24 mins extra runtime", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text("Data Saved: 1.4 GB (Estimated ad payload prevention)")
    }
}

@Composable
fun AIInsightsScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("AI & Heuristics", style = MaterialTheme.typography.headlineMedium)
        Text("Total detections: 2,405")
        LinearProgressIndicator(progress = 0.85f, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun MeshSettingsScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Mesh Configuration", style = MaterialTheme.typography.headlineMedium)
        ListItem(
            headlineContent = { Text("Participate in Peer Intelligence") },
            supportingContent = { Text("Share anonymous threat data with the mesh network.") },
            trailingContent = { Switch(checked = true, onCheckedChange = {}) }
        )
    }
}

@Composable
fun PyHoleXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF78DC77),
            background = Color(0xFF131313)
        ),
        content = content
    )
}
