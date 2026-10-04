package ir.hooshamoozan.chatyar.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import ir.hooshamoozan.chatyar.R
import ir.hooshamoozan.chatyar.data.ThemeMode

val BrandBlue = Color(0xFF2574FC)
val BrandBlueLight = Color(0xFF8DB8FF)

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)
private val vazirmatn = GoogleFont("Vazirmatn")
val VazirFamily = FontFamily(
    Font(vazirmatn, provider, FontWeight.Normal),
    Font(vazirmatn, provider, FontWeight.Medium),
    Font(vazirmatn, provider, FontWeight.Bold)
)

private val LightColors = lightColorScheme(
    primary = BrandBlue, onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FF), onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF49658F), secondaryContainer = Color(0xFFDCE8FF),
    background = Color(0xFFF7F9FD), onBackground = Color(0xFF12151C),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF161A22),
    surfaceVariant = Color(0xFFF0F3F8), onSurfaceVariant = Color(0xFF626B7A),
    outline = Color(0xFFD5DBE5), error = Color(0xFFD3424B)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6EA2FF), onPrimary = Color(0xFF001C43),
    primaryContainer = Color(0xFF102A55), onPrimaryContainer = Color(0xFFDCE8FF),
    secondary = Color(0xFFA9C7FF), secondaryContainer = Color(0xFF1A2A43),
    background = Color(0xFF080A0F), onBackground = Color(0xFFF3F5F9),
    surface = Color(0xFF11151D), onSurface = Color(0xFFF3F5F9),
    surfaceVariant = Color(0xFF171C25), onSurfaceVariant = Color(0xFF9AA4B5),
    outline = Color(0xFF2A3341), error = Color(0xFFFF6B73), errorContainer = Color(0xFF3A171C)
)

@Composable
fun ChatyarTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
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
                displayLarge = displayLarge.copy(fontFamily = VazirFamily), displayMedium = displayMedium.copy(fontFamily = VazirFamily), displaySmall = displaySmall.copy(fontFamily = VazirFamily),
                headlineLarge = headlineLarge.copy(fontFamily = VazirFamily), headlineMedium = headlineMedium.copy(fontFamily = VazirFamily), headlineSmall = headlineSmall.copy(fontFamily = VazirFamily),
                titleLarge = titleLarge.copy(fontFamily = VazirFamily), titleMedium = titleMedium.copy(fontFamily = VazirFamily), titleSmall = titleSmall.copy(fontFamily = VazirFamily),
                bodyLarge = bodyLarge.copy(fontFamily = VazirFamily), bodyMedium = bodyMedium.copy(fontFamily = VazirFamily), bodySmall = bodySmall.copy(fontFamily = VazirFamily),
                labelLarge = labelLarge.copy(fontFamily = VazirFamily), labelMedium = labelMedium.copy(fontFamily = VazirFamily), labelSmall = labelSmall.copy(fontFamily = VazirFamily)
            )
        },
        shapes = Shapes(
            extraSmall = RoundedCornerShape(10.dp), small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(30.dp)
        ),
        content = content
    )
}
