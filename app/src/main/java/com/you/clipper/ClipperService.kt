package com.you.clipper
import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
class ClipperService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var nm: NotificationManager
    companion object { private const val CHANNEL = "clipper"; private const val ID = 42; private const val DONE_ID = 43; const val EXTRA_URI = "uri" }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL, "Clipping", NotificationManager.IMPORTANCE_LOW)
            ch.setShowBadge(false); nm.createNotificationChannel(ch)
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uri = intent?.getStringExtra(EXTRA_URI)?.let(Uri::parse)
        if (uri == null) { stopSelf(); return START_NOT_STICKY }
        startForeground(ID, progress("Starting…"))
        scope.launch {
            try {
                val clips = Pipeline.run(this@ClipperService, uri) { step -> nm.notify(ID, progress(step)) }
                nm.notify(DONE_ID, finish("Clipping done", "${clips.size} clip(s) ready in your gallery folder"))
            } catch (e: CancellationException) { throw e } 
            catch (e: Exception) { nm.notify(DONE_ID, finish("Clipping failed", e.message ?: "Unknown error")) } 
            finally { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
        }
        return START_NOT_STICKY
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
    private fun progress(text: String): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("Clipper").setContentText(text).setOngoing(true).setOnlyAlertOnce(true).setProgress(0, 0, true).setContentIntent(open).build()
    }
    private fun finish(title: String, body: String): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.stat_sys_download_done).setContentTitle(title).setContentText(body).setAutoCancel(true).setContentIntent(open).build()
    }
}
