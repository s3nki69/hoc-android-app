package hu.hoc.app

import android.app.Application

class HocApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        HocFirebase.initializeFromCache(this)
    }
}
