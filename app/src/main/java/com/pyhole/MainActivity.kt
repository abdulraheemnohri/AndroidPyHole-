package com.pyhole

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PyHoleXTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DashboardScreen()
                }
            }
        }
    }
}

@Composable
fun DashboardScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "PYHOLEX", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF78DC77))
        Spacer(modifier = Modifier.height(20.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Service Status: RUNNING", color = Color(0xFF78DC77))
                Text("Total Queries: 52,314")
                Text("Blocked: 18,403", color = Color(0xFFFFB4A9))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = { /* Start/Stop logic */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Stop Service")
        }
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
