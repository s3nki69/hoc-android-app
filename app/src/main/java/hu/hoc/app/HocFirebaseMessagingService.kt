package hu.hoc.app

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HocFirebaseMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        if (!NativeApi.isPushEnabled(this)) return
        scope.launch { runCatching { NativeApi.registerFcmToken(this@HocFirebaseMessagingService, token) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (!NativeApi.isPushEnabled(this)) return
        val d = message.data
        val title = d["title"] ?: message.notification?.title ?: "HOC.hu"
        val body = d["body"] ?: message.notification?.body.orEmpty()
        val url = HocUrls.normalize(d["url"].orEmpty()).ifBlank { HocUrls.SITE + "/" }
        val inboxId = d["inbox_id"]?.toIntOrNull() ?: 0
        showNotification(title, body, url, inboxId)
    }

    private fun showNotification(title: String, body: String, url: String, inboxId: Int) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        HocNotificationWorker.createChannel(this)
        val target = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(url)
            putExtra(MainActivity.EXTRA_INBOX_ID, inboxId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val requestCode = if (inboxId > 0) inboxId else (System.currentTimeMillis() and 0x7fffffff).toInt()
        val pi = PendingIntent.getActivity(this, requestCode, target, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, HocNotificationWorker.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(this).notify(requestCode, n)
    }
}
