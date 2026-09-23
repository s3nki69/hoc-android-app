package hu.hoc.app

import android.content.Intent
import android.os.Bundle
import android.text.Html
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
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


data class Article(
    val id: Int,
    val title: String,
    val excerpt: String,
    val link: String,
    val imageUrl: String?,
    val date: String
)

class ArticlesFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var searchView: SearchView

    private val articles = mutableListOf<Article>()
    private val filteredArticles = mutableListOf<Article>()
    private lateinit var adapter: ArticlesAdapter

    private var currentPage = 1
    private var isLoading = false
    private var endReached = false
    private val pageSize = 15

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_articles, container, false)
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

        val cached = CacheStore.loadArticles(requireContext())
        if (cached.isNotEmpty()) {
            articles.addAll(cached)
            filterArticles("")
            showContent()
        }

        // Cache esetén azonnal van tartalom, a frissítés háttérben fut.
        loadArticles(page = 1, replace = true, showSpinner = cached.isEmpty())
    }

    private fun setupRecyclerView() {
        adapter = ArticlesAdapter(filteredArticles) { article ->
            startActivity(Intent(requireContext(), ArticleActivity::class.java)
                .putExtra(ArticleActivity.EXTRA_URL, article.link)
                .putExtra(ArticleActivity.EXTRA_POST_ID, article.id))
        }
        val lm = LinearLayoutManager(context)
        recyclerView.layoutManager = lm
        recyclerView.adapter = adapter
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                if (dy <= 0 || isLoading || endReached || searchView.query.isNotEmpty()) return
                val last = lm.findLastVisibleItemPosition()
                if (last >= adapter.itemCount - 4) loadArticles(currentPage + 1, replace = false, showSpinner = false)
            }
        })
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterArticles(newText.orEmpty())
                return true
            }
        })
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            currentPage = 1
            endReached = false
            loadArticles(page = 1, replace = true, showSpinner = false)
        }
    }

    private fun filterArticles(query: String) {
        filteredArticles.clear()
        if (query.isBlank()) filteredArticles.addAll(articles)
        else filteredArticles.addAll(articles.filter {
            it.title.contains(query, ignoreCase = true) || it.excerpt.contains(query, ignoreCase = true)
        })
        adapter.notifyDataSetChanged()
    }

    private fun loadArticles(page: Int, replace: Boolean, showSpinner: Boolean) {
        if (isLoading) return
        isLoading = true
        if (showSpinner) showLoading()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) { fetchPage(page) }
                if (!isAdded) return@launch

                if (replace) {
                    articles.clear()
                    articles.addAll(loaded)
                    currentPage = 1
                    // Az első oldal cache-elése elég az azonnali induláshoz.
                    CacheStore.saveArticles(requireContext(), loaded)
                } else {
                    val known = articles.asSequence().map { it.id }.toHashSet()
                    articles.addAll(loaded.filterNot { it.id in known })
                    currentPage = page
                }
                endReached = loaded.size < pageSize
                filterArticles(searchView.query.toString())
                showContent()
            } catch (e: Exception) {
                if (articles.isEmpty()) showError(e.message ?: "Ismeretlen hiba")
                else Toast.makeText(context, "A frissítés most nem sikerült", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun fetchPage(page: Int): List<Article> {
        val endpoint = "https://www.hoc.hu/wp-json/wp/v2/posts?per_page=$pageSize&page=$page&_embed=wp:featuredmedia"
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 10000
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "HOC-Android/2.0")
        conn.inputStream.bufferedReader().use { reader ->
            val jsonArray = JSONArray(reader.readText())
            return buildList {
                for (i in 0 until jsonArray.length()) {
                    val post = jsonArray.getJSONObject(i)
                    add(parseArticle(post))
                }
            }
        }
    }

    private fun parseArticle(post: JSONObject): Article {
        val title = decodeHtml(post.getJSONObject("title").optString("rendered"))
        val excerpt = decodeHtml(post.getJSONObject("excerpt").optString("rendered"))
            .replace(Regex("\\s+"), " ").trim()
        val link = post.optString("link")
        val date = post.optString("date").take(10)
        var imageUrl: String? = null

        runCatching {
            val media = post.optJSONObject("_embedded")?.optJSONArray("wp:featuredmedia")
            if (media != null && media.length() > 0) {
                val m = media.getJSONObject(0)
                val sizes = m.optJSONObject("media_details")?.optJSONObject("sizes")
                imageUrl = sizes?.optJSONObject("medium_large")?.optString("source_url")
                    ?.takeIf { it.isNotBlank() }
                    ?: sizes?.optJSONObject("medium")?.optString("source_url")?.takeIf { it.isNotBlank() }
                    ?: m.optString("source_url").takeIf { it.isNotBlank() }
            }
        }

        return Article(post.getInt("id"), title, excerpt, link, imageUrl, date)
    }

    @Suppress("DEPRECATION")
    private fun decodeHtml(value: String): String {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString()
        } else Html.fromHtml(value).toString()
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = if (articles.isEmpty()) View.GONE else View.VISIBLE
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
        swipeRefresh.isRefreshing = false
    }
}
