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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class CouponsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var searchView: SearchView
    private val coupons = mutableListOf<Coupon>()
    private val filteredCoupons = mutableListOf<Coupon>()
    private lateinit var adapter: CouponsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = 
        inflater.inflate(R.layout.fragment_coupons, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerView)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)
        searchView = view.findViewById(R.id.searchView)
        setupRecyclerView()
        setupSearch()
        setupSwipeRefresh()
        loadCoupons()
    }

    private fun setupRecyclerView() {
        adapter = CouponsAdapter(filteredCoupons, { copyCouponCode(it) }, { openStore(it) })
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean { if (!query.isNullOrEmpty()) searchCoupons(query); return true }
            override fun onQueryTextChange(newText: String?): Boolean { if (newText.isNullOrEmpty()) loadCoupons(); return true }
        })
    }

    private fun setupSwipeRefresh() { swipeRefresh.setOnRefreshListener { loadCoupons() } }

    private fun searchCoupons(query: String) {
        showLoading()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val document = Jsoup.connect("https://kupon.hoc.hu/").data("search", query).userAgent("Mozilla/5.0").get()
                val loaded = parseCoupons(document)
                withContext(Dispatchers.Main) {
                    filteredCoupons.clear(); filteredCoupons.addAll(loaded); adapter.notifyDataSetChanged(); showContent()
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { showError("Hiba: ${e.message}") } }
        }
    }

    private fun loadCoupons() {
        showLoading()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val document = Jsoup.connect("https://kupon.hoc.hu/").userAgent("Mozilla/5.0").get()
                val loaded = parseCoupons(document)
                withContext(Dispatchers.Main) {
                    coupons.clear(); coupons.addAll(loaded); filteredCoupons.clear(); filteredCoupons.addAll(loaded); adapter.notifyDataSetChanged(); showContent()
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { showError("Hiba: ${e.message}") } }
        }
    }

    private fun parseCoupons(doc: org.jsoup.nodes.Document): List<Coupon> {
        val list = mutableListOf<Coupon>()
        doc.select(".coupon-item, .deal-item, article, .product-card").forEach { item ->
            val title = item.select("h2, h3, .title").text()
            val code = item.select(".code, .coupon-code, strong").text().ifEmpty { "Kód az oldalon" }
            val link = item.select("a").attr("abs:href")
            if (title.isNotEmpty() && link.isNotEmpty()) {
                list.add(Coupon(title, item.select(".store").text(), code, item.select(".discount").text(), link))
            }
        }
        return list
    }

    private fun copyCouponCode(coupon: Coupon) {
        val cb = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("Coupon", coupon.code))
        Toast.makeText(context, "Kód másolva!", Toast.LENGTH_SHORT).show()
    }

    private fun openStore(coupon: Coupon) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(coupon.link))) }
    private fun showLoading() { progressBar.visibility = View.VISIBLE; recyclerView.visibility = View.GONE; errorText.visibility = View.GONE; swipeRefresh.isRefreshing = false }
    private fun showContent() { progressBar.visibility = View.GONE; recyclerView.visibility = View.VISIBLE; errorText.visibility = View.GONE; swipeRefresh.isRefreshing = false }
    private fun showError(msg: String) { progressBar.visibility = View.GONE; recyclerView.visibility = View.GONE; errorText.visibility = View.VISIBLE; errorText.text = msg; swipeRefresh.isRefreshing = false }
}
