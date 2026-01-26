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
                    filterCoupons("")
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
                    it.store.contains(query, ignoreCase = true) ||
                    it.discount.contains(query, ignoreCase = true)
                }
            )
        }
        adapter.notifyDataSetChanged()
    }
    
    private fun searchCoupons(query: String) {
        showLoading()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val searchUrl = "https://kupon.hoc.hu/?search=${Uri.encode(query)}"
                val document = Jsoup.connect(searchUrl).get()
                
                val loadedCoupons = mutableListOf<Coupon>()
                val items = document.select(".deal-item, .coupon-item, article, .product-item")
                
                items.forEach { item ->
                    try {
                        val title = item.select("h2, h3, .title, .product-title").text()
                        val store = item.select(".store, .shop-name").text().ifEmpty { 
                            item.select("a[href*=store]").text().ifEmpty { "N/A" }
                        }
                        val code = item.select(".coupon-code, code, .code").text().ifEmpty { "N/A" }
                        val discount = item.select(".discount, .price, .deal-price").text().ifEmpty { "-" }
                        val link = item.select("a").attr("abs:href")
                        
                        if (title.isNotEmpty() && link.isNotEmpty()) {
                            loadedCoupons.add(
                                Coupon(
                                    title = title,
                                    store = store,
                                    code = code,
                                    discount = discount,
                                    link = link
                                )
                            )
                        }
                    } catch (e: Exception) {
                        // Skip this item
                    }
                }
                
                withContext(Dispatchers.Main) {
                    coupons.clear()
                    coupons.addAll(loadedCoupons)
                    filterCoupons("")
                    showContent()
                    
                    if (loadedCoupons.isEmpty()) {
                        Toast.makeText(context, "Nincs találat", Toast.LENGTH_SHORT).show()
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError(e.message ?: "Ismeretlen hiba")
                }
            }
        }
    }
    
    private fun loadCoupons() {
        showLoading()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://kupon.hoc.hu"
                val document = Jsoup.connect(url).get()
                
                val loadedCoupons = mutableListOf<Coupon>()
                val items = document.select(".deal-item, .coupon-item, article, .product-item")
                
                items.take(20).forEach { item ->
                    try {
                        val title = item.select("h2, h3, .title, .product-title").text()
                        val store = item.select(".store, .shop-name").text().ifEmpty { 
                            item.select("a[href*=store]").text().ifEmpty { "N/A" }
                        }
                        val code = item.select(".coupon-code, code, .code").text().ifEmpty { "N/A" }
                        val discount = item.select(".discount, .price, .deal-price").text().ifEmpty { "-" }
                        val link = item.select("a").attr("abs:href")
                        
                        if (title.isNotEmpty() && link.isNotEmpty()) {
                            loadedCoupons.add(
                                Coupon(
                                    title = title,
                                    store = store,
                                    code = code,
                                    discount = discount,
                                    link = link
                                )
                            )
                        }
                    } catch (e: Exception) {
                        // Skip this item
                    }
                }
                
                withContext(Dispatchers.Main) {
                    coupons.clear()
                    coupons.addAll(loadedCoupons)
                    filterCoupons("")
                    showContent()
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError(e.message ?: "Ismeretlen hiba")
                }
            }
        }
    }
    
    private fun copyCouponCode(coupon: Coupon) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Coupon Code", coupon.code)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, getString(R.string.coupon_copied), Toast.LENGTH_SHORT).show()
    }
    
    private fun openStore(coupon: Coupon) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(coupon.link))
        startActivity(intent)
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
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
