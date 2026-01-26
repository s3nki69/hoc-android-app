package hu.hoc.app

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    
    // Most már 4 fülünk van
    override fun getItemCount(): Int = 4
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ArticlesFragment()
            1 -> CouponsFragment()
            2 -> VideosFragment()
            3 -> TikTokFragment() // Itt az új TikTok fül
            else -> ArticlesFragment()
        }
    }
}
