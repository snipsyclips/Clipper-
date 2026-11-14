package com.you.clipper
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
        val cfg = Settings(this)
        setContent {
            MaterialTheme {
                var groq by remember { mutableStateOf(cfg.groqKey) }
                var gemini by remember { mutableStateOf(cfg.geminiKey) }
                var status by remember { mutableStateOf("Pick a video — processing runs in the background") }
                val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
                val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    cfg.groqKey = groq; cfg.geminiKey = gemini
                    if (Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                    startForegroundService(Intent(this@MainActivity, ClipperService::class.java).putExtra(ClipperService.EXTRA_URI, uri.toString()))
                    status = "Processing in background — check the notification"
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Clipper", style = MaterialTheme.typography.headlineMedium)
                    OutlinedTextField(groq, { groq = it }, label = { Text("Groq API key") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(gemini, { gemini = it }, label = { Text("Gemini API key") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { picker.launch(arrayOf("video/*")) }, enabled = groq.isNotBlank() && gemini.isNotBlank()) { Text("Pick video & clip") }
                    Text(status)
                }
            }
        }
    }
}
