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
import org.json.JSONObject
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
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        adapter = ArticlesAdapter(filteredArticles) { article ->
            openArticle(article.link)
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }
    
    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                filterArticles(newText ?: "")
                return true
            }
        })
    }
    
    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadArticles()
        }
    }
    
    private fun filterArticles(query: String) {
        filteredArticles.clear()
        if (query.isEmpty()) {
            filteredArticles.addAll(articles)
        } else {
            filteredArticles.addAll(
                articles.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.excerpt.contains(query, ignoreCase = true)
                }
            )
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
                    
                    // Cím dekódolása (HTML entitások javítása)
                    val rawTitle = post.getJSONObject("title").getString("rendered")
                    val title = Html.fromHtml(rawTitle, Html.FROM_HTML_MODE_LEGACY).toString()
                    
                    val excerpt = post.getJSONObject("excerpt").getString("rendered")
                        .replace("<[^>]*>".toRegex(), "")
                        .replace("&hellip;", "...")
                        .trim()
                    val link = post.getString("link")
                    val date = post.getString("date").substring(0, 10)
                    
                    var imageUrl: String? = null
                    try {
                        val embedded = post.getJSONObject("_embedded")
                        if (embedded.has("wp:featuredmedia")) {
                            val media = embedded.getJSONArray("wp:featuredmedia")
                            if (media.length() > 0) {
                                val mediaObj = media.getJSONObject(0)
                                // Megpróbáljuk a teljes méretű képet kiszedni, függetlenül attól, hol van tárolva
                                imageUrl = mediaObj.getString("source_url")
                            }
                        }
                    } catch (e: Exception) { }
                    
                    loadedArticles.add(
                        Article(
                            id = post.getInt("id"),
                            title = title,
                            excerpt = excerpt,
                            link = link,
                            imageUrl = imageUrl,
                            date = date
                        )
                    )
                }
                
                withContext(Dispatchers.Main) {
                    articles.clear()
                    articles.addAll(loadedArticles)
                    filterArticles(searchView.query.toString())
                    showContent()
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError(e.message ?: "Ismeretlen hiba")
                }
            }
        }
    }
    
    private fun openArticle(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
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
