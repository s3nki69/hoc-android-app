package hu.hoc.app

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
import org.jsoup.parser.Parser


data class Video(
    val id: String,
    val title: String,
    val thumbnail: String,
    val duration: String,
    val views: String,
    val publishedAt: String
)

class VideosFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var searchView: SearchView

    private val videos = mutableListOf<Video>()
    private val filteredVideos = mutableListOf<Video>()
    private lateinit var adapter: VideosAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_videos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerView)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)
        searchView = view.findViewById(R.id.searchView)

        adapter = VideosAdapter(filteredVideos) { openVideo(it) }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean { filterVideos(newText.orEmpty()); return true }
        })
        swipeRefresh.setOnRefreshListener { loadVideos(showSpinner = false) }

        val cached = CacheStore.loadVideos(requireContext())
        if (cached.isNotEmpty()) {
            videos.addAll(cached)
            filterVideos("")
            showContent()
        }
        loadVideos(showSpinner = cached.isEmpty())
    }

    private fun filterVideos(query: String) {
        filteredVideos.clear()
        if (query.isBlank()) filteredVideos.addAll(videos)
        else filteredVideos.addAll(videos.filter { it.title.contains(query, ignoreCase = true) })
        adapter.notifyDataSetChanged()
    }

    private fun loadVideos(showSpinner: Boolean) {
        if (showSpinner) showLoading()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) { fetchFeed() }
                videos.clear()
                videos.addAll(loaded)
                CacheStore.saveVideos(requireContext(), loaded)
                filterVideos(searchView.query.toString())
                showContent()
            } catch (e: Exception) {
                if (videos.isEmpty()) showError(e.message ?: "Ismeretlen hiba")
                else Toast.makeText(context, "A videók frissítése most nem sikerült", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun fetchFeed(): List<Video> {
        val feed = "https://www.youtube.com/feeds/videos.xml?channel_id=UCu9MacCblbUrePyC3jEmbng"
        val document = Jsoup.connect(feed)
            .userAgent("HOC-Android/2.0")
            .timeout(10000)
            .parser(Parser.xmlParser())
            .get()

        return document.getElementsByTag("entry").take(20).mapNotNull { entry ->
            val id = entry.getElementsByTag("yt:videoId").first()?.text()
                ?: entry.getElementsByTag("videoId").first()?.text()
                ?: return@mapNotNull null
            val title = entry.getElementsByTag("title").first()?.text().orEmpty()
            val published = entry.getElementsByTag("published").first()?.text()?.take(10).orEmpty()
            val thumb = entry.getElementsByTag("media:thumbnail").first()?.attr("url")
                ?.takeIf { it.isNotBlank() }
                ?: "https://i.ytimg.com/vi/$id/hqdefault.jpg"
            Video(id, title, thumb, "", published, published)
        }
    }

    private fun openVideo(video: Video) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.id}")))
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = if (videos.isEmpty()) View.GONE else View.VISIBLE
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
