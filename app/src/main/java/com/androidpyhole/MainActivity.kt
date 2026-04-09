package com.androidpyhole

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val dnsManager = DNSManager()
    private lateinit var settings: SettingsRepository
    private val excludedApps = mutableStateListOf<String>()

    private val vpnRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> if (result.resultCode == Activity.RESULT_OK) startVpnServices() }
    private val createBackupFile = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let { saveBackupToUri(it) } }
    private val pickRestoreFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { restoreFromUri(it) } }
    private val exportLogsCsv = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let { saveLogsToCsv(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsRepository(this); excludedApps.addAll(settings.getExcludedApps()); scheduleGravityUpdates()
        setContent {
            var themeMode by remember { mutableStateOf(settings.getThemeMode()) }
            var isLocked by remember { mutableStateOf(settings.getAppLockPin() != null) }
            PyHoleXTheme(darkTheme = themeMode == 2) {
                if (isLocked) LockScreen { isLocked = false }
                else MainContent(dnsManager, settings, excludedApps, { requestVpn() }, { createBackupFile.launch("pyholex_backup.json") }, { pickRestoreFile.launch(arrayOf("application/json")) }, { exportLogsCsv.launch("pyholex_audit_log.csv") }) { themeMode = it; settings.saveThemeMode(it) }
            }
        }
    }

    private fun saveBackupToUri(uri: Uri) { lifecycleScope.launch { val backup = dnsManager.getBackup(); if (backup != null) { contentResolver.openOutputStream(uri)?.use { os -> OutputStreamWriter(os).use { writer -> writer.write(backup) } }; Toast.makeText(this@MainActivity, "Backup Saved", Toast.LENGTH_SHORT).show() } } }
    private fun restoreFromUri(uri: Uri) { lifecycleScope.launch { contentResolver.openInputStream(uri)?.use { inputStream -> val reader = BufferedReader(InputStreamReader(inputStream)); val json = reader.readText(); if (dnsManager.restoreBackup(json)) Toast.makeText(this@MainActivity, "Settings Restored", Toast.LENGTH_SHORT).show() } } }
    private fun saveLogsToCsv(uri: Uri) { lifecycleScope.launch { val logs = dnsManager.searchLogs(limit = 1000); val csv = StringBuilder("ID,Timestamp,Domain,App,Blocked,Reason,Category\n"); for (i in 0 until logs.length()) { val log = logs.getJSONObject(i); csv.append("${log.optInt("id")},${log.optString("timestamp")},${log.optString("domain")},${log.optString("app_package")},${log.optBoolean("blocked")},${log.optString("reason")},${log.optString("category")}\n") }; contentResolver.openOutputStream(uri)?.use { os -> OutputStreamWriter(os).use { writer -> writer.write(csv.toString()) } }; Toast.makeText(this@MainActivity, "Logs Exported", Toast.LENGTH_SHORT).show() } }
    private fun scheduleGravityUpdates() { val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).setRequiresBatteryNotLow(true).build(); val updateRequest = PeriodicWorkRequestBuilder<GravityUpdateWorker>(24, TimeUnit.HOURS).setConstraints(constraints).build(); WorkManager.getInstance(this).enqueueUniquePeriodicWork("gravity_sync", ExistingPeriodicWorkPolicy.KEEP, updateRequest) }
    private fun requestVpn() { val intent = VpnService.prepare(this); if (intent != null) vpnRequest.launch(intent) else startVpnServices() }
    private fun startVpnServices() { settings.saveExcludedApps(excludedApps.toList()); val intent = Intent(this, VPNServiceBridge::class.java).apply { setAction(VPNServiceBridge.ACTION_START); putStringArrayListExtra(VPNServiceBridge.EXTRA_EXCLUDED_APPS, ArrayList(excludedApps)) }; startService(intent); startService(Intent(this, DNSService::class.java)); Toast.makeText(this, "Shield Engaged", Toast.LENGTH_SHORT).show() }
}

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    val context = LocalContext.current
    val actualPin = SettingsRepository(context).getAppLockPin()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(40.dp), Alignment.CenterHorizontally, Arrangement.Center) {
        Icon(Icons.Default.Lock, null, Modifier.size(64.dp), MaterialTheme.colorScheme.primary); Spacer(Modifier.height(24.dp)); Text("SHIELD LOCKED", fontWeight = FontWeight.Black, fontSize = 24.sp); Spacer(Modifier.height(16.dp)); OutlinedTextField(value = pin, onValueChange = { if(it.length <= 4) pin = it; if(it == actualPin) onUnlocked() }, label = { Text("4-Digit PIN") }, modifier = Modifier.width(200.dp))
    }
}

