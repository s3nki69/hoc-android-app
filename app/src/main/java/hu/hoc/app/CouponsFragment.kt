package hu.hoc.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup


data class Coupon(
    val title: String,
    val store: String,
    val code: String,
    val discount: String,
    val link: String
)

class CouponsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var searchView: SearchView
    private lateinit var resultTitle: TextView

    private val coupons = mutableListOf<Coupon>()
    private val filteredCoupons = mutableListOf<Coupon>()
    private lateinit var adapter: CouponsAdapter

    private val couponHome = "https://kupon.hoc.hu"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_coupons, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerView)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)
        searchView = view.findViewById(R.id.searchView)
        resultTitle = view.findViewById(R.id.resultTitle)

        adapter = CouponsAdapter(filteredCoupons, ::copyCouponCode, ::openStore)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        view.findViewById<View>(R.id.storeAll).setOnClickListener { openCouponPage(couponHome) }
        view.findViewById<View>(R.id.storeVevor).setOnClickListener { openCouponPage("$couponHome/kategoria/vevor") }
        view.findViewById<View>(R.id.storeAllegro).setOnClickListener { openCouponPage("$couponHome/kategoria/allegro") }
        view.findViewById<View>(R.id.storeAliexpress).setOnClickListener { openCouponPage("$couponHome/kategoria/aliexpress") }
        view.findViewById<View>(R.id.storeBanggood).setOnClickListener { openCouponPage("$couponHome/kategoria/banggood") }
        view.findViewById<View>(R.id.storeGeekbuying).setOnClickListener { openCouponPage("$couponHome/kategoria/geekbuying") }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    resultTitle.text = "Találatok: \"$query\""
                    searchCoupons(query)
                    searchView.clearFocus()
                }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    resultTitle.text = "Legfrissebb kuponok"
                    filterCoupons("")
                }
                return true
            }
        })
        swipeRefresh.setOnRefreshListener { loadCoupons(showSpinner = false) }

        val cached = CacheStore.loadCoupons(requireContext())
        if (cached.isNotEmpty()) {
            coupons.addAll(cached)
            filterCoupons("")
            showContent()
        }
        loadCoupons(showSpinner = cached.isEmpty())
    }

    private fun filterCoupons(query: String) {
        filteredCoupons.clear()
        if (query.isBlank()) filteredCoupons.addAll(coupons)
        else filteredCoupons.addAll(coupons.filter {
            it.title.contains(query, true) || it.store.contains(query, true) || it.discount.contains(query, true)
        })
        adapter.notifyDataSetChanged()
    }

    private fun searchCoupons(query: String) = fetchCoupons("$couponHome/?search=${Uri.encode(query)}", cache = false)

    private fun loadCoupons(showSpinner: Boolean) = fetchCoupons(couponHome, cache = true, showSpinner = showSpinner)

    private fun fetchCoupons(url: String, cache: Boolean, showSpinner: Boolean = true) {
        if (showSpinner) showLoading()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    val document = Jsoup.connect(url).userAgent("HOC-Android/2.0.4").timeout(10000).get()
                    document.select(".deal-item, .coupon-item, article, .product-item").take(if (cache) 20 else 50).mapNotNull { item ->
                        val title = item.select("h2, h3, .title, .product-title").text()
                        val store = item.select(".store, .shop-name").text().ifEmpty { item.select("a[href*=store]").text().ifEmpty { "N/A" } }
                        val code = item.select(".coupon-code, code, .code").text().ifEmpty { "N/A" }
                        val discount = item.select(".discount, .price, .deal-price").text().ifEmpty { "-" }
                        val link = item.select("a").attr("abs:href")
                        if (title.isNotBlank() && link.isNotBlank()) Coupon(title, store, code, discount, link) else null
                    }
                }
                coupons.clear(); coupons.addAll(loaded)
                if (cache) CacheStore.saveCoupons(requireContext(), loaded)
                filterCoupons("")
                showContent()
                if (loaded.isEmpty()) {
                    errorText.visibility = View.VISIBLE
                    errorText.text = "Nem találtam helyben megjeleníthető kuponokat. A kereső és az áruházi gyorsgombok továbbra is használhatók."
                }
            } catch (e: Exception) {
                if (coupons.isEmpty()) showError(e.message ?: "Ismeretlen hiba")
                else Toast.makeText(context, "A kuponok frissítése most nem sikerült", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun copyCouponCode(coupon: Coupon) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Coupon Code", coupon.code))
        Toast.makeText(context, getString(R.string.coupon_copied), Toast.LENGTH_SHORT).show()
    }

    private fun openStore(coupon: Coupon) = openCouponPage(coupon.link)

    private fun openCouponPage(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { Toast.makeText(context, "A hivatkozás nem nyitható meg", Toast.LENGTH_SHORT).show() }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = if (coupons.isEmpty()) View.GONE else View.VISIBLE
        errorText.visibility = View.GONE
    }

    private fun showContent() {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        errorText.visibility = View.GONE
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.GONE
        errorText.visibility = View.VISIBLE
        errorText.text = "Hiba: $message"
    }
}
