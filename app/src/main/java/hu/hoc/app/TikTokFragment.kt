package hu.hoc.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment

class TikTokFragment : Fragment() {
    private lateinit var webView: WebView
    private lateinit var backCallback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_tiktok, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView = view.findViewById(R.id.tiktokWebView)
        setupWebView()

        backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)

        if (savedInstanceState == null) {
            webView.loadUrl(TIKTOK_URL)
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    private fun setupWebView() {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = MOBILE_USER_AGENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setAcceptThirdPartyCookies(webView, true)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return handleUrl(request.url)
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return handleUrl(Uri.parse(url))
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                backCallback.isEnabled = view.canGoBack()
                hideCookieBanner(view)
            }
        }
    }

    private fun handleUrl(uri: Uri): Boolean {
        val host = uri.host.orEmpty().lowercase()
        if (host == "tiktok.com" || host.endsWith(".tiktok.com")) return false
        return runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        }.getOrDefault(false)
    }

    private fun hideCookieBanner(view: WebView) {
        val script = """
            (function() {
                var css = 'div[class*="cookie-banner"], div[class*="CookieBanner"], #tiktok-cookie-banner { display: none !important; }';
                var head = document.head || document.getElementsByTagName('head')[0];
                if (!head || document.getElementById('hoc-tiktok-style')) return;
                var style = document.createElement('style');
                style.id = 'hoc-tiktok-style';
                style.type = 'text/css';
                style.appendChild(document.createTextNode(css));
                head.appendChild(style);
            })();
        """.trimIndent()
        view.evaluateJavascript(script, null)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::webView.isInitialized) webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        if (::webView.isInitialized) webView.stopLoading()
        super.onDestroyView()
    }

    companion object {
        private const val TIKTOK_URL = "https://www.tiktok.com/@hoc.hu"
        private const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
