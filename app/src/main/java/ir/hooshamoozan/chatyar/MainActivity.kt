package ir.hooshamoozan.chatyar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.hooshamoozan.chatyar.ui.ChatyarApp
import ir.hooshamoozan.chatyar.ui.MainViewModel
import ir.hooshamoozan.chatyar.ui.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val app = application as ChatyarApplication
        setContent {
            val vm: MainViewModel = viewModel(
                factory = MainViewModelFactory(app.container)
            )
            ChatyarApp(vm)
        }
    }
}
