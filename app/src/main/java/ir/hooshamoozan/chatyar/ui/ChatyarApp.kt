package ir.hooshamoozan.chatyar.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.hooshamoozan.chatyar.data.AppLanguage

@Composable
fun ChatyarApp(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val providers by vm.providers.collectAsStateWithLifecycle()
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
                MainNavigation(vm = vm, hasProviders = providers.isNotEmpty())
            }
        }
    }
}

@Composable
private fun MainNavigation(vm: MainViewModel, hasProviders: Boolean) {
    val navController = rememberNavController()
    val start = if (hasProviders) "chats" else "providers"

    NavHost(
        navController = navController,
        startDestination = start,
        modifier = Modifier.fillMaxSize()
    ) {
        composable("chats") {
            ChatsScreen(
                vm = vm,
                onOpenChat = { navController.navigate("chat/$it") },
                onAddProvider = { navController.navigate("provider/new") },
                onProviders = { navController.navigate("providers") },
                onSettings = { navController.navigate("settings") }
            )
        }
        composable("providers") {
            ProvidersScreen(
                vm = vm,
                onBack = { if (!navController.popBackStack()) navController.navigate("chats") },
                onAdd = { navController.navigate("provider/new") },
                onEdit = { navController.navigate("provider/$it") }
            )
        }
        composable("settings") {
            SettingsScreen(
                vm = vm,
                onBack = { navController.popBackStack() }
            )
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
            ChatScreen(
                vm = vm,
                chatId = entry.arguments?.getString("chatId").orEmpty(),
                onBack = { navController.popBackStack() },
                onProviders = { navController.navigate("providers") },
                onSettings = { navController.navigate("settings") }
            )
        }
    }
}