@Composable
fun MainContent(dnsManager: DNSManager, settings: SettingsRepository, excludedApps: MutableList<String>, onToggleVpn: () -> Unit, onBackup: () -> Unit, onRestore: () -> Unit, onExportLogs: () -> Unit, onThemeChange: (Int) -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Analytics, null) }, label = { Text("Dash") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Security, null) }, label = { Text("Shield") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.CloudSync, null) }, label = { Text("Rules") })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.Tune, null) }, label = { Text("System") })
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> DashboardScreen(dnsManager, onToggleVpn)
                1 -> ShieldScreen(dnsManager, onExportLogs)
                2 -> GravityScreen(dnsManager)
                3 -> SystemScreen(dnsManager, settings, excludedApps, onBackup, onRestore, onThemeChange)
            }
        }
    }
}

@Composable
fun DashboardScreen(dnsManager: DNSManager, onToggleVpn: () -> Unit) {
    var stats by remember { mutableStateOf<JSONObject?>(null) }
    LaunchedEffect(Unit) { while(true) { stats = dnsManager.getStats(); delay(3000) } }
    Column(Modifier.padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("PYHOLEX ENGINE", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black); Spacer(Modifier.height(20.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("STATUS: PROTECTED", color = Color(0xFF78DC77), fontWeight = FontWeight.Bold); Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { StatItem("Queries", stats?.optInt("total_queries") ?: 0); StatItem("Blocked", stats?.optInt("blocked_queries") ?: 0, Color(0xFFFFB4A9)) }
                Spacer(Modifier.height(16.dp)); LinearProgressIndicator(progress = (stats?.optDouble("blocking_percentage")?.div(100.0) ?: 0.0).toFloat(), Modifier.fillMaxWidth().height(8.dp), Color(0xFF78DC77))
            }
        }
        Spacer(Modifier.height(24.dp)); Text("ACTIVE THREAT INTELLIGENCE", fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth().padding(top = 8.dp)) { Column(Modifier.padding(16.dp)) { SustainabilityRow(Icons.Default.Dns, "Domains Blocked", "${stats?.optInt("domains_loaded") ?: 0}", Color(0xFF78DC77)); SustainabilityRow(Icons.Default.Update, "Last Gravity Sync", stats?.optString("last_sync")?.split("T")?.first() ?: "Never", Color(0xFF33A0FE)); SustainabilityRow(Icons.Default.Speed, "Avg Latency", "${stats?.optDouble("avg_latency_ms") ?: 0.0} ms", Color(0xFF8BC34A)) } }
        Spacer(Modifier.height(24.dp)); Text("APP AUDIT (TOP BY TRAFFIC)", fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(Modifier.padding(12.dp)) {
                val appStats = stats?.optJSONArray("app_stats") ?: JSONArray()
                if (appStats.length() == 0) Text("Aggregating local metrics...", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                for (i in 0 until appStats.length()) {
                    val app = appStats.getJSONObject(i)
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Alignment.CenterVertically) {
                        Icon(Icons.Default.Android, null, Modifier.size(16.dp), Color.Gray)
                        Spacer(Modifier.width(8.dp))
                        Text(app.optString("package").split(".").last(), Modifier.weight(1f), maxLines = 1, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("${app.optInt("blocked")}/${app.optInt("total")}", style = MaterialTheme.typography.labelSmall, color = if(app.optInt("blocked") > 0) Color(0xFFFFB4A9) else Color.Gray)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp)); Text("ENVIRONMENT IMPACT", fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth().padding(top = 8.dp)) { Column(Modifier.padding(16.dp)) { SustainabilityRow(Icons.Default.DataUsage, "Data Conserved", "${stats?.optInt("saved_data_kb") ?: 0} KB", Color(0xFF78DC77)); SustainabilityRow(Icons.Default.Bolt, "Energy Saved", "${String.format("%.2f", stats?.optDouble("saved_power_mwh") ?: 0.0)} mWh", Color(0xFF33A0FE)) } }
        Spacer(Modifier.height(24.dp)); Button(onClick = onToggleVpn, Modifier.fillMaxWidth()) { Text("Rotate Resolution Interface") }
    }
}

@Composable fun SustainabilityRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color) { Row(Modifier.padding(vertical = 4.dp), Alignment.CenterVertically) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Column { Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray); Text(value, fontWeight = FontWeight.Bold) } } }
@Composable fun StatItem(label: String, value: Int, color: Color = MaterialTheme.colorScheme.onSurface) { Column { Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray); Text(value.toString(), style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.ExtraBold) } }

