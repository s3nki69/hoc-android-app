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

    // A TikTok profilod címe
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
        
        // Alapbeállítások
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        
        // Fontos: TikTok-hoz kell a hardveres gyorsítás és a vegyes tartalom engedélyezése
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        // Böngésző álcázása (hogy ne butított verziót kapjunk)
        webSettings.userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        // --- SÜTIK (COOKIES) ENGEDÉLYEZÉSE ---
        // Ez oldja meg a GDPR ablak problémáját
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
                
                // Extra trükk: Megpróbáljuk elrejteni a bannereket, ha mégis maradnának
                // (Opcionális, de segíthet tisztítani a képet)
                webView.evaluateJavascript(
                    "javascript:(function() { " +
                            "document.getElementsByClassName('tiktok-cookie-banner')[0].style.display='none';" +
                            "})()", 
                    null
                )
            }
        }
    }
}
