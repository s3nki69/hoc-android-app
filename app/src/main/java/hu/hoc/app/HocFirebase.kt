package hu.hoc.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

object HocFirebase {
    private const val PREFS = "hoc_firebase"
    private const val PROJECT_ID = "project_id"
    private const val PROJECT_NUMBER = "project_number"
    private const val APP_ID = "app_id"
    private const val API_KEY = "api_key"

    fun initializeFromCache(context: Context): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cfg = NativeApi.FcmConfig(
            enabled = p.getBoolean("enabled", false),
            projectId = p.getString(PROJECT_ID, "").orEmpty(),
            projectNumber = p.getString(PROJECT_NUMBER, "").orEmpty(),
            appId = p.getString(APP_ID, "").orEmpty(),
            apiKey = p.getString(API_KEY, "").orEmpty()
        )
        return initialize(context, cfg, save = false)
    }

    fun initialize(context: Context, cfg: NativeApi.FcmConfig, save: Boolean = true): Boolean {
        if (save) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("enabled", cfg.enabled)
                .putString(PROJECT_ID, cfg.projectId)
                .putString(PROJECT_NUMBER, cfg.projectNumber)
                .putString(APP_ID, cfg.appId)
                .putString(API_KEY, cfg.apiKey)
                .apply()
        }
        if (!cfg.ready) return false
        if (FirebaseApp.getApps(context).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApplicationId(cfg.appId)
                .setApiKey(cfg.apiKey)
                .setProjectId(cfg.projectId)
                .setGcmSenderId(cfg.projectNumber)
                .build()
            FirebaseApp.initializeApp(context, options)
        }
        return FirebaseApp.getApps(context).isNotEmpty()
    }

    suspend fun refreshAndRegister(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (!NativeApi.isPushEnabled(context)) return@withContext false
        val cfg = NativeApi.appConfig().fcm
        val initialized = withContext(Dispatchers.Main) { initialize(context, cfg) }
        if (!initialized) {
            val cats = runCatching { NativeApi.status(context).categories }.getOrDefault(emptyList())
            runCatching { NativeApi.ensureRegistered(context, cats) }
            HocNotificationWorker.schedule(context, fcmReady = false)
            return@withContext false
        }
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        if (token.isNullOrBlank()) {
            val cats = runCatching { NativeApi.status(context).categories }.getOrDefault(emptyList())
            runCatching { NativeApi.ensureRegistered(context, cats) }
            HocNotificationWorker.schedule(context, fcmReady = false)
            return@withContext false
        }
        val ok = NativeApi.registerFcmToken(context, token)
        HocNotificationWorker.schedule(context, fcmReady = ok)
        ok
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        NativeApi.setPushEnabled(context, enabled)
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            FirebaseMessaging.getInstance().isAutoInitEnabled = enabled
            if (!enabled) FirebaseMessaging.getInstance().deleteToken()
        }
        if (enabled) HocNotificationWorker.schedule(context, fcmReady = false)
        else HocNotificationWorker.cancel(context)
    }
}