@Composable
fun ShieldScreen(dnsManager: DNSManager, onExportLogs: () -> Unit) {
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<JSONObject?>(null) }
    var logs by remember { mutableStateOf(JSONArray()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }
    LaunchedEffect(searchQuery, selectedCategory) { while(true) { stats = dnsManager.getStats(); logs = dnsManager.searchLogs(searchQuery, category = if(selectedCategory == "all") null else selectedCategory); delay(3000) } }
    Column(Modifier.padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("SHIELD AUDIT", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); IconButton(onClick = onExportLogs) { Icon(Icons.Default.Share, null) } }
        Spacer(Modifier.height(16.dp)); OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search Domains or Apps...") }, leadingIcon = { Icon(Icons.Default.Search, null) })
        Spacer(Modifier.height(12.dp)); Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selectedCategory == "all", onClick = { selectedCategory = "all" }, label = { Text("All") })
            FilterChip(selected = selectedCategory == "ads", onClick = { selectedCategory = "ads" }, label = { Text("Ads") })
            FilterChip(selected = selectedCategory == "malware", onClick = { selectedCategory = "malware" }, label = { Text("Threats") })
            FilterChip(selected = selectedCategory == "tracker", onClick = { selectedCategory = "tracker" }, label = { Text("Tracks") })
        }
        Spacer(Modifier.height(12.dp)); Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) { ShieldCard("Standard", stats?.optString("profile") == "standard", Modifier.weight(1f)) { scope.launch { dnsManager.setProfile("standard") } }; ShieldCard("Strict", stats?.optString("profile") == "strict", Modifier.weight(1f)) { scope.launch { dnsManager.setProfile("strict") } } }
        Spacer(Modifier.height(24.dp)); LazyColumn(Modifier.weight(1f)) { items((0 until logs.length()).map { logs.getJSONObject(it) }) { log -> ListItem(headlineContent = { Text(log.optString("domain"), maxLines = 1, fontSize = 14.sp, fontWeight = FontWeight.Bold) }, supportingContent = { Text("${log.optString("app_package").split(".").last()} | ${log.optString("reason", "Passed")}", fontSize = 10.sp) }, trailingContent = { if (log.optBoolean("blocked")) Badge(containerColor = Color(0xFFFFB4A9)) { Text(log.optString("category").toUpperCase()) } } ); Divider(color = Color.Gray.copy(alpha = 0.1f)) } }
    }
}

@Composable fun ShieldCard(title: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) { Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) { Box(Modifier.padding(16.dp).fillMaxWidth(), Alignment.Center) { Text(title, fontWeight = FontWeight.Bold) } } }

