package ir.hooshamoozan.chatyar.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.core.view.WindowCompat
import ir.hooshamoozan.chatyar.R
import ir.hooshamoozan.chatyar.data.ThemeMode

val BrandBlue = Color(0xFF2574FC)
val BrandBlueDark = Color(0xFF79A8FF)
val BrandSurface = Color(0xFFF7F9FF)
val BrandDarkSurface = Color(0xFF0B1020)

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val vazirmatn = GoogleFont("Vazirmatn")

val VazirFamily = FontFamily(
    Font(googleFont = vazirmatn, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = vazirmatn, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = vazirmatn, fontProvider = provider, weight = FontWeight.Bold)
)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF4B638D),
    background = BrandSurface,
    surface = Color.White,
    surfaceVariant = Color(0xFFEEF3FC),
    outline = Color(0xFFCBD4E4),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = BrandBlueDark,
    onPrimary = Color(0xFF002E6C),
    primaryContainer = Color(0xFF0D4CA6),
    onPrimaryContainer = Color(0xFFDCE8FF),
    secondary = Color(0xFFB3C8F2),
    background = BrandDarkSurface,
    surface = Color(0xFF11182A),
    surfaceVariant = Color(0xFF1A2336),
    outline = Color(0xFF42506A),
    error = Color(0xFFFFB4AB)
)

@Composable
fun ChatyarTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors
    val context = LocalContext.current
    SideEffect {
        val activity = context as? Activity ?: return@SideEffect
        activity.window.statusBarColor = colors.background.toArgb()
        activity.window.navigationBarColor = colors.background.toArgb()
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography.run {
            copy(
                displayLarge = displayLarge.copy(fontFamily = VazirFamily),
                displayMedium = displayMedium.copy(fontFamily = VazirFamily),
                displaySmall = displaySmall.copy(fontFamily = VazirFamily),
                headlineLarge = headlineLarge.copy(fontFamily = VazirFamily),
                headlineMedium = headlineMedium.copy(fontFamily = VazirFamily),
                headlineSmall = headlineSmall.copy(fontFamily = VazirFamily),
                titleLarge = titleLarge.copy(fontFamily = VazirFamily),
                titleMedium = titleMedium.copy(fontFamily = VazirFamily),
                titleSmall = titleSmall.copy(fontFamily = VazirFamily),
                bodyLarge = bodyLarge.copy(fontFamily = VazirFamily),
                bodyMedium = bodyMedium.copy(fontFamily = VazirFamily),
                bodySmall = bodySmall.copy(fontFamily = VazirFamily),
                labelLarge = labelLarge.copy(fontFamily = VazirFamily),
                labelMedium = labelMedium.copy(fontFamily = VazirFamily),
                labelSmall = labelSmall.copy(fontFamily = VazirFamily)
            )
        },
        content = content
    )
}
