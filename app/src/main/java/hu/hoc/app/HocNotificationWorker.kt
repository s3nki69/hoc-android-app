package hu.hoc.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class HocNotificationWorker(appContext: Context, params: androidx.work.WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            NativeApi.ensureRegistered(applicationContext)
            val items = NativeApi.inbox(applicationContext)
            if (items.isEmpty()) return@withContext Result.success()

            val maxId = items.maxOf { it.id }
            val lastId = NativeApi.lastInboxId(applicationContext)
            createChannel(applicationContext)
            // Új natív eszközhöz a szerver nem rendel korábbi inbox-elemeket, ezért
            // az első megjelenő üzenetet sem szabad elnyelni. Frissítés után legfeljebb
            // a hat legújabb értesítést mutatjuk meg.
            items.filter { it.id > lastId }.sortedBy { it.id }.takeLast(6).forEach { showNotification(applicationContext, it) }
            NativeApi.setLastInboxId(applicationContext, maxId)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(context: Context, item: NativeApi.InboxItem) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val target = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(item.url.ifBlank { "https://www.hoc.hu/" })
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(context, item.id, target, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(item.title)
            .setContentText(item.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(context).notify(item.id, notification)
    }

    companion object {
        private const val CHANNEL_ID = "hoc_updates"
        private const val WORK_NAME = "hoc_notifications_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request = PeriodicWorkRequestBuilder<HocNotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun runNow(context: Context) {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<HocNotificationWorker>().setConstraints(constraints).build()
            )
        }

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "HOC értesítések", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Új HOC cikkek, kuponok és figyelések"
                })
            }
        }
    }
}
