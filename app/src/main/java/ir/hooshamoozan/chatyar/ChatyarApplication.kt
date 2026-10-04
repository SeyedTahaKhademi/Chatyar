package ir.hooshamoozan.chatyar

import android.app.Application

class ChatyarApplication : Application() {
    lateinit var container: AppContainer
        private set
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
