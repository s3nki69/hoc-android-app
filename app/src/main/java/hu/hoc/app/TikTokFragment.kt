package hu.hoc.app

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment

class TikTokFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    private val TIKTOK_URL = "https://www.tiktok.com/@hoc.hu"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tiktok, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = view.findViewById(R.id.tiktokWebView)
        progressBar = view.findViewById(R.id.progressBar)

        setupWebView()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        })

        webView.loadUrl(TIKTOK_URL)
    }

    private fun setupWebView() {
        val webSettings: WebSettings = webView.settings
        
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        
        // JAVÍTÁS: Jobb illeszkedés a képernyőhöz
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false

        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        webSettings.userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                
                // JAVÍTÁS: Drasztikusabb GDPR banner eltávolítás
                // Ez a szkript megkeresi a gyakori TikTok banner elemeket és elrejti őket
                webView.evaluateJavascript(
                    """
                    (function() {
                        var css = 'div[class*="cookie-banner"], div[class*="CookieBanner"], #tiktok-cookie-banner { display: none !important; }';
                        var head = document.head || document.getElementsByTagName('head')[0];
                        var style = document.createElement('style');
                        style.type = 'text/css';
                        style.appendChild(document.createTextNode(css));
                        head.appendChild(style);
                        
                        // Azonnali kényszerített eltávolítás az ismert osztályokra
                        var banners = document.querySelectorAll('div[class*="cookie-banner"], div[class*="CookieBanner"]');
                        for (var i = 0; i < banners.length; i++) {
                            banners[i].style.setProperty('display', 'none', 'important');
                        }
                    })()
                    """.trimIndent(), 
                    null
                )
            }
        }
    }
}
