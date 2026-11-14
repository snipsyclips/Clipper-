// MainActivity.kt
package com.you.aiclipper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ClipperUI()
            }
        }
    }
}

@Composable
fun ClipperUI() {
    var prompt by remember { mutableStateOf("") }
    var videoStyle by remember { mutableStateOf("Podcast") }
    var autoCaptions by remember { mutableStateOf(true) }
    var faceTracking by remember { mutableStateOf(true) }
    var bRoll by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("AI Video Clipper Pro", style = MaterialTheme.typography.headlineMedium)
        
        // The Magic Prompt
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("What do you want? (e.g., 'Find the funniest moments and add yellow captions')") },
            modifier = Modifier.fillMaxWidth()
        )

        // Dropdown for Style
        Text("Video Style:")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = videoStyle == "Podcast", onClick = { videoStyle = "Podcast" }, label = { Text("Podcast") })
            FilterChip(selected = videoStyle == "Gaming", onClick = { videoStyle = "Gaming" }, label = { Text("Gaming") })
            FilterChip(selected = videoStyle == "Vlog", onClick = { videoStyle = "Vlog" }, label = { Text("Vlog") })
        }

        // Toggles for Advanced Features
        Text("AI Enhancements:")
        SwitchRow("Auto-Captions (Karaoke Style)", autoCaptions) { autoCaptions = it }
        SwitchRow("Auto Face Tracking (9:16)", faceTracking) { faceTracking = it }
        SwitchRow("Auto B-Roll Insertion", bRoll) { bRoll = it }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { 
                isProcessing = true 
                // Send prompt and video to Python Backend via Retrofit
                // See Backend code below
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing
        ) {
            Text(if (isProcessing) "Generating Magic..." else "Generate Clips")
        }

        if (isProcessing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Analyzing audio, finding viral moments, and rendering...")
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
