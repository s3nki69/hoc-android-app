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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

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
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_videos, container, false)
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
        
        loadVideos()
    }
    
    private fun setupRecyclerView() {
        adapter = VideosAdapter(filteredVideos) { video ->
            openVideo(video)
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
                filterVideos(newText ?: "")
                return true
            }
        })
    }
    
    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadVideos()
        }
    }
    
    private fun filterVideos(query: String) {
        filteredVideos.clear()
        if (query.isEmpty()) {
            filteredVideos.addAll(videos)
        } else {
            filteredVideos.addAll(
                videos.filter {
                    it.title.contains(query, ignoreCase = true)
                }
            )
        }
        adapter.notifyDataSetChanged()
    }
    
    private fun loadVideos() {
        showLoading()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val channelUrl = "https://www.youtube.com/@HOCTvChannel/videos"
                val document = Jsoup.connect(channelUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(15000)
                    .get()
                
                val loadedVideos = mutableListOf<Video>()
                val scriptElements = document.select("script")
                
                for (script in scriptElements) {
                    val scriptContent = script.html()
                    if (scriptContent.contains("\"videoId\"")) {
                        val videoIdPattern = "\"videoId\":\"([^\"]+)\"".toRegex()
                        val titlePattern = "\"title\":\\{\"runs\":\\[\\{\"text\":\"([^\"]+)\"".toRegex()
                        
                        val videoIds = videoIdPattern.findAll(scriptContent).map { it.groupValues[1] }.toList()
                        val titles = titlePattern.findAll(scriptContent).map { it.groupValues[1] }.toList()
                        
                        for (i in 0 until minOf(videoIds.size, titles.size, 30)) {
                            val videoId = videoIds[i]
                            val title = titles[i]
                            
                            loadedVideos.add(
                                Video(
                                    id = videoId,
                                    title = title,
                                    thumbnail = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                                    duration = "",
                                    views = "",
                                    publishedAt = ""
                                )
                            )
                        }
                        if (loadedVideos.isNotEmpty()) break
                    }
                }
                
                // Ha nem sikerült videókat találni (pl. YouTube blokkolás), adjunk hozzá egy fix gombot
                if (loadedVideos.isEmpty()) {
                    loadedVideos.add(
                        Video(
                            id = "channel",
                            title = "HOC TV Channel megnyitása a YouTube-on",
                            thumbnail = "",
                            duration = "",
                            views = "",
                            publishedAt = ""
                        )
                    )
                }
                
                withContext(Dispatchers.Main) {
                    videos.clear()
                    videos.addAll(loadedVideos)
                    filterVideos(searchView.query.toString())
                    showContent()
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    videos.clear()
                    videos.add(
                        Video(
                            id = "channel",
                            title = "HOC TV Channel - Kattints a megnyitáshoz!",
                            thumbnail = "",
                            duration = "",
                            views = "",
                            publishedAt = ""
                        )
                    )
                    filterVideos("")
                    showContent()
                }
            }
        }
    }
    
    private fun openVideo(video: Video) {
        val url = if (video.id == "channel") {
            "https://www.youtube.com/@HOCTvChannel"
        } else {
            "https://www.youtube.com/watch?v=${video.id}"
        }
        
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
}
