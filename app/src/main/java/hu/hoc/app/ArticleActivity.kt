package hu.hoc.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArticleActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_POST_ID = "post_id"
    }

    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private lateinit var watchButton: Button
    private var postId: Int = 0
    private var articleUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article)

        webView = findViewById(R.id.articleWebView)
        progress = findViewById(R.id.articleProgress)
        watchButton = findViewById(R.id.watchButton)
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        findViewById<ImageButton>(R.id.shareButton).setOnClickListener { shareCurrent() }
        watchButton.setOnClickListener { watchCurrent() }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            builtInZoomControls = false
            displayZoomControls = false
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                return if (uri.host?.endsWith("hoc.hu") == true) false
                else {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    true
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progress.visibility = View.GONE
            }
        }

        articleUrl = intent.getStringExtra(EXTRA_URL) ?: intent.dataString ?: "https://www.hoc.hu/"
        postId = intent.getIntExtra(EXTRA_POST_ID, 0)
        webView.loadUrl(articleUrl)
        if (postId == 0 && articleUrl != "https://www.hoc.hu/") resolvePostId()
    }

    private fun resolvePostId() {
        lifecycleScope.launch {
            postId = withContext(Dispatchers.IO) { runCatching { NativeApi.resolvePostId(articleUrl) }.getOrDefault(0) }
            watchButton.isEnabled = postId > 0
        }
    }

    private fun watchCurrent() {
        if (postId <= 0) return
        watchButton.isEnabled = false
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    NativeApi.ensureRegistered(this@ArticleActivity)
                    NativeApi.watch(this@ArticleActivity, postId, "deal")
                }.getOrDefault(false)
            }
            watchButton.isEnabled = true
            if (ok) {
                watchButton.text = "♥"
                Toast.makeText(this@ArticleActivity, "A cikket figyeled", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(this@ArticleActivity, "A figyelést nem sikerült beállítani", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    private fun shareCurrent() {
        val url = webView.url ?: articleUrl
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(i, getString(R.string.share_article)))
    }
}
