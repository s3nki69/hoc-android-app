package hu.hoc.app

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment

// Adatmodell megtartása a kompatibilitás miatt
data class Coupon(
    val title: String,
    val store: String,
    val code: String,
    val discount: String,
    val link: String
)

class CouponsFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_coupons, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = view.findViewById(R.id.couponWebView)
        progressBar = view.findViewById(R.id.progressBar)

        setupWebView()

        // Vissza gomb kezelése: ha már böngészel, ne lépjen ki, hanem menjen vissza
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    // Ha a helyi kezdőlapon vagyunk és vissza nyomunk, akkor engedjük át a vezérlést (kilépés/főmenü)
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        })

        // ITT A VÁLTOZÁS: Nem az internetes URL-t töltjük be, hanem a helyi fájlt
        // Ez azonnal meg fog jelenni internetkapcsolat nélkül is
        webView.loadUrl("file:///android_asset/coupon_landing.html")
    }

    private fun setupWebView() {
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        
        // Gyorsítótár bekapcsolása a későbbi betöltésekhez
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                // False-t adunk vissza, így minden linket (és a keresés eredményét) 
                // a WebView-n belül nyit meg, nem dob ki a Chrome-ba.
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                // Csak akkor mutatunk töltést, ha nem a helyi fájlt töltjük
                if (url != null && !url.contains("file:///android_asset")) {
                    progressBar.visibility = View.VISIBLE
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }
        }
    }
}
