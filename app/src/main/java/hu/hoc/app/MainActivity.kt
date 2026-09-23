package hu.hoc.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupViews()
        setupViewPager()
        setupNativeNotifications()
        handleIncomingLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingLink(intent)
    }

    private fun setupViews() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
    }

    private fun setupViewPager() {
        viewPager.adapter = MainPagerAdapter(this)
        viewPager.isUserInputEnabled = false
        viewPager.offscreenPageLimit = 2

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_articles)
                1 -> getString(R.string.tab_coupons)
                2 -> getString(R.string.tab_videos)
                3 -> getString(R.string.tab_tiktok)
                4 -> getString(R.string.tab_notifications)
                else -> ""
            }
        }.attach()
    }

    private fun setupNativeNotifications() {
        HocNotificationWorker.createChannel(this)
        if (!NativeApi.isPushEnabled(this)) {
            HocNotificationWorker.cancel(this)
            return
        }
        HocNotificationWorker.schedule(this, fcmReady = false)
        HocNotificationWorker.runNow(this)
        lifecycleScope.launch {
            try { HocFirebase.refreshAndRegister(this@MainActivity) } catch (_: Exception) { }
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun handleIncomingLink(sourceIntent: Intent?) {
        val inboxId = sourceIntent?.getIntExtra(EXTRA_INBOX_ID, 0) ?: 0
        if (inboxId > 0) lifecycleScope.launch(Dispatchers.IO) { runCatching { NativeApi.markRead(this@MainActivity, inboxId) } }
        if (sourceIntent?.action != Intent.ACTION_VIEW) return
        val uri: Uri = sourceIntent.data ?: return
        val host = uri.host.orEmpty().lowercase()
        if (host == "hoc.hu" || host == "www.hoc.hu") {
            val path = uri.path.orEmpty()
            if (path.isNotBlank() && path != "/") {
                startActivity(Intent(this, ArticleActivity::class.java).putExtra(ArticleActivity.EXTRA_URL, uri.toString()))
            }
        }
    }

    companion object {
        const val EXTRA_INBOX_ID = "hoc_inbox_id"
    }
}
