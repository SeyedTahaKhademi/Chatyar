package ir.hooshamoozan.chatyar.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.hooshamoozan.chatyar.data.AppLanguage

@Composable
fun ChatyarApp(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val texts = textsFor(settings.language)
    val direction = if (settings.language == AppLanguage.FA) LayoutDirection.Rtl else LayoutDirection.Ltr

    ChatyarTheme(settings.themeMode) {
        CompositionLocalProvider(
            LocalAppText provides texts,
            androidx.compose.ui.platform.LocalLayoutDirection provides direction
        ) {
            if (!settings.onboarded) {
                OnboardingScreen(onDone = vm::completeOnboarding)
            } else {
                MainNavigation(vm)
            }
        }
    }
}

private data class BottomDestination(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: (AppText) -> String
)

@Composable
private fun MainNavigation(vm: MainViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val t = LocalAppText.current
    val bottom = listOf(
        BottomDestination("chats", Icons.Outlined.ChatBubbleOutline) { it.chats },
        BottomDestination("providers", Icons.Outlined.Dns) { it.providers },
        BottomDestination("settings", Icons.Outlined.Settings) { it.settings }
    )
    val showBottom = currentRoute in bottom.map { it.route } || currentRoute == null

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottom) {
                NavigationBar {
                    bottom.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label(t)) },
                            label = { Text(item.label(t)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "providers",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("chats") {
                ChatsScreen(
                    vm = vm,
                    contentPadding = padding,
                    onOpenChat = { navController.navigate("chat/$it") },
                    onAddProvider = { navController.navigate("provider/new") }
                )
            }
            composable("providers") {
                ProvidersScreen(
                    vm = vm,
                    contentPadding = padding,
                    onAdd = { navController.navigate("provider/new") },
                    onEdit = { navController.navigate("provider/$it") }
                )
            }
            composable("settings") {
                SettingsScreen(vm = vm, contentPadding = padding)
            }
            composable("provider/new") {
                ProviderEditorScreen(
                    vm = vm,
                    providerId = null,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "provider/{providerId}",
                arguments = listOf(navArgument("providerId") { type = NavType.StringType })
            ) { entry ->
                ProviderEditorScreen(
                    vm = vm,
                    providerId = entry.arguments?.getString("providerId"),
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "chat/{chatId}",
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { entry ->
                val chatId = entry.arguments?.getString("chatId").orEmpty()
                ChatScreen(
                    vm = vm,
                    chatId = chatId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
