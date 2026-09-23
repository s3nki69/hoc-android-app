package hu.hoc.app

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationsFragment : Fragment() {
    private lateinit var topicList: LinearLayout
    private lateinit var watchList: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var saveButton: Button
    private val topicChecks = linkedMapOf<Int, CheckBox>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        topicList = view.findViewById(R.id.topicList)
        watchList = view.findViewById(R.id.watchList)
        progress = view.findViewById(R.id.notifyProgress)
        saveButton = view.findViewById(R.id.saveTopics)
        saveButton.setOnClickListener { saveTopics() }
        view.findViewById<Button>(R.id.openWebSettings).setOnClickListener {
            startActivity(Intent(requireContext(), ArticleActivity::class.java).putExtra(ArticleActivity.EXTRA_URL, "https://www.hoc.hu/ertesitesek/"))
        }
        loadData()
    }

    private fun loadData() {
        progress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    NativeApi.ensureRegistered(requireContext())
                    Triple(NativeApi.appConfig(), NativeApi.status(requireContext()).second, NativeApi.watchlist(requireContext()))
                }
                renderTopics(result.first, result.second)
                renderWatches(result.third)
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Az értesítési beállítások nem tölthetők be", Toast.LENGTH_LONG).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun renderTopics(topics: List<NativeApi.Topic>, selected: List<Int>) {
        topicList.removeAllViews(); topicChecks.clear()
        topics.forEach { t ->
            val cb = CheckBox(requireContext()).apply {
                text = t.name
                isChecked = t.id in selected
                setPadding(0, 6, 0, 6)
            }
            topicChecks[t.id] = cb
            topicList.addView(cb)
        }
        saveButton.isEnabled = topics.isNotEmpty()
    }

    private fun saveTopics() {
        val selected = topicChecks.filterValues { it.isChecked }.keys.toList()
        if (selected.isEmpty()) {
            Toast.makeText(context, "Válassz legalább egy témát", Toast.LENGTH_SHORT).show()
            return
        }
        saveButton.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { NativeApi.saveCategories(requireContext(), selected) }.getOrDefault(false) }
            saveButton.isEnabled = true
            Toast.makeText(context, if (ok) "Témák elmentve" else "A mentés nem sikerült", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderWatches(items: List<NativeApi.WatchItem>) {
        watchList.removeAllViews()
        if (items.isEmpty()) {
            watchList.addView(TextView(requireContext()).apply { text = "Még nincs figyelt cikked vagy terméked." })
            return
        }
        items.forEach { w ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 10, 0, 10)
            }
            val title = TextView(requireContext()).apply {
                text = w.title
                setTypeface(typeface, Typeface.BOLD)
                textSize = 15f
                setOnClickListener { startActivity(Intent(requireContext(), ArticleActivity::class.java).putExtra(ArticleActivity.EXTRA_URL, w.url)) }
            }
            val remove = Button(requireContext()).apply {
                text = "Figyelés törlése"
                setOnClickListener {
                    isEnabled = false
                    viewLifecycleOwner.lifecycleScope.launch {
                        val ok = withContext(Dispatchers.IO) { runCatching { NativeApi.unwatch(requireContext(), w.id) }.getOrDefault(false) }
                        if (ok) loadData() else { isEnabled = true; Toast.makeText(context, "Nem sikerült törölni", Toast.LENGTH_SHORT).show() }
                    }
                }
            }
            row.addView(title)
            row.addView(remove)
            watchList.addView(row)
        }
    }
}
