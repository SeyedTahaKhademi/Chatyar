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
        // Keep Compose insets in control of IME and navigation bars on Android 8–15.
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val app = application as ChatyarApplication
        setContent {
            val vm: MainViewModel = viewModel(factory = MainViewModelFactory(app.container))
            ChatyarApp(vm)
        }
    }
}
