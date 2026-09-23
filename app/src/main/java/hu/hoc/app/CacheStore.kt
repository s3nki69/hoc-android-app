package hu.hoc.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object CacheStore {
    private const val PREFS = "hoc_cache"
    private const val ARTICLES = "articles_v2"
    private const val ARTICLES_TS = "articles_ts_v2"
    private const val VIDEOS = "videos_v2"
    private const val VIDEOS_TS = "videos_ts_v2"
    private const val COUPONS = "coupons_v2"
    private const val COUPONS_TS = "coupons_ts_v2"

    fun saveArticles(context: Context, items: List<Article>) {
        val a = JSONArray()
        items.forEach { item ->
            a.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("excerpt", item.excerpt)
                put("link", item.link)
                put("imageUrl", item.imageUrl ?: JSONObject.NULL)
                put("date", item.date)
            })
        }
        prefs(context).edit().putString(ARTICLES, a.toString()).putLong(ARTICLES_TS, System.currentTimeMillis()).apply()
    }

    fun loadArticles(context: Context): List<Article> = runCatching {
        val raw = prefs(context).getString(ARTICLES, null) ?: return emptyList()
        val a = JSONArray(raw)
        buildList {
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                add(Article(
                    id = o.getInt("id"),
                    title = o.getString("title"),
                    excerpt = o.getString("excerpt"),
                    link = o.getString("link"),
                    imageUrl = if (o.isNull("imageUrl")) null else o.getString("imageUrl"),
                    date = o.getString("date")
                ))
            }
        }
    }.getOrDefault(emptyList())

    fun saveVideos(context: Context, items: List<Video>) {
        val a = JSONArray()
        items.forEach { item ->
            a.put(JSONObject().apply {
                put("id", item.id); put("title", item.title); put("thumbnail", item.thumbnail)
                put("duration", item.duration); put("views", item.views); put("publishedAt", item.publishedAt)
            })
        }
        prefs(context).edit().putString(VIDEOS, a.toString()).putLong(VIDEOS_TS, System.currentTimeMillis()).apply()
    }

    fun loadVideos(context: Context): List<Video> = runCatching {
        val raw = prefs(context).getString(VIDEOS, null) ?: return emptyList()
        val a = JSONArray(raw)
        buildList {
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                add(Video(o.getString("id"), o.getString("title"), o.getString("thumbnail"), o.optString("duration"), o.optString("views"), o.optString("publishedAt")))
            }
        }
    }.getOrDefault(emptyList())

    fun saveCoupons(context: Context, items: List<Coupon>) {
        val a = JSONArray()
        items.forEach { item ->
            a.put(JSONObject().apply {
                put("title", item.title); put("store", item.store); put("code", item.code); put("discount", item.discount); put("link", item.link)
            })
        }
        prefs(context).edit().putString(COUPONS, a.toString()).putLong(COUPONS_TS, System.currentTimeMillis()).apply()
    }

    fun loadCoupons(context: Context): List<Coupon> = runCatching {
        val raw = prefs(context).getString(COUPONS, null) ?: return emptyList()
        val a = JSONArray(raw)
        buildList {
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                add(Coupon(o.getString("title"), o.getString("store"), o.getString("code"), o.getString("discount"), o.getString("link")))
            }
        }
    }.getOrDefault(emptyList())

    fun ageMinutes(context: Context, kind: String): Long {
        val key = when(kind) { "articles" -> ARTICLES_TS; "videos" -> VIDEOS_TS; else -> COUPONS_TS }
        val ts = prefs(context).getLong(key, 0L)
        return if (ts == 0L) Long.MAX_VALUE else (System.currentTimeMillis() - ts) / 60000L
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
