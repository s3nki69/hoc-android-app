package hu.hoc.app

object HocUrls {
    const val SITE = "https://hoc.hu"
    const val API = "$SITE/wp-json/hoc-notify/v1/"

    /**
     * A hoc.hu és a www.hoc.hu egyaránt érvényes. Csak a nem titkosított
     * http:// URL-eket emeljük HTTPS-re, hostot nem írunk át.
     */
    fun normalize(url: String?): String {
        if (url.isNullOrBlank()) return ""
        return url
            .replace("http://www.hoc.hu", "https://www.hoc.hu", ignoreCase = true)
            .replace("http://hoc.hu", "https://hoc.hu", ignoreCase = true)
    }
}