@Composable
fun GravityScreen(dnsManager: DNSManager) {
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<JSONObject?>(null) }
    var item by remember { mutableStateOf("") }
    var selectedList by remember { mutableStateOf("gravity") }
    LaunchedEffect(Unit) { stats = dnsManager.getStats() }
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("RULE ENGINE", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) { FilterChip(selected = selectedList == "gravity", onClick = { selectedList = "gravity" }, label = { Text("Gravity") }); FilterChip(selected = selectedList == "blacklist", onClick = { selectedList = "blacklist" }, label = { Text("Black") }); FilterChip(selected = selectedList == "whitelist", onClick = { selectedList = "whitelist" }, label = { Text("White") }); FilterChip(selected = selectedList == "regex", onClick = { selectedList = "regex" }, label = { Text("Regex") }) }
        Card(Modifier.fillMaxWidth().padding(top = 16.dp)) { Column(Modifier.padding(16.dp)) { Text("Add Entry", fontWeight = FontWeight.Bold); OutlinedTextField(value = item, onValueChange = { item = it }, label = { Text("Domain or Pattern") }, modifier = Modifier.fillMaxWidth()); Button(onClick = { scope.launch { if (dnsManager.manageList(selectedList, "add", item)) { item = ""; stats = dnsManager.getStats() } } }, Modifier.align(Alignment.End).padding(top = 8.dp)) { Text("Add") } } }
        Spacer(Modifier.height(24.dp)); Text("ACTIVE RULES", fontWeight = FontWeight.Bold)
        val currentList = when(selectedList) { "gravity" -> stats?.optJSONArray("urls"); "blacklist" -> stats?.optJSONArray("blacklist"); "whitelist" -> stats?.optJSONArray("whitelist"); "regex" -> stats?.optJSONArray("regex_rules"); else -> JSONArray() } ?: JSONArray()
        for (i in 0 until currentList.length()) { val entry = currentList.getString(i); ListItem(headlineContent = { Text(entry, fontSize = 12.sp, maxLines = 1) }, trailingContent = { IconButton(onClick = { scope.launch { dnsManager.manageList(selectedList, "remove", entry); stats = dnsManager.getStats() } }) { Icon(Icons.Default.Delete, null) } }) }
        Spacer(Modifier.height(24.dp)); Button(onClick = { scope.launch { dnsManager.updateBlocklists(); stats = dnsManager.getStats() } }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Sync, null); Spacer(Modifier.width(8.dp)); Text("Force Full Sync") }
    }
}

data class AppInfo(val name: String, val packageName: String)

