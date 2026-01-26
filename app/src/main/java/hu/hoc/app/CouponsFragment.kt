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
    
    private val coupons = mutableListOf<Coupon>()
    private val filteredCoupons = mutableListOf<Coupon>()
    private lateinit var adapter: CouponsAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_coupons, container, false)
    }
    
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
        adapter = CouponsAdapter(
            coupons = filteredCoupons,
            onCopyClick = { coupon -> copyCouponCode(coupon) },
            onStoreClick = { coupon -> openStore(coupon) }
        )
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }
    
    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchCoupons(query)
                }
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    loadCoupons()
                }
                return true
            }
        })
    }
    
    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadCoupons()
        }
    }
    
    private fun filterCoupons(query: String) {
        filteredCoupons.clear()
        if (query.isEmpty()) {
            filteredCoupons.addAll(coupons)
        } else {
            filteredCoupons.addAll(
                coupons.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.store.contains(query, ignoreCase = true)
                }
            )
        }
        adapter.notifyDataSetChanged()
    }
    
    private fun searchCoupons(query: String) {
        showLoading()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Laravel kereső URL kezelése
                val searchUrl = "https://kupon.hoc.hu/?search=${Uri.encode(query)}"
                val document = Jsoup.connect(searchUrl)
                    .userAgent("Mozilla/5.0")
                    .get()
                
                val loadedCoupons = parseCoupons(document)
                
                withContext(Dispatchers.Main) {
                    coupons.clear()
                    coupons.addAll(loadedCoupons)
                    filterCoupons("")
                    showContent()
                    if (loadedCoupons.isEmpty()) {
                        Toast.makeText(context, "Nincs találat a kuponok között", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError(e.message ?: "Hiba a keresés során")
                }
            }
        }
    }
    
    private fun loadCoupons() {
        showLoading()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val document = Jsoup.connect("https://kupon.hoc.hu")
                    .userAgent("Mozilla/5.0")
                    .get()
                
                val loadedCoupons = parseCoupons(document)
                
                withContext(Dispatchers.Main) {
                    coupons.clear()
                    coupons.addAll(loadedCoupons)
                    filterCoupons("")
                    showContent()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError(e.message ?: "Hiba a letöltéskor")
                }
            }
        }
    }

    private fun parseCoupons(document: org.jsoup.nodes.Document): List<Coupon> {
        val list = mutableListOf<Coupon>()
        // Rugalmasabb elemkeresés a Laravel struktúrához
        val items = document.select(".deal-item, .coupon-item, article, .product-card")
        
        items.forEach { item ->
            try {
                val title = item.select("h2, h3, .title").text()
                val store = item.select(".store, .shop").text().ifEmpty { "Bolt" }
                val code = item.select(".coupon-code, code").text().ifEmpty { "Nincs kód" }
                val discount = item.select(".discount, .price").text().ifEmpty { "-" }
                val link = item.select("a").first()?.attr("abs:href") ?: ""
                
                if (title.isNotEmpty()) {
                    list.add(Coupon(title, store, code, discount, link))
                }
            } catch (e: Exception) { }
        }
        return list
    }
    
    private fun copyCouponCode(coupon: Coupon) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Coupon Code", coupon.code)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Kód másolva!", Toast.LENGTH_SHORT).show()
    }
    
    private fun openStore(coupon: Coupon) {
        if (coupon.link.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(coupon.link))
            startActivity(intent)
        }
    }
    
    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        errorText.visibility = View.GONE
        swipeRefresh.isRefreshing = false
    }
    
    private fun showContent() {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        swipeRefresh.isRefreshing = false
    }
    
    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.GONE
        errorText.visibility = View.VISIBLE
        errorText.text = "Hiba: $message"
        swipeRefresh.isRefreshing = false
    }
}
