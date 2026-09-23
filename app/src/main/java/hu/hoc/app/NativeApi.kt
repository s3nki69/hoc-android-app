package hu.hoc.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom

object NativeApi {
    private const val BASE = HocUrls.API
    private const val PREFS = "hoc_native"
    private const val DEVICE = "device"
    private const val LAST_INBOX = "last_inbox"

    data class Topic(val id: Int, val name: String)
    data class InboxItem(val id: Int, val title: String, val body: String, val url: String, val imageUrl: String)
    data class WatchItem(val id: Int, val postId: Int, val title: String, val url: String, val type: String)

    fun deviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(DEVICE, null)?.let { return it }
        val b = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val id = b.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        prefs.edit().putString(DEVICE, id).apply()
        return id
    }

    fun ensureRegistered(context: Context, categories: List<Int>? = null): Boolean {
        if (categories == null) {
            runCatching { if (status(context).first) return true }
        }
        val device = deviceId(context)
        val cats = JSONArray(categories ?: emptyList<Int>())
        val body = JSONObject().apply {
            put("device", device)
            put("subscription", JSONObject().apply {
                put("endpoint", "https://hoc.hu/?hoc_native_device=$device")
                put("keys", JSONObject())
            })
            put("categories", cats)
            put("source_url", "android-app")
        }
        return post("subscribe", body).optBoolean("ok", false)
    }

    fun status(context: Context): Pair<Boolean, List<Int>> {
        val o = get("status?device=${deviceId(context)}")
        val a = o.optJSONArray("categories") ?: JSONArray()
        val out = mutableListOf<Int>()
        for (i in 0 until a.length()) out += a.optInt(i)
        return o.optBoolean("active", false) to out
    }

    fun appConfig(): List<Topic> {
        val o = get("app-config")
        val a = o.optJSONArray("categories") ?: JSONArray()
        val out = mutableListOf<Topic>()
        for (i in 0 until a.length()) {
            val x = a.optJSONObject(i) ?: continue
            out += Topic(x.optInt("id"), x.optString("name"))
        }
        return out
    }

    fun saveCategories(context: Context, categories: List<Int>): Boolean {
        val o = post("preferences", JSONObject().apply {
            put("device", deviceId(context))
            put("categories", JSONArray(categories))
        })
        return o.optBoolean("ok", false)
    }

    fun watch(context: Context, postId: Int, type: String = "deal", priceThreshold: Double? = null): Boolean {
        val body = JSONObject().apply {
            put("device", deviceId(context))
            put("post_id", postId)
            put("watch_type", type)
            if (priceThreshold != null) put("price_threshold", priceThreshold)
        }
        return post("watch", body).optBoolean("ok", false)
    }


    fun watchlist(context: Context): List<WatchItem> {
        val o = get("watchlist?device=${deviceId(context)}")
        val a = o.optJSONArray("items") ?: JSONArray()
        val out = mutableListOf<WatchItem>()
        for (i in 0 until a.length()) {
            val x = a.optJSONObject(i) ?: continue
            out += WatchItem(x.optInt("id"), x.optInt("post_id"), x.optString("title"), HocUrls.normalize(x.optString("url")), x.optString("type"))
        }
        return out
    }

    fun unwatch(context: Context, watchId: Int): Boolean {
        return post("unwatch", JSONObject().apply { put("device", deviceId(context)); put("id", watchId) }).optBoolean("ok", false)
    }

    fun inbox(context: Context): List<InboxItem> {
        val o = get("inbox?device=${deviceId(context)}")
        val a = o.optJSONArray("items") ?: JSONArray()
        val out = mutableListOf<InboxItem>()
        for (i in 0 until a.length()) {
            val x = a.optJSONObject(i) ?: continue
            out += InboxItem(
                id = x.optInt("id"),
                title = x.optString("title"),
                body = x.optString("body"),
                url = HocUrls.normalize(x.optString("url")),
                imageUrl = HocUrls.normalize(x.optString("image_url"))
            )
        }
        return out
    }

    fun lastInboxId(context: Context): Int = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(LAST_INBOX, 0)
    fun setLastInboxId(context: Context, value: Int) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(LAST_INBOX, value).apply()

    fun resolvePostId(url: String): Int {
        val slug = URL(HocUrls.normalize(url)).path.trim('/').substringAfterLast('/').trim()
        if (slug.isBlank()) return 0
        val conn = URL("https://hoc.hu/wp-json/wp/v2/posts?slug=${java.net.URLEncoder.encode(slug, "UTF-8")}&_fields=id").openConnection() as HttpURLConnection
        conn.connectTimeout = 8000; conn.readTimeout = 8000
        val raw = conn.inputStream.bufferedReader().use { it.readText() }
        val a = JSONArray(raw)
        return if (a.length() > 0) a.getJSONObject(0).optInt("id") else 0
    }

    private fun get(path: String): JSONObject {
        val conn = URL(BASE + path).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000; conn.readTimeout = 10000
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "HOC-Android/2.0")
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (conn.responseCode !in 200..299) throw IllegalStateException(JSONObject(raw).optString("message", "HTTP ${conn.responseCode}"))
        return JSONObject(raw)
    }

    private fun post(path: String, body: JSONObject): JSONObject {
        val conn = URL(BASE + path).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 8000; conn.readTimeout = 10000
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "HOC-Android/2.0")
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (conn.responseCode !in 200..299) throw IllegalStateException(runCatching { JSONObject(raw).optString("message") }.getOrNull().orEmpty().ifBlank { "HTTP ${conn.responseCode}" })
        return JSONObject(raw)
    }
}
