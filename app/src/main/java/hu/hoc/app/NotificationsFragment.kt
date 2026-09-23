package hu.hoc.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationsFragment : Fragment() {
    private lateinit var topicList: LinearLayout
    private lateinit var watchList: LinearLayout
    private lateinit var inboxList: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var saveButton: Button
    private lateinit var masterSwitch: SwitchMaterial
    private lateinit var statusText: TextView
    private val topicChecks = linkedMapOf<Int, CheckBox>()
    private var bindingSwitch = false
    private data class Loaded(val config: NativeApi.AppConfig, val status: NativeApi.Status, val watches: List<NativeApi.WatchItem>, val inbox: List<NativeApi.InboxItem>)

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        topicList = view.findViewById(R.id.topicList)
        watchList = view.findViewById(R.id.watchList)
        inboxList = view.findViewById(R.id.inboxList)
        progress = view.findViewById(R.id.notifyProgress)
        saveButton = view.findViewById(R.id.saveTopics)
        masterSwitch = view.findViewById(R.id.pushMasterSwitch)
        statusText = view.findViewById(R.id.pushStatus)

        saveButton.setOnClickListener { saveTopics() }
        view.findViewById<Button>(R.id.refreshNotifications).setOnClickListener { loadData() }
        masterSwitch.setOnCheckedChangeListener { _, checked -> if (!bindingSwitch) setPushEnabled(checked) }
        loadData()
    }

    private fun loadData() {
        progress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    if (NativeApi.isPushEnabled(requireContext())) NativeApi.ensureRegistered(requireContext())
                    val config = NativeApi.appConfig()
                    val status = runCatching { NativeApi.status(requireContext()) }.getOrElse { NativeApi.Status(false, emptyList(), "native_poll", false) }
                    val watches = runCatching { NativeApi.watchlist(requireContext()) }.getOrDefault(emptyList())
                    val inbox = runCatching { NativeApi.inbox(requireContext()) }.getOrDefault(emptyList())
                    Loaded(config, status, watches, inbox)
                }
                val config = loaded.config
                val status = loaded.status
                val watches = loaded.watches
                val inbox = loaded.inbox
                bindingSwitch = true
                masterSwitch.isChecked = NativeApi.isPushEnabled(requireContext()) && status.active
                bindingSwitch = false
                renderStatus(config, status)
                renderTopics(config.topics, status.categories)
                renderInbox(inbox)
                renderWatches(watches)
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Az értesítési beállítások nem tölthetők be", Toast.LENGTH_LONG).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun renderStatus(config: NativeApi.AppConfig, status: NativeApi.Status) {
        val enabled = NativeApi.isPushEnabled(requireContext()) && status.active
        saveButton.isEnabled = enabled && config.topics.isNotEmpty()
        topicChecks.values.forEach { it.isEnabled = enabled }
        statusText.text = when {
            !enabled -> "Kikapcsolva. Az app nem kér Push értesítést és a háttérellenőrzést is leállítja."
            status.fcmRegistered -> "Azonnali natív Push aktív. A ritka háttérszinkron csak tartalék ellenőrzés."
            config.fcm.ready -> "A natív Push be van állítva; az FCM-regisztráció folyamatban van. Addig a takarékos háttérellenőrzés működik."
            else -> "Takarékos háttérellenőrzés aktív. A valódi azonnali Push a Firebase beállítása után kapcsol be."
        }
    }

    private fun setPushEnabled(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        HocFirebase.setEnabled(requireContext(), enabled)
        progress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                if (enabled) runCatching { NativeApi.ensureRegistered(requireContext()) }.getOrDefault(false)
                else runCatching { NativeApi.unsubscribe(requireContext()) }.getOrDefault(true)
            }
            if (enabled && ok) try { HocFirebase.refreshAndRegister(requireContext()) } catch (_: Exception) { }
            if (!ok && enabled) Toast.makeText(context, "A Push bekapcsolása nem sikerült", Toast.LENGTH_SHORT).show()
            loadData()
        }
    }

    private fun renderTopics(topics: List<NativeApi.Topic>, selected: List<Int>) {
        topicList.removeAllViews(); topicChecks.clear()
        val enabled = masterSwitch.isChecked
        topics.forEach { t ->
            val cb = CheckBox(requireContext()).apply {
                text = t.name
                isChecked = t.id in selected
                isEnabled = enabled
                setPadding(0, 6, 0, 6)
            }
            topicChecks[t.id] = cb
            topicList.addView(cb)
        }
        saveButton.isEnabled = enabled && topics.isNotEmpty()
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
            saveButton.isEnabled = masterSwitch.isChecked
            Toast.makeText(context, if (ok) "Témák elmentve" else "A mentés nem sikerült", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderInbox(items: List<NativeApi.InboxItem>) {
        inboxList.removeAllViews()
        if (items.isEmpty()) {
            inboxList.addView(TextView(requireContext()).apply { text = "Még nincs értesítésed." })
            return
        }
        items.take(20).forEach { item ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 10, 0, 10)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) { runCatching { NativeApi.markRead(requireContext(), item.id) } }
                    if (item.url.isNotBlank()) startActivity(Intent(requireContext(), ArticleActivity::class.java).putExtra(ArticleActivity.EXTRA_URL, item.url))
                }
            }
            row.addView(TextView(requireContext()).apply {
                text = item.title
                textSize = 15f
                if (!item.read) setTypeface(typeface, Typeface.BOLD)
            })
            if (item.body.isNotBlank()) row.addView(TextView(requireContext()).apply {
                text = item.body
                textSize = 13f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            })
            if (item.createdAt.isNotBlank()) row.addView(TextView(requireContext()).apply {
                text = item.createdAt
                textSize = 11f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            })
            inboxList.addView(row)
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
