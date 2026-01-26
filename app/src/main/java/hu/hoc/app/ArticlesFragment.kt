package hu.hoc.app

import android.content.Intent
import android.net.Uri
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URL

// EZT A SORT HAGYTA EL A RENDSZER KORÁBBAN - NÉLKÜLE NEM MŰKÖDIK:
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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
        loadArticles()
    }

    private fun setupRecyclerView() {
        adapter = ArticlesAdapter(filteredArticles) { article -> openArticle(article.link) }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterArticles(newText ?: "")
                return true
            }
        })
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener { loadArticles() }
    }

    private fun filterArticles(query: String) {
        filteredArticles.clear()
        if (query.isEmpty()) {
            filteredArticles.addAll(articles)
        } else {
            filteredArticles.addAll(articles.filter { 
                it.title.contains(query, ignoreCase = true) || it.excerpt.contains(query, ignoreCase = true) 
            })
        }
        adapter.notifyDataSetChanged()
    }

    private fun loadArticles() {
        showLoading()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://www.hoc.hu/wp-json/wp/v2/posts?per_page=50&_embed"
                val response = URL(url).readText()
                val jsonArray = org.json.JSONArray(response)
                val loadedArticles = mutableListOf<Article>()

                for (i in 0 until jsonArray.length()) {
                    val post = jsonArray.getJSONObject(i)
                    
                    // Cím és kivonat tisztítása
                    val rawTitle = post.getJSONObject("title").getString("rendered")
                    val title = Html.fromHtml(rawTitle, Html.FROM_HTML_MODE_LEGACY).toString()
                    val rawExcerpt = post.getJSONObject("excerpt").getString("rendered")
                    val excerpt = Html.fromHtml(rawExcerpt, Html.FROM_HTML_MODE_LEGACY).toString().trim()

                    // Képkeresés 3 szinten (API -> Jetpack -> Tartalom elemzése)
                    var imageUrl: String? = null
                    try {
                        // 1. API Embedded
                        if (post.has("_embedded")) {
                            val embedded = post.getJSONObject("_embedded")
                            if (embedded.has("wp:featuredmedia")) {
                                val media = embedded.getJSONArray("wp:featuredmedia")
                                if (media.length() > 0) {
                                    imageUrl = media.getJSONObject(0).optString("source_url")
                                }
                            }
                        }
                        // 2. Jetpack / Cloud
                        if (imageUrl.isNullOrEmpty() && post.has("jetpack_featured_media_url")) {
                            imageUrl = post.getString("jetpack_featured_media_url")
                        }
                        // 3. Fallback: Keresés a HTML tartalomban (ez megoldja a felhős képeket)
                        if (imageUrl.isNullOrEmpty()) {
                            val contentHtml = post.getJSONObject("content").getString("rendered")
                            val doc = Jsoup.parse(contentHtml)
                            val img = doc.select("img").first()
                            if (img != null) {
                                imageUrl = img.attr("src")
                            }
                        }
                    } catch (e: Exception) { }

                    loadedArticles.add(Article(
                        id = post.getInt("id"),
                        title = title,
                        excerpt = excerpt,
                        link = post.getString("link"),
                        imageUrl = imageUrl,
                        date = post.getString("date").substring(0, 10)
                    ))
                }

                withContext(Dispatchers.Main) {
                    articles.clear()
                    articles.addAll(loadedArticles)
                    filterArticles(searchView.query.toString())
                    showContent()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Hiba: ${e.message}")
                }
            }
        }
    }

    private fun openArticle(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
    
    private fun showLoading() { progressBar.visibility = View.VISIBLE; recyclerView.visibility = View.GONE; errorText.visibility = View.GONE; swipeRefresh.isRefreshing = false }
    private fun showContent() { progressBar.visibility = View.GONE; recyclerView.visibility = View.VISIBLE; errorText.visibility = View.GONE; swipeRefresh.isRefreshing = false }
    private fun showError(message: String) { progressBar.visibility = View.GONE; recyclerView.visibility = View.GONE; errorText.visibility = View.VISIBLE; errorText.text = message; swipeRefresh.isRefreshing = false }
}