@Composable
fun SystemScreen(dnsManager: DNSManager, settings: SettingsRepository, excludedApps: MutableList<String>, onBackup: () -> Unit, onRestore: () -> Unit, onThemeChange: (Int) -> Unit) {
    val scope = rememberCoroutineScope(); val context = LocalContext.current
    var upstream by remember { mutableStateOf(settings.getUpstreamDns()) }
    var diagDomain by remember { mutableStateOf("") }; var diagResult by remember { mutableStateOf("") }
    var stats by remember { mutableStateOf<JSONObject?>(null) }; var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var showAppSelector by remember { mutableStateOf(false) }; var appLockPin by remember { mutableStateOf(settings.getAppLockPin() ?: "") }
    var networkDevices by remember { mutableStateOf(JSONArray()) }
    LaunchedEffect(Unit) { stats = dnsManager.getStats(); networkDevices = dnsManager.scanNetwork(); val pm = context.packageManager; apps = pm.getInstalledApplications(PackageManager.GET_META_DATA).filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }.map { AppInfo(it.loadLabel(pm).toString(), it.packageName) }.sortedBy { it.name } }
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("SYSTEM STACK", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Spacer(Modifier.height(24.dp))
        Text("APPEARANCE", fontWeight = FontWeight.Bold); Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onThemeChange(1) }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) { Text("Light") }
            Button(onClick = { onThemeChange(2) }, Modifier.weight(1f)) { Text("Dark") }
        }
        Spacer(Modifier.height(16.dp)); Text("NETWORK INVENTORY", fontWeight = FontWeight.Bold); Card(Modifier.fillMaxWidth().height(150.dp).padding(vertical = 8.dp)) { LazyColumn { items((0 until networkDevices.length()).map { networkDevices.getJSONObject(it) }) { dev -> ListItem(headlineContent = { Text(dev.optString("vendor"), fontSize = 12.sp) }, supportingContent = { Text(dev.optString("ip"), fontSize = 10.sp) }, leadingContent = { Icon(Icons.Default.Devices, null) }) } } }
        Spacer(Modifier.height(16.dp)); Text("DIAGNOSTICS", fontWeight = FontWeight.Bold); Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Column(Modifier.padding(16.dp)) { OutlinedTextField(value = diagDomain, onValueChange = { diagDomain = it }, label = { Text("Lookup Domain") }, modifier = Modifier.fillMaxWidth()); Button(onClick = { scope.launch { diagResult = dnsManager.lookup(diagDomain) } }, Modifier.padding(top = 8.dp)) { Text("Run Query") }; if(diagResult.isNotEmpty()) { Text(diagResult, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) } } }
        Spacer(Modifier.height(16.dp)); Text("MAINTENANCE", fontWeight = FontWeight.Bold); Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { dnsManager.purgeCache() } }, Modifier.weight(1f)) { Text("Purge Cache") }
            Button(onClick = { scope.launch { dnsManager.flushLogs() } }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB4A9), contentColor = Color.Black)) { Text("Flush Logs") }
        }
        Spacer(Modifier.height(16.dp)); Text("SECURITY LOCK", fontWeight = FontWeight.Bold); OutlinedTextField(value = appLockPin, onValueChange = { if(it.length <= 4) appLockPin = it; settings.setAppLockPin(if(it.isEmpty()) null else it) }, modifier = Modifier.fillMaxWidth(), label = { Text("4-Digit PIN") }, placeholder = { Text("Disabled") })
        Spacer(Modifier.height(16.dp)); Text("UPSTREAM", fontWeight = FontWeight.Bold); OutlinedTextField(value = upstream, onValueChange = { upstream = it }, modifier = Modifier.fillMaxWidth(), label = { Text("IP:Port") }, trailingIcon = { IconButton(onClick = { scope.launch { if (dnsManager.setUpstream(upstream)) settings.saveUpstreamDns(upstream) } }) { Icon(Icons.Default.Save, null) } })
        Spacer(Modifier.height(16.dp)); Text("SPLIT TUNNELING", fontWeight = FontWeight.Bold); Button(onClick = { showAppSelector = true }, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Bypass VPN (${excludedApps.size})") }
        if (showAppSelector) { AlertDialog(onDismissRequest = { showAppSelector = false }, title = { Text("VPN Bypass") }, text = { Box(Modifier.height(400.dp)) { LazyColumn { items(apps) { app -> Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Alignment.CenterVertically) { Checkbox(checked = excludedApps.contains(app.packageName), onCheckedChange = { checked -> if (checked) excludedApps.add(app.packageName) else excludedApps.remove(app.packageName); settings.saveExcludedApps(excludedApps.toList()) }); Spacer(Modifier.width(8.dp)); Text(app.name, Modifier.weight(1f), fontSize = 14.sp) } } } } }, confirmButton = { TextButton(onClick = { showAppSelector = false }) { Text("Done") } }) }
        Spacer(Modifier.height(16.dp)); Text("PORTABILITY", fontWeight = FontWeight.Bold); Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) { Button(onClick = onBackup, Modifier.weight(1f)) { Text("Backup") }; OutlinedButton(onClick = onRestore, Modifier.weight(1f)) { Text("Restore") } }
        Spacer(Modifier.height(24.dp)); Text("Engine: v5.2.1 (Rust Core)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Composable fun PyHoleXTheme(darkTheme: Boolean, content: @Composable () -> Unit) { val colorScheme = if (darkTheme) darkColorScheme(primary = Color(0xFF78DC77), background = Color(0xFF131313), surface = Color(0xFF1C1B1B)) else lightColorScheme(primary = Color(0xFF2E7D32), background = Color(0xFFF4F4F4), surface = Color.White); MaterialTheme(colorScheme = colorScheme, content = content) }