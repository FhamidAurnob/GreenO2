package com.example.greeno2

import android.app.Application
import android.content.Context
import com.example.greeno2.data.GreenDatabase
import com.example.greeno2.data.GreenRepository
import com.example.greeno2.data.SessionManager

class GreenMilesApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Simple manual dependency container shared by the UI and the tracking service. */
class AppContainer(context: Context) {
    val repository = GreenRepository(GreenDatabase(context))
    val session = SessionManager(context)
}

val Context.appContainer: AppContainer
    get() = (applicationContext as GreenMilesApp).container
