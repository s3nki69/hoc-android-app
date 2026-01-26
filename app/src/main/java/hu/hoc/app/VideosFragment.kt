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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL

data class Video(val id: String, val title: String, val thumbnail: String, val link: String)

class VideosFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var searchView: SearchView
    private val videos = mutableListOf<Video>()
    private val filteredVideos = mutableListOf<Video>()
    private lateinit var adapter: VideosAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = 
        inflater.inflate(R.layout.fragment_videos, container, false)

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
        loadVideos()
    }

    private fun setupRecyclerView() {
        adapter = VideosAdapter(filteredVideos) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.link))) }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean = false
            override fun onQueryTextChange(query: String?): Boolean { filterVideos(query ?: ""); return true }
        })
    }

    private fun setupSwipeRefresh() { swipeRefresh.setOnRefreshListener { loadVideos() } }

    private fun filterVideos(query: String) {
        filteredVideos.clear()
        if (query.isEmpty()) filteredVideos.addAll(videos)
        else filteredVideos.addAll(videos.filter { it.title.contains(query, ignoreCase = true) })
        adapter.notifyDataSetChanged()
    }

    private fun loadVideos() {
        showLoading()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://www.hoc.hu/wp-json/wp/v2/posts?per_page=30&_embed"
                val response = URL(url).readText()
                val jsonArray = JSONArray(response)
                val loaded = mutableListOf<Video>()
                for (i in 0 until jsonArray.length()) {
                    val post = jsonArray.getJSONObject(i)
                    val title = Html.fromHtml(post.getJSONObject("title").getString("rendered"), Html.FROM_HTML_MODE_LEGACY).toString()
                    var thumb = ""
                    if (post.has("_embedded")) {
                        val media = post.getJSONObject("_embedded").optJSONArray("wp:featuredmedia")?.optJSONObject(0)
                        thumb = media?.optString("source_url") ?: ""
                    }
                    if (thumb.isNotEmpty()) loaded.add(Video(post.getInt("id").toString(), title, thumb, post.getString("link")))
                }
                withContext(Dispatchers.Main) {
                    videos.clear(); videos.addAll(loaded); filterVideos(searchView.query.toString()); showContent()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    videos.clear(); videos.add(Video("1", "HOC TV Megnyitása", "", "https://www.youtube.com/@HOCTvChannel")); filterVideos(""); showContent()
                }
            }
        }
    }

    private fun showLoading() { progressBar.visibility = View.VISIBLE; recyclerView.visibility = View.GONE; errorText.visibility = View.GONE; swipeRefresh.isRefreshing = false }
    private fun showContent() { progressBar.visibility = View.GONE; recyclerView.visibility = View.VISIBLE; errorText.visibility = View.GONE; swipeRefresh.isRefreshing = false }
}
