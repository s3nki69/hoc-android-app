package hu.hoc.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {
    
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupViews()
        setupViewPager()
    }
    
    private fun setupViews() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
    }
    
    private fun setupViewPager() {
        val adapter = MainPagerAdapter(this)
        viewPager.adapter = adapter
        
        // A fülek elnevezései
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Hírek"
                1 -> "Kuponok"
                2 -> "Videók"
                3 -> "TikTok"
                else -> ""
            }
        }.attach()
    }
}
