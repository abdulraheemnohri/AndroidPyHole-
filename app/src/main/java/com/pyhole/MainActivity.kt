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
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Apps, "Apps") }, label = { Text("Apps") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Security, "AI") }, label = { Text("AI") })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.Settings, "Settings") }, label = { Text("Settings") })
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> DashboardScreen()
                1 -> AppControlScreen()
                2 -> AIInsightsScreen()
                3 -> SettingsScreen()
            }
        }
    }
}

@Composable
fun DashboardScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("PYHOLEX GLOBAL", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF78DC77))
        Spacer(modifier = Modifier.height(20.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Status: RUNNING", color = Color(0xFF78DC77))
                Text("Total Queries: 52,314")
                Text("Threats Blocked: 18,403", color = Color(0xFFFFB4A9))
                Text("P2P Nodes Sync: ACTIVE", color = Color(0xFF33A0FE))
            }
        }
    }
}

@Composable
fun AppControlScreen() {
    val apps = listOf("Chrome", "TikTok", "Instagram", "YouTube")
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item { Text("App-Level DNS Control", style = MaterialTheme.typography.headlineMedium) }
        items(apps) { app ->
            ListItem(
                headlineContent = { Text(app) },
                trailingContent = { Switch(checked = true, onCheckedChange = {}) }
            )
        }
    }
}

@Composable
fun AIInsightsScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("AI & Heuristics", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(10.dp))
        Text("Real-time entropy analysis active.")
        LinearProgressIndicator(progress = 0.7f, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun SettingsScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Configuration", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("Update Gravity") }
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
