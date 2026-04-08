@file:OptIn(ExperimentalMaterial3Api::class)
package com.androidpyhole

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private val dnsManager = DNSManager()
    private val vpnPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) startVpn()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var themeMode by remember { mutableIntStateOf(0) }
            val useDarkTheme = when(themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
            PyHoleXTheme(darkTheme = useDarkTheme) {
                MainNavigation(dnsManager, themeMode, onThemeChange = { themeMode = it }, onToggleVpn = { toggleVpn() })
            }
        }
    }

    private fun toggleVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) vpnPermissionLauncher.launch(intent) else startVpn()
    }

    private fun startVpn() {
        val startIntent = Intent(this, VPNServiceBridge::class.java).apply { action = VPNServiceBridge.ACTION_START }
        startService(startIntent)
        startService(Intent(this, DNSService::class.java))
    }
}

@Composable
fun MainNavigation(dnsManager: DNSManager, themeMode: Int, onThemeChange: (Int) -> Unit, onToggleVpn: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    Scaffold(bottomBar = { NavigationBar {
        NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Dashboard, "Home") }, label = { Text("Dashboard") })
        NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.List, "Traffic") }, label = { Text("Audit") })
        NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Security, "Shield") }, label = { Text("Profile") })
        NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.Settings, "Settings") }, label = { Text("System") })
    } }) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> DashboardScreen(dnsManager, onToggleVpn)
                1 -> LogsScreen(dnsManager)
                2 -> ShieldProfileScreen(dnsManager)
                3 -> SettingsScreen(themeMode, onThemeChange, dnsManager)
            }
        }
    }
}

@Composable
fun DashboardScreen(dnsManager: DNSManager, onToggleVpn: () -> Unit) {
    var stats by remember { mutableStateOf<JSONObject?>(null) }
    LaunchedEffect(Unit) { while(true) { stats = dnsManager.getStats(); delay(3000) } }
    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("PYHOLEX DASHBOARD", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(20.dp))
        Card(modifier = Modifier.fillMaxWidth()) { Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("System Protection: ", style = MaterialTheme.typography.titleMedium)
                Text("ACTIVE", color = Color(0xFF78DC77), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatCard("Queries", stats?.optInt("total_queries") ?: 0)
                StatCard("Blocked", stats?.optInt("blocked_queries") ?: 0, Color(0xFFFFB4A9))
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(progress = (stats?.optDouble("blocking_percentage")?.div(100.0) ?: 0.0).toFloat(), modifier = Modifier.fillMaxWidth().height(8.dp), color = Color(0xFF78DC77))
        } }
        Spacer(modifier = Modifier.height(24.dp))
        Text("App Distribution", style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                AppStatRow("Google Chrome", 1402, 0.45f)
                AppStatRow("Instagram", 892, 0.30f)
                AppStatRow("System Services", 450, 0.15f)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onToggleVpn, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Rotate VPN Interface") }
    }
}

@Composable
fun AppStatRow(name: String, queries: Int, ratio: Float) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(queries.toString(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.width(60.dp).height(4.dp).background(Color.DarkGray)) {
            Box(modifier = Modifier.fillMaxWidth(ratio).fillMaxHeight().background(Color(0xFF33A0FE)))
        }
    }
}

@Composable
fun StatCard(label: String, value: Int, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column { Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray); Text(value.toString(), style = MaterialTheme.typography.headlineSmall, color = color) }
}

@Composable
fun LogsScreen(dnsManager: DNSManager) {
    var logs by remember { mutableStateOf(JSONArray()) }
    LaunchedEffect(Unit) { while(true) { logs = dnsManager.getLogs(); delay(5000) } }
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Real-Time Audit", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items((0 until logs.length()).map { logs.getJSONObject(it) }) { log ->
                val blocked = log.optBoolean("blocked")
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(log.optString("domain"), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                        Text("${log.optString("timestamp").split(" ").last()} | Src: ${log.optString("client_ip")}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Badge(containerColor = if (blocked) Color(0xFFFFB4A9) else Color(0xFF78DC77)) {
                        Text(if (blocked) "BLOCK" else "PASS", color = Color.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Divider(color = Color.DarkGray.copy(alpha = 0.2f))
            }
        }
    }
}

@Composable
fun ShieldProfileScreen(dnsManager: DNSManager) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf("standard") }
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Security Profiles", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(20.dp))
        ProfileOption("Standard Shield", "Balanced privacy with community blocklists.", profile == "standard") { scope.launch { if(dnsManager.setProfile("standard")) profile = "standard" } }
        Spacer(Modifier.height(12.dp))
        ProfileOption("Strict Protocol", "Paranoid mode. Blocks all telemetry and metrics.", profile == "strict") { scope.launch { if(dnsManager.setProfile("strict")) profile = "strict" } }
        Spacer(Modifier.height(12.dp))
        ProfileOption("DNS Resolver", "Zero filtering. Local high-speed resolution only.", profile == "dns_only") { scope.launch { if(dnsManager.setProfile("dns_only")) profile = "dns_only" } }

        Spacer(modifier = Modifier.height(30.dp))
        Text("Content Rules", style = MaterialTheme.typography.titleMedium)
        ListItem(headlineContent = { Text("YouTube Ad-Free (Beta)") }, supportingContent = { Text("Attempts to block video ad domains") }, trailingContent = { Switch(checked = true, onCheckedChange = {}) })
    }
}

@Composable
fun ProfileOption(title: String, desc: String, active: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(title, style = MaterialTheme.typography.titleLarge); if(active) Icon(Icons.Default.Verified, null, Modifier.padding(start = 8.dp), MaterialTheme.colorScheme.primary) }
            Text(desc, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SettingsScreen(themeMode: Int, onThemeChange: (Int) -> Unit, dnsManager: DNSManager) {
    val scope = rememberCoroutineScope()
    var updating by remember { mutableStateOf(false) }
    var upstream by remember { mutableStateOf("1.1.1.1:53") }
    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("System Configuration", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Text("DNS Protocol", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(value = upstream, onValueChange = { upstream = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Upstream Resolver (IP:Port)") }, trailingIcon = { IconButton(onClick = { scope.launch { dnsManager.setUpstream(upstream) } }) { Icon(Icons.Default.Save, null) } })
        Spacer(modifier = Modifier.height(24.dp))
        Text("Privacy Filters", style = MaterialTheme.typography.titleSmall)
        ListItem(headlineContent = { Text("Parental Control") }, supportingContent = { Text("Active filtering for Adult/Gamble content") }, trailingContent = { Switch(checked = true, onCheckedChange = { scope.launch { dnsManager.toggleParentalControl() } }) })
        Spacer(modifier = Modifier.height(24.dp))
        Text("Gravity Lists", style = MaterialTheme.typography.titleSmall)
        Text("• https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { scope.launch { updating = true; dnsManager.updateBlocklists(); updating = false } }, modifier = Modifier.fillMaxWidth(), enabled = !updating) {
            if (updating) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White) else Text("Sync Remote Gravity")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Upload, null); Spacer(Modifier.width(8.dp)); Text("Export Audit Log (SQLite)") }
    }
}

@Composable
fun PyHoleXTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) darkColorScheme(primary = Color(0xFF78DC77), background = Color(0xFF131313), surface = Color(0xFF1C1B1B)) else lightColorScheme(primary = Color(0xFF2E7D32), background = Color(0xFFF4F4F4), surface = Color.White)
    MaterialTheme(colorScheme = colorScheme, content = content)
}
